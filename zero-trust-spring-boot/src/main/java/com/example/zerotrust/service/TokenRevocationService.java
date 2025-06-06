package com.example.zerotrust.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para gestión de revocación de tokens (implementación básica)
 */
@Service
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);

    // Cache en memoria para desarrollo
    private final ConcurrentHashMap<String, String> revokedTokens = new ConcurrentHashMap<>();

    /**
     * Verifica si un token está revocado
     */
    public boolean isTokenRevoked(String token) {
        boolean revoked = revokedTokens.containsKey(getTokenHash(token));
        if (revoked) {
            log.debug("Token found in revocation list");
        }
        return revoked;
    }

    /**
     * Revoca un token - MÉTODO QUE FALTABA
     */
    public void revokeToken(String token, String reason) {
        String tokenHash = getTokenHash(token);
        revokedTokens.put(tokenHash, reason);
        log.info("Token revoked (reason: {})", reason);
    }

    /**
     * Limpia tokens expirados
     */
    public void cleanupExpiredTokens() {
        log.debug("Cleanup expired tokens called");
    }

    /**
     * Obtiene hash del token
     */
    private String getTokenHash(String token) {
        // Implementación simple - usar solo los últimos 8 caracteres
        return token.length() > 8 ? token.substring(token.length() - 8) : token;
    }
}