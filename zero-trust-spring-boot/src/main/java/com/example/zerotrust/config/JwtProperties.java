package com.example.zerotrust.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * ✅ ZERO TRUST JWT Properties - Secrets SOLO desde Vault
 *
 * 🔐 SEGURIDAD:
 * - Secret OBLIGATORIO desde external source (Vault)
 * - NO hardcoded fallbacks
 * - Fail-fast si no hay secret
 * - Preparado para rotación automática
 *
 * 🎯 ALINEADO CON TEST:
 * - Misma estructura que la clase del test
 * - Mismos métodos y propiedades
 * - Compatibilidad total para copy/paste directo
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    // ✅ SIN VALORES POR DEFECTO - obligatorio desde Vault
    private String secret;

    // ✅ Configuraciones con valores sensatos (no secretos)
    private Duration accessTokenDuration = Duration.ofMinutes(15);
    private Duration refreshTokenDuration = Duration.ofDays(7);
    private String issuer = "zero-trust-app";
    private boolean enableRefreshTokenRotation = true;

    // 🔐 Metadatos del secret para auditoría/rotación futura
    private String secretVersion;
    private String secretCreatedAt;
    private boolean secretFromVault = false;

    // Eliminar @PostConstruct
    // @PostConstruct
    // public void validateConfiguration() { ... }

    // Mover la validación a un método público
    public void validate() {
        System.out.println("🔐 Validating JWT Configuration... " + (secret != null ? "secret present" : "no secret"));
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "🚨 SECURITY VIOLATION: JWT secret is REQUIRED and must come from Vault. " +
                            "No hardcoded fallbacks allowed in Zero Trust architecture. " +
                            "Check your Vault configuration: app.jwt.secret"
            );
        }
        if (secret.length() < 64) {
            throw new IllegalStateException(
                    "🚨 SECURITY VIOLATION: JWT secret must be at least 64 characters for security. " +
                            "Current length: " + secret.length() + ". " +
                            "Generate a proper secret in Vault."
            );
        }

        // ✅ Log seguro (sin exponer el secret)
        System.out.println("🔐 JWT Configuration validated:");
        System.out.println("   Secret source: " + (secretFromVault ? "✅ Vault" : "⚠️ Other"));
        System.out.println("   Secret length: " + secret.length() + " characters");
        System.out.println("   Secret version: " + (secretVersion != null ? secretVersion : "unknown"));
        System.out.println("   Access token duration: " + accessTokenDuration);
        System.out.println("   Refresh token duration: " + refreshTokenDuration);
        System.out.println("   Rotation enabled: " + enableRefreshTokenRotation);
    }

    // =====================================================
    // GETTERS Y SETTERS - IDÉNTICOS AL TEST
    // =====================================================

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
        // 🔍 Detectar si viene de Vault basado en el patrón
        if (secret != null && (secret.startsWith("vault:") || secret.contains("vault-generated"))) {
            this.secretFromVault = true;
        }
    }

    public Duration getAccessTokenDuration() {
        return accessTokenDuration;
    }

    public void setAccessTokenDuration(Duration accessTokenDuration) {
        this.accessTokenDuration = accessTokenDuration;
    }

    public Duration getRefreshTokenDuration() {
        return refreshTokenDuration;
    }

    public void setRefreshTokenDuration(Duration refreshTokenDuration) {
        this.refreshTokenDuration = refreshTokenDuration;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public boolean isEnableRefreshTokenRotation() {
        return enableRefreshTokenRotation;
    }

    public void setEnableRefreshTokenRotation(boolean enableRefreshTokenRotation) {
        this.enableRefreshTokenRotation = enableRefreshTokenRotation;
    }

    // =====================================================
    // METADATOS PARA ROTACIÓN FUTURA
    // =====================================================

    public String getSecretVersion() {
        return secretVersion;
    }

    public void setSecretVersion(String secretVersion) {
        this.secretVersion = secretVersion;
    }

    public String getSecretCreatedAt() {
        return secretCreatedAt;
    }

    public void setSecretCreatedAt(String secretCreatedAt) {
        this.secretCreatedAt = secretCreatedAt;
    }

    public boolean isSecretFromVault() {
        return secretFromVault;
    }

    /**
     * ✅ MÉTODO PARA AUDITORÍA
     * NO expone el secret, solo metadatos
     */
    public String getSecretInfo() {
        return String.format("Secret{length=%d, source=%s, version=%s, created=%s}",
                secret != null ? secret.length() : 0,
                secretFromVault ? "Vault" : "Other",
                secretVersion != null ? secretVersion : "unknown",
                secretCreatedAt != null ? secretCreatedAt : "unknown"
        );
    }

    @Override
    public String toString() {
        // ✅ NUNCA exponer el secret en toString
        return String.format("JwtProperties{secretInfo='%s', accessTokenDuration=%s, refreshTokenDuration=%s, issuer='%s', rotationEnabled=%s}",
                getSecretInfo(), accessTokenDuration, refreshTokenDuration, issuer, enableRefreshTokenRotation);
    }
}