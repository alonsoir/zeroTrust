package com.example.zerotrust.unit;

import com.example.zerotrust.controller.HealthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitario puro para HealthController - Sin Spring Context
 * Esta es la versión más simple que no depende de configuración de Spring
 */
class HealthControllerUnitTest {

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController();
        // Simular la inyección de la propiedad @Value
        ReflectionTestUtils.setField(healthController, "activeProfiles", "test");
    }

    @Test
    void healthEndpointShouldReturnCorrectData() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.health();

        // Then
        assertEquals(200, response.getStatusCodeValue());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals("UP", body.get("status"));
        assertEquals("Zero Trust App", body.get("application"));
        assertEquals("1.0.0", body.get("version"));
        assertNotNull(body.get("timestamp"));
        assertTrue(body.get("timestamp") instanceof Instant);
    }

    @Test
    void infoEndpointShouldReturnCorrectData() {

        // When
        ResponseEntity<Map<String, Object>> response = healthController.info();

        // Then
        assertEquals(200, response.getStatusCodeValue());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals("Zero Trust Spring Boot Application", body.get("name"));
        assertEquals("Enterprise-grade Zero Trust security implementation", body.get("description"));
        assertEquals("1.0.0", body.get("version"));
        assertEquals("test", body.get("spring_profiles"));

        assertNotNull(body.get("java_version"));
        String javaVersion = (String) body.get("java_version");
        assertFalse(javaVersion.isEmpty());
// Verificar que contiene al menos un dígito (más flexible que buscar puntos)
        assertTrue(javaVersion.matches(".*\\d.*"), "Java version should contain at least one digit");
    }

    @Test
    void healthResponseShouldContainTimestamp() {
        // When
        Instant before = Instant.now();
        ResponseEntity<Map<String, Object>> response = healthController.health();
        Instant after = Instant.now();

        // Then
        Map<String, Object> body = response.getBody();
        Instant timestamp = (Instant) body.get("timestamp");

        assertTrue(timestamp.equals(before) || timestamp.isAfter(before));
        assertTrue(timestamp.equals(after) || timestamp.isBefore(after));
    }
}