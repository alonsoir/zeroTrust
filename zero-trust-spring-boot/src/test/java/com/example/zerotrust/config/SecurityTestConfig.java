package com.example.zerotrust.config;

import org.junit.jupiter.api.BeforeAll;

/**
 * CLASE BASE INDEPENDIENTE PARA TESTS CON SEGURIDAD
 *
 * NO hereda de BaseTestConfig para evitar conflictos de configuración.
 * Configuración mínima enfocada exclusivamente en habilitar Spring Security.
 *
 * IMPORTANTE:
 * - Usar junto con el profile 'test-security' o @TestPropertySource
 * - NO configura user/password aquí para evitar conflictos entre tests
 * - Cada test debe configurar sus propias credenciales usando @TestPropertySource
 */
public abstract class SecurityTestConfig {

    @BeforeAll
    static void configureSecurityProperties() {
        // Deshabilitar Spring Cloud Vault y Bootstrap
        System.setProperty("spring.cloud.vault.enabled", "false");
        System.setProperty("spring.cloud.bootstrap.enabled", "false");
        System.setProperty("spring.cloud.config.enabled", "false");
        System.setProperty("spring.cloud.discovery.enabled", "false");
        System.setProperty("spring.cloud.service-registry.auto-registration.enabled", "false");

        // Configuración de base de datos H2 para tests
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:testdb");
        System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");
        System.setProperty("spring.datasource.username", "sa");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");
        System.setProperty("spring.h2.console.enabled", "true");

        // Habilitar Spring Security (crítico)
        System.setProperty("spring.security.enabled", "true");

        // Logging para debugging de seguridad
        System.setProperty("logging.level.org.springframework.security", "DEBUG");
        System.setProperty("logging.level.org.springframework.web", "DEBUG");

        // ===== NO configurar user/password aquí =====
        // Cada test debe usar @TestPropertySource con sus propias credenciales:
        // @TestPropertySource(properties = {
        //     "spring.security.user.name=testuser",
        //     "spring.security.user.password=testpass",
        //     "spring.security.user.roles=USER"
        // })
    }
}