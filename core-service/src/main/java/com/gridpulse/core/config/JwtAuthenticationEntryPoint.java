package com.gridpulse.core.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridpulse.web.error.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// protected url called without a valid token -> 401 in the usual error json (wrong role = 403, GlobalExceptionHandler)
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        String authHeader = request.getHeader("Authorization");
        boolean sentToken = authHeader != null && authHeader.startsWith("Bearer ");

        // runs before any controller, so GlobalLoggingAspect never sees this
        log.warn("SECURITY_REJECTED | status=401 UNAUTHORIZED | {} {} | reason={}", request.getMethod(),
                request.getRequestURI(), sentToken ? "invalid or expired token" : "no token");

        response.setHeader("WWW-Authenticate", sentToken ? "Bearer error=\"invalid_token\"" : "Bearer");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(401, "Unauthorized", "Please log in to continue",
                request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
