package com.gridpulse.core.config;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// runs before every request: reads the token and tells spring security who the user is
// no / bad token -> carries on as "not logged in", SecurityConfig decides; the role is read from the db, not the token
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // only a "Bearer ..." header carries a token, anything else stays anonymous
        Optional.ofNullable(request.getHeader("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7))
                .ifPresent(token -> tryLogIn(token, request));
        filterChain.doFilter(request, response);
    }

    // a bad token is no error here, the request just goes on as "not logged in"
    private void tryLogIn(String token, HttpServletRequest request) {
        try {
            logIn(token, request);
        } catch (Exception e) {
            log.debug("Rejected JWT on {}: {}", request.getRequestURI(), e.getClass().getSimpleName());
        }
    }

    private void logIn(String token, HttpServletRequest request) {
        String email = jwtUtils.extractUsername(token);
        // nobody logged in yet -> load the account; disabled = not logged in, even with a valid token
        Optional.ofNullable(email)
                .filter(subject -> SecurityContextHolder.getContext().getAuthentication() == null)
                .map(userDetailsService::loadUserByUsername)
                .filter(UserDetails::isEnabled)
                .filter(userDetails -> jwtUtils.validateToken(token, userDetails))
                .ifPresent(userDetails -> {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.trace("authenticated {} for {}", email, request.getRequestURI());
                });
    }
}
