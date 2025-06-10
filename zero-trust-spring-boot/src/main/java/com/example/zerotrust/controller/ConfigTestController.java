package com.example.zerotrust.controller;

import com.example.zerotrust.config.JwtProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 🎮 Controller Estado del Arte
 *
 * ✅ CARACTERÍSTICAS MODERNAS:
 * - Usa @ConfigurationProperties en lugar de @Value
 * - Immutable properties (Record)
 * - Zero Trust security aware
 * - Información segura (no expone secrets)
 */
@RestController
public class ConfigTestController {

    private final JwtProperties jwtProperties;

    /**
     * ✅ Constructor injection (mejor práctica)
     */
    public ConfigTestController(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * ✅ Endpoint de configuración (SEGURO - no expone secrets)
     */
    @GetMapping("/test/config")
    public Map<String, Object> getConfig() {
        return Map.of(
                // ✅ SEGURO: Solo prefijo del secret (nunca el valor completo)
                "jwtSecretPrefix", jwtProperties.secret().substring(0, Math.min(20, jwtProperties.secret().length())) + "...",
                "jwtSecretLength", jwtProperties.secret().length(),
                "jwtSecretSource", jwtProperties.isSecretFromVault() ? "✅ Vault" : "⚠️ Fallback",
                "jwtIssuer", jwtProperties.issuer(),
                "jwtAlgorithm", jwtProperties.algorithm(),
                "accessTokenDuration", jwtProperties.accessTokenDuration().toString(),
                "refreshTokenDuration", jwtProperties.refreshTokenDuration().toString(),
                "rotationEnabled", jwtProperties.enableRefreshTokenRotation(),
                "configurationValid", isConfigurationValid()
        );
    }

    /**
     * ✅ Endpoint de salud de configuración
     */
    @GetMapping("/test/config/health")
    public Map<String, Object> getConfigHealth() {
        try {
            jwtProperties.validate();
            return Map.of(
                    "status", "✅ HEALTHY",
                    "secretSource", jwtProperties.isSecretFromVault() ? "Vault" : "Fallback",
                    "secretLength", jwtProperties.secret().length(),
                    "lastValidated", java.time.Instant.now().toString()
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "❌ UNHEALTHY",
                    "error", e.getMessage(),
                    "lastValidated", java.time.Instant.now().toString()
            );
        }
    }

    /**
     * ✅ Información detallada de configuración (sin secrets)
     */
    @GetMapping("/test/config/details")
    public Map<String, Object> getConfigDetails() {
        return Map.of(
                "jwtProperties", Map.of(
                        "secretInfo", jwtProperties.getSecretInfo(),
                        "issuer", jwtProperties.issuer(),
                        "algorithm", jwtProperties.algorithm(),
                        "accessTokenDuration", jwtProperties.accessTokenDuration().toString(),
                        "refreshTokenDuration", jwtProperties.refreshTokenDuration().toString(),
                        "rotationEnabled", jwtProperties.enableRefreshTokenRotation(),
                        "fromVault", jwtProperties.isSecretFromVault()
                ),
                "security", Map.of(
                        "secretCompliant", jwtProperties.secret().length() >= 64,
                        "algorithmSecure", "HS256".equals(jwtProperties.algorithm()),
                        "issuerSet", jwtProperties.issuer() != null && !jwtProperties.issuer().isEmpty(),
                        "rotationEnabled", jwtProperties.enableRefreshTokenRotation()
                ),
                "metadata", Map.of(
                        "configurationClass", jwtProperties.getClass().getSimpleName(),
                        "timestamp", java.time.Instant.now().toString(),
                        "profile", System.getProperty("spring.profiles.active", "unknown")
                )
        );
    }

    /**
     * ✅ Helper method para validar configuración
     */
    private boolean isConfigurationValid() {
        try {
            jwtProperties.validate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}