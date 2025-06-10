package com.example.zerotrust.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;

/**
 * Configuración principal para JWT en el sistema Zero Trust
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    private final JwtProperties jwtProperties;

    public JwtConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Validación de configuración al inicializar
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("Initializing JWT configuration...");

        if (!jwtProperties.isValid()) {
            throw new IllegalStateException("Invalid JWT configuration detected");
        }

        // Validaciones adicionales de seguridad
        validateSecuritySettings();

        log.info("JWT Configuration initialized successfully:");
        log.info("  - Issuer: {}", jwtProperties.getIssuer());
        log.info("  - Access Token Duration: {}", jwtProperties.getAccessTokenDuration());
        log.info("  - Refresh Token Duration: {}", jwtProperties.getRefreshTokenDuration());
        log.info("  - Refresh Token Rotation: {}", jwtProperties.isEnableRefreshTokenRotation());
        log.info("  - Token Blacklist: {}", jwtProperties.isEnableTokenBlacklist());
        log.info("  - Max Active Tokens per User: {}", jwtProperties.getMaxActiveTokensPerUser());
    }

    /**
     * Validaciones adicionales de seguridad
     */
    private void validateSecuritySettings() {
        // Validar que el secret esté configurado
        if (jwtProperties.getSecret() == null || jwtProperties.getSecret().trim().isEmpty()) {
            throw new IllegalStateException("JWT secret must be configured");
        }

        // Validar longitud mínima del secret para HS256 (256 bits = 32 bytes)
        if (jwtProperties.getSecret().length() < 32) {
            log.warn("JWT secret is shorter than recommended 256 bits. Current length: {} characters",
                    jwtProperties.getSecret().length());
        }

        // Validar duraciones mínimas de seguridad
        if (jwtProperties.getAccessTokenDuration().toMinutes() < 1) {
            log.warn("Access token duration is very short ({}). Consider increasing for better performance.",
                    jwtProperties.getAccessTokenDuration());
        }

        if (jwtProperties.getAccessTokenDuration().toHours() > 24) {
            log.warn("Access token duration is very long ({}). Consider reducing for better security.",
                    jwtProperties.getAccessTokenDuration());
        }

        if (jwtProperties.getRefreshTokenDuration().toDays() > 30) {
            log.warn("Refresh token duration is very long ({}). Consider reducing for better security.",
                    jwtProperties.getRefreshTokenDuration());
        }

        // Validar que refresh token sea mayor que access token
        if (jwtProperties.getRefreshTokenDuration().compareTo(jwtProperties.getAccessTokenDuration()) <= 0) {
            throw new IllegalStateException(
                    "Refresh token duration must be greater than access token duration");
        }

        // Validar límites razonables
        if (jwtProperties.getMaxActiveTokensPerUser() > 50) {
            log.warn("Max active tokens per user is very high ({}). This might impact performance.",
                    jwtProperties.getMaxActiveTokensPerUser());
        }

        if (jwtProperties.getMaxActiveTokensPerUser() < 1) {
            throw new IllegalStateException("Max active tokens per user must be at least 1");
        }

        // Log información sobre el secret (sin revelar el valor)
        log.info("  - Secret Source: {}", jwtProperties.isSecretFromVault() ? "Vault" : "Configuration");
        log.info("  - Secret Info: {}", jwtProperties.getSecretInfo());
    }

    /**
     * Bean principal de JwtProperties
     */
    @Bean
    @Primary
    public JwtProperties jwtProperties() {
        return jwtProperties;
    }

    /**
     * Bean para configuración de duración de access token (útil para inyección específica)
     */
    @Bean("accessTokenDuration")
    public java.time.Duration accessTokenDuration() {
        return jwtProperties.getAccessTokenDuration();
    }

    /**
     * Bean para configuración de duración de refresh token
     */
    @Bean("refreshTokenDuration")
    public java.time.Duration refreshTokenDuration() {
        return jwtProperties.getRefreshTokenDuration();
    }
}