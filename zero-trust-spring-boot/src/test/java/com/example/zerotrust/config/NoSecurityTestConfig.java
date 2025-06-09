package com.example.zerotrust.config;

import org.junit.jupiter.api.BeforeAll;

/**
 * CLASE BASE PARA TESTS SIN SEGURIDAD
 * Extiende BaseTestConfig y agrega configuraciones específicas para tests sin seguridad
 *
 * Usar para tests @WebMvcTest que necesitan deshabilitar completamente Spring Security
 */
public abstract class NoSecurityTestConfig extends BaseTestConfig {

    @BeforeAll
    static void configureNoSecurityProperties() {
        // Configuraciones específicas para tests SIN seguridad
        System.setProperty("spring.security.enabled", "false");
        System.setProperty("logging.level.org.springframework.security", "OFF");

        // Configuraciones adicionales para tests unitarios
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("spring.jpa.show-sql", "false");
        System.setProperty("management.endpoints.web.exposure.include", "health,info");
        System.setProperty("management.health.vault.enabled", "false");
    }
}