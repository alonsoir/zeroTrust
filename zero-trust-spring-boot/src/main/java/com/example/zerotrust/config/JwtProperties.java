package com.example.zerotrust.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret = "zero-trust-default-secret-key-change-in-production-must-be-at-least-64-characters";
    private Duration accessTokenDuration = Duration.ofMinutes(15);
    private Duration refreshTokenDuration = Duration.ofDays(7);
    private String issuer = "zero-trust-app";
    private boolean enableRefreshTokenRotation = true;

    // Getters y setters
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public Duration getAccessTokenDuration() { return accessTokenDuration; }
    public void setAccessTokenDuration(Duration accessTokenDuration) { this.accessTokenDuration = accessTokenDuration; }

    public Duration getRefreshTokenDuration() { return refreshTokenDuration; }
    public void setRefreshTokenDuration(Duration refreshTokenDuration) { this.refreshTokenDuration = refreshTokenDuration; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public boolean isEnableRefreshTokenRotation() { return enableRefreshTokenRotation; }
    public void setEnableRefreshTokenRotation(boolean enableRefreshTokenRotation) { this.enableRefreshTokenRotation = enableRefreshTokenRotation; }
}