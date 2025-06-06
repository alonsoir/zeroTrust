package com.example.zerotrust;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // CRÍTICO: Deshabilitar Bootstrap Context
        "spring.cloud.bootstrap.enabled=false",

        // Deshabilitar Vault completamente
        "spring.cloud.vault.enabled=false",
        "spring.cloud.vault.config.enabled=false",

        // Deshabilitar otras configuraciones problemáticas
        "spring.cloud.config.enabled=false",

        // Configuraciones de test adicionales
        "app.jwt.secret=test-secret-key-only-for-testing-must-be-at-least-256-bits-long",

        // Logging
        "logging.level.org.springframework.cloud=ERROR",
        "logging.level.org.springframework.vault=ERROR"
})
class ZeroTrustApplicationTests {

    @Test
    void contextLoads() {
        // Test básico que verifica que el contexto se carga correctamente
    }
}