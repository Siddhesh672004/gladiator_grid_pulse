package com.gridpulse.core.platform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gridpulse.core.platform.properties.CorsProperties;

import lombok.RequiredArgsConstructor;

// lets angular (8081) call the core directly; allowed origins come from CorsProperties
// off behind the gateway (gridpulse.cors.enabled=false), the gateway answers CORS there
@Configuration
@ConditionalOnProperty(name = "gridpulse.cors.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class CrosConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // angular reads the file name of a download (the e-ticket) from this header
                .exposedHeaders(HttpHeaders.CONTENT_DISPOSITION)
                .maxAge(3600);
    }
}
