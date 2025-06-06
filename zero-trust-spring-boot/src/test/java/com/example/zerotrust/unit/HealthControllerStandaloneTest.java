package com.example.zerotrust.unit;

import com.example.zerotrust.controller.HealthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test con MockMvc standalone - No carga Spring Context completo
 * Balance perfecto entre simplicidad y funcionalidad de testing web
 */
class HealthControllerStandaloneTest {

    private MockMvc mockMvc;
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController();

        // Simular la inyección de @Value
        ReflectionTestUtils.setField(healthController, "activeProfiles", "test");

        // Configurar MockMvc standalone (sin contexto de Spring)
        mockMvc = MockMvcBuilders
                .standaloneSetup(healthController)
                .build();
    }

    @Test
    void healthEndpointShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Zero Trust App"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void infoEndpointShouldReturnApplicationInfo() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.name").value("Zero Trust Spring Boot Application"))
                .andExpect(jsonPath("$.description").value("Enterprise-grade Zero Trust security implementation"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.java_version").exists())
                .andExpect(jsonPath("$.spring_profiles").value("test"));
    }

    @Test
    void healthEndpointShouldReturnValidTimestamp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp").isNumber()); // Cambio: es un número, no string
    }
}