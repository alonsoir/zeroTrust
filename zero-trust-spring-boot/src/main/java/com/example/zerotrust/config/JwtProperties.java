package com.example.zerotrust.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

/**
 * Configuración de propiedades JWT para Zero Trust
 * Mapea propiedades desde application.yml bajo el prefijo 'app.jwt'
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

    /**
     * Emisor del token JWT (iss claim)
     */
    @NotBlank(message = "JWT issuer cannot be blank")
    private String issuer = "zero-trust-service";

    /**
     * Duración del access token (tokens de corta duración)
     */
    @NotNull(message = "Access token duration cannot be null")
    private Duration accessTokenDuration = Duration.ofMinutes(15);

    /**
     * Duración del refresh token (tokens de larga duración)
     */
    @NotNull(message = "Refresh token duration cannot be null")
    private Duration refreshTokenDuration = Duration.ofDays(7);

    /**
     * Habilitar rotación automática de refresh tokens
     */
    private boolean enableRefreshTokenRotation = true;

    /**
     * Algoritmo de firma a utilizar
     */
    @NotBlank(message = "Signing algorithm cannot be blank")
    private String signingAlgorithm = "HS256";

    /**
     * Tiempo de gracia para validación de tokens (clock skew)
     */
    private Duration clockSkew = Duration.ofMinutes(1);

    /**
     * Audiencia esperada del token (aud claim)
     */
    private String audience = "zero-trust-clients";

    /**
     * Prefijo para el header Authorization
     */
    @NotBlank(message = "Token prefix cannot be blank")
    private String tokenPrefix = "Bearer ";

    /**
     * Nombre del header donde se envía el token
     */
    @NotBlank(message = "Token header cannot be blank")
    private String tokenHeader = "Authorization";

    /**
     * Máximo número de tokens activos por usuario
     */
    @Positive(message = "Max active tokens must be positive")
    private int maxActiveTokensPerUser = 5;

    /**
     * Habilitar blacklist de tokens
     */
    private boolean enableTokenBlacklist = true;

    /**
     * TTL para la blacklist en Redis
     */
    private Duration blacklistTtl = Duration.ofDays(30);

    /**
     * Secret para firmar JWT (puede venir de Vault)
     */
    private String secret;

    /**
     * Indica si el secret viene de Vault
     */
    private boolean secretFromVault = false;

    // Constructores
    public JwtProperties() {
    }

    // Getters y Setters
    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
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

    public boolean isEnableRefreshTokenRotation() {
        return enableRefreshTokenRotation;
    }

    public void setEnableRefreshTokenRotation(boolean enableRefreshTokenRotation) {
        this.enableRefreshTokenRotation = enableRefreshTokenRotation;
    }

    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }

    public void setSigningAlgorithm(String signingAlgorithm) {
        this.signingAlgorithm = signingAlgorithm;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public String getTokenHeader() {
        return tokenHeader;
    }

    public void setTokenHeader(String tokenHeader) {
        this.tokenHeader = tokenHeader;
    }

    public int getMaxActiveTokensPerUser() {
        return maxActiveTokensPerUser;
    }

    public void setMaxActiveTokensPerUser(int maxActiveTokensPerUser) {
        this.maxActiveTokensPerUser = maxActiveTokensPerUser;
    }

    public boolean isEnableTokenBlacklist() {
        return enableTokenBlacklist;
    }

    public void setEnableTokenBlacklist(boolean enableTokenBlacklist) {
        this.enableTokenBlacklist = enableTokenBlacklist;
    }

    public Duration getBlacklistTtl() {
        return blacklistTtl;
    }

    public void setBlacklistTtl(Duration blacklistTtl) {
        this.blacklistTtl = blacklistTtl;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isSecretFromVault() {
        return secretFromVault;
    }

    public void setSecretFromVault(boolean secretFromVault) {
        this.secretFromVault = secretFromVault;
    }

    // Métodos alternativos sin prefijo get/is (para compatibilidad con código existente)
    public String secret() {
        return getSecret();
    }

    public String issuer() {
        return getIssuer();
    }

    public String algorithm() {
        return getSigningAlgorithm();
    }

    public Duration accessTokenDuration() {
        return getAccessTokenDuration();
    }

    public Duration refreshTokenDuration() {
        return getRefreshTokenDuration();
    }

    public boolean enableRefreshTokenRotation() {
        return isEnableRefreshTokenRotation();
    }

    /**
     * Método de validación
     */
    public boolean validate() {
        return isValid();
    }

    /**
     * Obtiene información del secret para debugging/logging
     */
    public String getSecretInfo() {
        if (secret == null) {
            return "Secret not configured";
        }

        String source = secretFromVault ? "Vault" : "Configuration";
        String length = secret.length() > 0 ? String.valueOf(secret.length()) : "0";
        return String.format("Secret loaded from %s (length: %s characters)", source, length);
    }

    // Métodos de utilidad
    /**
     * Obtiene la duración del access token en segundos
     */
    public long getAccessTokenDurationSeconds() {
        return accessTokenDuration.getSeconds();
    }

    /**
     * Obtiene la duración del refresh token en segundos
     */
    public long getRefreshTokenDurationSeconds() {
        return refreshTokenDuration.getSeconds();
    }

    /**
     * Verifica si la configuración es válida
     */
    public boolean isValid() {
        return issuer != null && !issuer.trim().isEmpty() &&
                accessTokenDuration != null && !accessTokenDuration.isNegative() &&
                refreshTokenDuration != null && !refreshTokenDuration.isNegative() &&
                accessTokenDuration.compareTo(refreshTokenDuration) < 0 && // Access token debe ser menor que refresh
                secret != null && !secret.trim().isEmpty(); // Secret debe estar configurado
    }

    @Override
    public String toString() {
        return "JwtProperties{" +
                "issuer='" + issuer + '\'' +
                ", accessTokenDuration=" + accessTokenDuration +
                ", refreshTokenDuration=" + refreshTokenDuration +
                ", enableRefreshTokenRotation=" + enableRefreshTokenRotation +
                ", signingAlgorithm='" + signingAlgorithm + '\'' +
                ", clockSkew=" + clockSkew +
                ", audience='" + audience + '\'' +
                ", tokenPrefix='" + tokenPrefix + '\'' +
                ", tokenHeader='" + tokenHeader + '\'' +
                ", maxActiveTokensPerUser=" + maxActiveTokensPerUser +
                ", enableTokenBlacklist=" + enableTokenBlacklist +
                ", blacklistTtl=" + blacklistTtl +
                ", secretConfigured=" + (secret != null && !secret.isEmpty()) +
                ", secretFromVault=" + secretFromVault +
                '}';
    }
}