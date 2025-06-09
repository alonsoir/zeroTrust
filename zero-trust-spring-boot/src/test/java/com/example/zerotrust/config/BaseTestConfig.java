package com.example.zerotrust.config;

import org.junit.jupiter.api.BeforeAll;

/**
 * Clase base que proporciona configuración común para todos los tests
 * Usa System.setProperty() que se aplica ANTES del bootstrap de Spring Cloud
 */
public abstract class BaseTestConfig {

    @BeforeAll
    static void configureSystemProperties() {
        // CRÍTICO: Estas propiedades se configuran ANTES del bootstrap
        System.setProperty("spring.cloud.bootstrap.enabled", "false");
        System.setProperty("spring.cloud.config.enabled", "false");
        System.setProperty("spring.cloud.vault.enabled", "false");
        System.setProperty("spring.cloud.vault.config.enabled", "false");
        System.setProperty("spring.cloud.vault.authentication.token", "");
        System.setProperty("spring.cloud.vault.token", "");

        // Base de datos H2 en memoria
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        System.setProperty("spring.datasource.username", "sa");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");

        // JPA/Hibernate para tests
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("spring.jpa.show-sql", "false");
        System.setProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.H2Dialect");

        // Configuraciones JWT para tests
        System.setProperty("app.jwt.secret", "test-secret-key-only-for-testing-must-be-at-least-256-bits-long-for-security-purposes");
        System.setProperty("app.security.jwt.secret", "test-secret-key-only-for-testing-must-be-at-least-256-bits-long-for-security");
        System.setProperty("app.security.risk.high-threshold", "1.0");

        // Deshabilitar Spring Security
        System.setProperty("spring.security.enabled", "false");

        // Actuator
        System.setProperty("management.endpoints.web.exposure.include", "health,info");
        System.setProperty("management.endpoint.health.enabled", "true");
        System.setProperty("management.endpoint.info.enabled", "true");
        System.setProperty("management.health.vault.enabled", "false");

        // Logging
        System.setProperty("logging.level.root", "WARN");
        System.setProperty("logging.level.com.example.zerotrust", "DEBUG");
        System.setProperty("logging.level.org.springframework.cloud", "ERROR");
        System.setProperty("logging.level.org.springframework.vault", "ERROR");
        System.setProperty("logging.level.org.springframework.security", "OFF");
    }
}