package com.gridpulse.core.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// dev only, to see the load balancing: every answer names the core instance that made it (host:port)
@Component
@Profile("dev")
public class ServedByFilter extends OncePerRequestFilter {

    private final String servedBy;

    public ServedByFilter(@Value("${server.port}") String port) {
        this.servedBy = hostName() + ":" + port;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Served-By", servedBy);
        chain.doFilter(request, response);
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
