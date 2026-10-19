package com.gridpulse.web.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// the X-Internal-Key check for a service that serves /internal/** (core, ai): @Import it from SecurityConfig
@Configuration
@EnableConfigurationProperties(InternalApiProperties.class)
@Import(InternalKeyFilter.class)
public class InternalKeySecurityConfiguration {
}
