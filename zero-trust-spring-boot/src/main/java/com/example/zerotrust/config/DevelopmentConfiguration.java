package com.example.zerotrust.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile; /**
 * 🧪 Configuración específica para desarrollo
 */
@Configuration
@Profile("development")
public class DevelopmentConfiguration {

    /**
     * Configuración adicional para desarrollo
     * Por ejemplo: timeouts más largos, logging adicional, etc.
     */
}
