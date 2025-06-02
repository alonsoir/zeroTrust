package com.example.zerotrust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Zero Trust Spring Boot Application
 *
 * Implementa un modelo de seguridad Zero Trust completo con:
 * - Autenticación basada en tokens JWT de corta duración
 * - Verificación continua de contexto (ABAC)
 * - Auditoría completa de todas las operaciones
 * - MFA obligatorio para operaciones críticas
 * - Análisis de riesgo en tiempo real
 */
@SpringBootApplication
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@EnableJpaAuditing
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableKafka
@ConfigurationPropertiesScan("com.example.zerotrust.config")
public class ZeroTrustApplication {

    public static void main(String[] args) {
        // Configuración de seguridad del sistema
        configureSystemSecurity();

        // Iniciar aplicación
        SpringApplication.run(ZeroTrustApplication.class, args);
    }

    private static void configureSystemSecurity() {
        System.setProperty("java.security.egd", "file:/dev/./urandom");
        System.setProperty("networkaddress.cache.ttl", "30");
        System.setProperty("jdk.tls.useExtendedMasterSecret", "true");
        System.setProperty("server.error.include-message", "never");
        System.setProperty("server.error.include-stacktrace", "never");
    }
}
