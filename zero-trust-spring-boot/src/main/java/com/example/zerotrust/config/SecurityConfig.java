package com.example.zerotrust.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad Zero Trust basada en properties
 * Compatible con Spring Boot 3.3.5 y Spring Security 6.1+
 */
@Configuration
@EnableWebSecurity
@ConfigurationProperties(prefix = "app.security")
public class SecurityConfig {

    /**
     * Si true, los endpoints /api/health e /api/info requieren autenticación
     * Si false, son públicos (comportamiento por defecto)
     */
    private boolean requireAuthForHealthEndpoints = false;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpSecurity httpSecurity = http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())  // Para H2 Console - Sintaxis moderna
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'")));

        if (requireAuthForHealthEndpoints) {
            // Configuración para tests de seguridad: /api/** requiere autenticación
            return httpSecurity
                    .authorizeHttpRequests(authz -> authz
                            .requestMatchers("/actuator/**", "/h2-console/**").permitAll()
                            .requestMatchers("/api/**").authenticated()  // Requiere autenticación
                            .anyRequest().authenticated())
                    .httpBasic(httpBasic -> {})  // Habilitar autenticación básica
                    .build();
        } else {
            // Configuración por defecto: /api/health y /api/info son públicos
            return httpSecurity
                    .authorizeHttpRequests(authz -> authz
                            .requestMatchers("/api/health", "/api/info", "/actuator/**", "/h2-console/**").permitAll()
                            .anyRequest().authenticated())
                    .build();
        }
    }

    // Getters y Setters para Spring Boot Configuration Properties
    public boolean isRequireAuthForHealthEndpoints() {
        return requireAuthForHealthEndpoints;
    }

    public void setRequireAuthForHealthEndpoints(boolean requireAuthForHealthEndpoints) {
        this.requireAuthForHealthEndpoints = requireAuthForHealthEndpoints;
    }
}