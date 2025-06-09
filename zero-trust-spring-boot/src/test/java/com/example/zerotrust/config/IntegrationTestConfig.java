package com.example.zerotrust.config;

import org.junit.jupiter.api.BeforeAll;

/**
 * CLASE BASE PARA TESTS DE INTEGRACIÓN
 * Extiende BaseTestConfig y agrega configuraciones específicas para tests de integración completos
 *
 * Usar para tests que necesiten:
 * - Toda la aplicación cargada (@SpringBootTest)
 * - Múltiples capas funcionando juntas
 * - Base de datos real o contenedores
 * - Configuraciones completas de actuator
 */
public abstract class IntegrationTestConfig extends BaseTestConfig {

    @BeforeAll
    static void configureIntegrationProperties() {
        // Configuraciones específicas para tests de integración

        // JPA y base de datos
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("spring.jpa.show-sql", "false");
        System.setProperty("spring.jpa.properties.hibernate.format_sql", "false");

        // Actuator completo para tests de integración
        System.setProperty("management.endpoints.web.exposure.include", "health,info,metrics,env,configprops");
        System.setProperty("management.endpoint.health.enabled", "true");
        System.setProperty("management.endpoint.info.enabled", "true");
        System.setProperty("management.endpoint.metrics.enabled", "true");
        System.setProperty("management.health.vault.enabled", "false");

        // Configuraciones adicionales para tests completos
        System.setProperty("spring.h2.console.enabled", "true");
        System.setProperty("management.security.enabled", "false");

        // Logging optimizado para integración
        System.setProperty("logging.level.root", "WARN");
        System.setProperty("logging.level.com.example.zerotrust", "INFO");
        System.setProperty("logging.level.org.springframework.web", "INFO");
        System.setProperty("logging.level.org.hibernate.SQL", "DEBUG");

        // Configuraciones de cache si las tienes
        System.setProperty("spring.cache.type", "simple");

        // Redis deshabilitado para tests (usa configuración en memoria)
        System.setProperty("spring.data.redis.repositories.enabled", "false");
    }
}