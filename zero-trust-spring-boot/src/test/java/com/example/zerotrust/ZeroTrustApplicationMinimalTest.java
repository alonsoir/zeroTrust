package com.example.zerotrust;

import com.example.zerotrust.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {HealthController.class}) // Solo cargar el controlador
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // Deshabilitar Bootstrap Context completamente
        "spring.cloud.bootstrap.enabled=false",

        // Deshabilitar Vault
        "spring.cloud.vault.enabled=false",
        "spring.cloud.vault.config.enabled=false",

        // Deshabilitar otras configuraciones problemáticas
        "spring.cloud.config.enabled=false",

        // Deshabilitar auto-configuraciones de seguridad
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration"
})
class ZeroTrustApplicationMinimalTest {

    @Test
    void contextLoads() {
        // Test básico que solo verifica que el contexto mínimo se carga
    }
}