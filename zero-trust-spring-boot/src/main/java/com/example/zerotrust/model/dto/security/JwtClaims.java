package com.example.zerotrust.model.dto.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class JwtClaims {
    private String subject;           // User ID
    private String username;
    private List<String> roles;
    private List<String> permissions;
    private String deviceId;
    private String sessionId;
    private Double riskScore;
    private String ipAddress;
    private Map<String, Object> context;
    private Instant issuedAt;
    private Instant expiresAt;
    private String tokenType;         // "access" or "refresh"

    // Constructor
    public JwtClaims() {}

    public JwtClaims(String subject, String username, List<String> roles) {
        this.subject = subject;
        this.username = username;
        this.roles = roles;
        this.issuedAt = Instant.now();
    }

    // Getters y setters
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
}