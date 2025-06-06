package com.example.zerotrust.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Servicio para gestión de secretos (implementación básica)
 * TODO: Integrar con Vault completamente
 */
@Service
public class SecretService {

    private static final Logger log = LoggerFactory.getLogger(SecretService.class);

    @Value("${app.jwt.secret:dev-jwt-secret-key-at-least-64-characters-for-development-use}")
    private String jwtSecret;

    // Cache temporal para secretos
    private final Cache<String, String> secretCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(100)
            .build();

    /**
     * Obtiene la clave de firma JWT
     */
    public String getJwtSigningKey() {
        return secretCache.get("jwt-signing-key", k -> {
            log.debug("Loading JWT signing key");
            // TODO: Obtener del Vault real
            return jwtSecret;
        });
    }

    /**
     * Obtiene clave de encriptación
     */
    public String getEncryptionKey() {
        return secretCache.get("encryption-key", k -> {
            log.debug("Loading encryption key");
            // TODO: Obtener del Vault real
            return "default-encryption-key-32-bytes-minimum";
        });
    }

    /**
     * Refresca todos los secretos del cache
     */
    public void refreshSecrets() {
        log.debug("Refreshing secrets cache");
        secretCache.invalidateAll();
    }
}