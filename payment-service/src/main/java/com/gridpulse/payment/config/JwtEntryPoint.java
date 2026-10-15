package com.gridpulse.payment.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridpulse.payment.dto.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// no or bad token -> the same 401 json as the core
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        String authHeader = request.getHeader("Authorization");
        boolean sentToken = authHeader != null && authHeader.startsWith("Bearer ");
        log.warn("SECURITY_REJECTED | status=401 UNAUTHORIZED | {} {} | reason={}", request.getMethod(),
                request.getRequestURI(), sentToken ? "invalid or expired token" : "no token");

        response.setHeader("WWW-Authenticate", sentToken ? "Bearer error=\"invalid_token\"" : "Bearer");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(401, "Unauthorized",
                "Please log in to continue", request.getRequestURI()));
    }
}
