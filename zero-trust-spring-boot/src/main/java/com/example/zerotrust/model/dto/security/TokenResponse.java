package com.example.zerotrust.model.dto.security;

import java.time.Instant;

public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private Instant expiresAt;
    private String scope;

    public TokenResponse(String accessToken, String refreshToken, long expiresIn, Instant expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.expiresAt = expiresAt;
        this.scope = "read write";
    }

    // Getters
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getScope() { return scope; }
}