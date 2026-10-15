package com.gridpulse.ai.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridpulse.ai.config.properties.InternalApiProperties;
import com.gridpulse.ai.dto.response.ErrorResponse;
import com.gridpulse.common.security.InternalApi;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

// /internal/** is for the other services only (the gateway never lets it through): without the right key -> 401
@Slf4j
@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    private final String key;
    private final ObjectMapper objectMapper;

    public InternalKeyFilter(InternalApiProperties properties, ObjectMapper objectMapper) {
        this.key = InternalApi.keyOrDevDefault(properties.internalApiKey());
        this.objectMapper = objectMapper;
        if (InternalApi.DEV_KEY.equals(key)) {
            log.warn("INTERNAL_API_KEY is not set - the services use a fixed dev key, set it on a real server");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (InternalApi.matches(key, request.getHeader(InternalApi.HEADER))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Missing or wrong internal key", request.getRequestURI()));
    }
}
