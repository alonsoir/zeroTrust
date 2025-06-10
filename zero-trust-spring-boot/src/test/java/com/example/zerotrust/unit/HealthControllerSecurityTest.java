package com.example.zerotrust.unit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test para HealthController CON seguridad activa
 * Este test verifica que los endpoints funcionen correctamente cuando Spring Security está habilitado
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-security")
@TestPropertySource(properties = {
        // Deshabilitar Vault y otras configuraciones
        "spring.cloud.vault.enabled=false",
        "spring.cloud.bootstrap.enabled=false",
        "spring.cloud.config.enabled=false"
})
class HealthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Configuración de seguridad específica para este test
     */
    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfiguration {

        @Bean
        @Primary
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .authorizeHttpRequests(authz -> authz
                            // TODOS los endpoints /api/** requieren autenticación
                            .requestMatchers("/api/**").authenticated()
                            // Permitir H2 console y actuator
                            .requestMatchers("/h2-console/**", "/actuator/**").permitAll()
                            // Cualquier otra petición requiere autenticación
                            .anyRequest().authenticated()
                    )
                    .httpBasic(httpBasic -> {}) // Habilitar HTTP Basic
                    .csrf(csrf -> csrf.disable()) // Deshabilitar CSRF para tests
                    .headers(headers -> headers
                            .frameOptions(frameOptions -> frameOptions.sameOrigin())
                    );

            return http.build();
        }

        @Bean
        @Primary
        public UserDetailsService testUserDetailsService() {
            UserDetails testUser = User.builder()
                    .username("testuser")
                    .password(passwordEncoder().encode("testpass"))
                    .roles("USER")
                    .build();

            return new InMemoryUserDetailsManager(testUser);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Test
    void healthEndpointShouldRequireAuthentication() throws Exception {
        // Test sin autenticación - debería fallar con 401
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void healthEndpointShouldReturnOkWithMockUser() throws Exception {
        // Test con usuario mock - debería funcionar
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Zero Trust App"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void healthEndpointShouldReturnOkWithBasicAuth() throws Exception {
        // Test con autenticación básica - debería funcionar
        mockMvc.perform(get("/api/health")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Zero Trust App"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void infoEndpointShouldReturnApplicationInfoWithMockUser() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.name").value("Zero Trust Spring Boot Application"))
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.java_version").exists())
                .andExpect(jsonPath("$.spring_profiles").exists());
    }

    @Test
    void infoEndpointShouldReturnApplicationInfoWithBasicAuth() throws Exception {
        mockMvc.perform(get("/api/info")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.name").value("Zero Trust Spring Boot Application"))
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.java_version").exists())
                .andExpect(jsonPath("$.spring_profiles").exists());
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        // Test con credenciales incorrectas - debería fallar con 401
        mockMvc.perform(get("/api/health")
                        .with(httpBasic("wronguser", "wrongpass")))
                .andExpect(status().isUnauthorized());
    }
}