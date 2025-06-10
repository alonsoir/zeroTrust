package com.example.zerotrust.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile; /**
 * 🧪 Configuración específica para tests
 */
@Configuration
@Profile({"test", "vault-integration"})
public class TestConfiguration {

    /**
     * Configuración adicional para tests
     * Por ejemplo: mocks, test doubles, etc.
     */
}
