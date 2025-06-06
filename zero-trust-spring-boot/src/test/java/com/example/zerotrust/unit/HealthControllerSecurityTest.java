package com.example.zerotrust.unit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test para HealthController CON seguridad activa
 * Este test verifica que los endpoints funcionen correctamente cuando Spring Security está habilitado
 *
 * Usa el perfil test-security que tiene toda la configuración necesaria
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-security")  // ← CAMBIO: Usar perfil test-security
@TestPropertySource(properties = {
        // Solo necesitamos deshabilitar Vault, el resto está en el perfil
        "spring.cloud.vault.enabled=false",
        "spring.cloud.bootstrap.enabled=false",
        "spring.cloud.config.enabled=false"
})
class HealthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointShouldRequireAuthentication() throws Exception {
        // Test sin autenticación - debería fallar
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
        // Test con credenciales incorrectas - debería fallar
        mockMvc.perform(get("/api/health")
                        .with(httpBasic("wronguser", "wrongpass")))
                .andExpect(status().isUnauthorized());
    }
}