package com.example.zerotrust.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile; /**
 * 🏭 Configuración específica para producción
 */
@Configuration
@Profile("production")
public class ProductionConfiguration {

    /**
     * Configuración adicional para producción
     * Por ejemplo: SSL, security headers, monitoring, etc.
     */
}
