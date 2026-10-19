package com.gridpulse.core.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// which websites may call the api: dev = "*", prod = only our own website. enabled=false behind the gateway
@ConfigurationProperties(prefix = "gridpulse.cors")
public record CorsProperties(List<String> allowedOrigins, @DefaultValue("true") boolean enabled) {
}
