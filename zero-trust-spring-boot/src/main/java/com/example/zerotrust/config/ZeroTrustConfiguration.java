package com.example.zerotrust.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 🔧 Configuración Spring Estado del Arte
 *
 * ✅ CARACTERÍSTICAS MODERNAS:
 * - EnableConfigurationProperties para Records
 * - Profile-specific configuration
 * - Bean validation automática
 * - ConfigData API integration
 */
@Configuration
@EnableConfigurationProperties({
        JwtProperties.class
        // Aquí puedes añadir otras @ConfigurationProperties
})
public class ZeroTrustConfiguration {

    /**
     * ✅ Bean de validación que se ejecuta al startup
     * Falla rápido si la configuración es inválida
     */
    @Bean
    public JwtConfigurationValidator jwtConfigurationValidator(JwtProperties jwtProperties) {
        return new JwtConfigurationValidator(jwtProperties);
    }

    /**
     * 🔐 Validador de configuración JWT
     */
    public static class JwtConfigurationValidator {
        private final JwtProperties jwtProperties;

        public JwtConfigurationValidator(JwtProperties jwtProperties) {
            this.jwtProperties = jwtProperties;
            // Validar inmediatamente al crear el bean
            validateConfiguration();
        }

        private void validateConfiguration() {
            System.out.println("🚀 Iniciando validación de configuración JWT...");

            try {
                jwtProperties.validate();
                System.out.println("✅ Configuración JWT validada exitosamente");
            } catch (Exception e) {
                System.err.println("❌ Error en configuración JWT: " + e.getMessage());
                throw new IllegalStateException("Configuración JWT inválida", e);
            }
        }

        public JwtProperties getJwtProperties() {
            return jwtProperties;
        }
    }
}

