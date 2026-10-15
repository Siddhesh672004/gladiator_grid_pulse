package com.gridpulse.gateway.ratelimit;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

// gridpulse.rate-limit.rules in application.yml, tried from the top, the first match counts
@ConfigurationProperties(prefix = "gridpulse.rate-limit")
public record RateLimitProperties(List<Rule> rules) {

    // method empty = any method; capacity 0 = no limit (live boards)
    public record Rule(String method, List<String> paths, int capacity, Duration period, KeyType key) {
    }
}
