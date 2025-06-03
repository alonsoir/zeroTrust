package com.example.zerotrust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Zero Trust Spring Boot Application
 *
 * Implementa un modelo de seguridad Zero Trust con:
 * - Autenticación basada en tokens JWT
 * - Verificación continua de contexto
 * - Auditoría completa de operaciones
 * - Control de acceso granular
 */
@SpringBootApplication
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class ZeroTrustApplication {

    public static void main(String[] args) {
        // Configuración de seguridad del sistema
        configureSystemSecurity();

        // Iniciar aplicación
        SpringApplication.run(ZeroTrustApplication.class, args);
    }

    private static void configureSystemSecurity() {
        System.setProperty("java.security.egd", "file:/dev/./urandom");
        System.setProperty("server.error.include-message", "never");
        System.setProperty("server.error.include-stacktrace", "never");
    }
}
