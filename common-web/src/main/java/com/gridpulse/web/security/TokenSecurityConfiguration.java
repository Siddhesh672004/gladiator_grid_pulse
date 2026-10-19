package com.gridpulse.web.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// the core's token check for a service without a user table (payment, ai): @Import it from SecurityConfig
// never in the core, its own filter puts a UserPrinciple into the security context
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@Import({ JwtConfig.class, JwtTokenFilter.class, JwtEntryPoint.class })
public class TokenSecurityConfiguration {
}
