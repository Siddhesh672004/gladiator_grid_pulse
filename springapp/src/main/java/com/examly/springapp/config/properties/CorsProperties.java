package com.examly.springapp.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

// which websites may call the api: dev = "*", prod = only our own website
@ConfigurationProperties(prefix = "gridpulse.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
