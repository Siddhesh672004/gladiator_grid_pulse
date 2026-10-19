package com.gridpulse.web.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gridpulse.common.security.JwtClaims;
import com.gridpulse.common.security.JwtVerifier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// the core's token -> who is calling (user id + role from the token itself, there is no user table here)
// trade-off: a changed role or a disabled account counts here only once the token ran out (10 h at most);
// the core checks the account again on every booking call, the admin read pages trust the token until then
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtVerifier jwtVerifier;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // no / bad token -> carries on as "not logged in", SecurityConfig decides
        Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7))
                .flatMap(jwtVerifier::verify)
                .filter(claims -> claims.userId() != null && claims.userRole() != null)
                .ifPresent(this::logIn);
        chain.doFilter(request, response);
    }

    private void logIn(JwtClaims claims) {
        TokenUser user = new TokenUser(claims.userId(), claims.email(), claims.username(), claims.userRole());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + claims.userRole()))));
    }
}
