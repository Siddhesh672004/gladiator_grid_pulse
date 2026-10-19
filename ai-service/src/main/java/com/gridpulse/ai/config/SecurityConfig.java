package com.gridpulse.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.gridpulse.web.security.InternalKeyFilter;
import com.gridpulse.web.security.InternalKeySecurityConfiguration;
import com.gridpulse.web.security.JwtEntryPoint;
import com.gridpulse.web.security.JwtTokenFilter;
import com.gridpulse.web.security.TokenSecurityConfiguration;

import lombok.RequiredArgsConstructor;

// the faq chat is open for everyone, /internal/** needs the internal key, everything else a core token
// (the role per url is on the methods)
// no CORS here: the browser only talks to the gateway
@Configuration
@EnableMethodSecurity
@Import({ TokenSecurityConfiguration.class, InternalKeySecurityConfiguration.class })
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final JwtEntryPoint jwtEntryPoint;
    private final InternalKeyFilter internalKeyFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/chat", "/api/chat/**", "/api/faqs").permitAll()
                        // health for docker / the gateway
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // the other services only: InternalKeyFilter checks the key first
                        .requestMatchers("/internal/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(internalKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
