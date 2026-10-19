package com.gridpulse.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// where the angular site runs (gridpulse.frontend-url), emails link to its pages
@ConfigurationProperties(prefix = "gridpulse")
public record FrontendProperties(String frontendUrl) {

    // "https://site.com/" and "https://site.com" give the same links
    public String link(String pathAndQuery) {
        return frontendUrl.replaceAll("/+$", "") + pathAndQuery;
    }
}
