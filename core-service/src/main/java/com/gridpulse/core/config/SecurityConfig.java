package com.gridpulse.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.gridpulse.core.config.properties.CorsProperties;
import com.gridpulse.web.security.InternalKeyFilter;
import com.gridpulse.web.security.InternalKeySecurityConfiguration;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

// public urls + "everything else needs a token" (401 -> JwtAuthenticationEntryPoint)
// the role per url is on the controller methods (@AdminOnly, @UserOnly ... -> wrong role = 403)
// only the internal key check comes from common-web: the core keeps its own jwt classes (UserPrinciple)
@Configuration
@EnableMethodSecurity
@Import(InternalKeySecurityConfiguration.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CorsProperties corsProperties;
    private final InternalKeyFilter internalKeyFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                // behind the gateway the gateway answers CORS, two answers would confuse the browser
                .cors(corsProperties.enabled() ? Customizer.withDefaults() : AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // open for everyone
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/register/send-code", "/api/register", "/api/login",
                                "/api/admin/login", "/api/password/forgot", "/api/password/reset",
                                "/api/password/set-initial").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/race-track", "/api/race-track/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/race-control/session/*", "/api/live/session/*").permitAll()
                        // the admin list page sits next to the public "/api/sessions/{id}"
                        .requestMatchers(HttpMethod.GET, "/api/sessions/page").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/sessions/*", "/api/sessions/track/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/competitions", "/api/competitions/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stats/leaderboard").permitAll()
                        .requestMatchers("/error").permitAll()
                        // other services only (InternalKeyFilter checks the key first): by-order needs only the key,
                        // the rest also the driver's own token
                        .requestMatchers("/internal/bookings/by-order/**").permitAll()
                        .requestMatchers("/internal/**").authenticated()
                        // health for docker / the gateway, the other actuator urls are for admins
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasAnyRole("Admin", "SuperAdmin")
                        // api docs, switched off in prod
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // azure: the angular site comes from the same server, its pages and files are plain GETs outside /api
                        .requestMatchers(this::isSitePage).permitAll()

                        // everything else needs a token
                        .anyRequest().authenticated())
                .addFilterBefore(internalKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private boolean isSitePage(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod()) && !request.getServletPath().startsWith("/api/");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
