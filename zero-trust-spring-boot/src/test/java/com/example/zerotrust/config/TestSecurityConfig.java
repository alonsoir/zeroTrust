package com.example.zerotrust.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad específica para tests
 * Se activa solo con el perfil test-security
 */
@TestConfiguration
@EnableWebSecurity
@Profile("test-security")
public class TestSecurityConfig {

    /**
     * Configuración de la cadena de filtros de seguridad para tests
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Todos los endpoints /api/** requieren autenticación
                        .requestMatchers("/api/**").authenticated()
                        // Permitir H2 console para tests
                        .requestMatchers("/h2-console/**").permitAll()
                        // Permitir actuator endpoints
                        .requestMatchers("/actuator/**").permitAll()
                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> {}) // Habilitar HTTP Basic para tests
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**") // Deshabilitar CSRF para H2
                        .disable() // Deshabilitar CSRF para tests
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin()) // Permitir frames para H2 console
                );

        return http.build();
    }

    /**
     * Servicio de usuarios en memoria para tests
     */
    @Bean
    public UserDetailsService testUserDetailsService() {
        UserDetails testUser = User.builder()
                .username("testuser")
                .password(passwordEncoder().encode("testpass"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(testUser);
    }

    /**
     * Encoder de contraseñas para tests
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}