package com.example.zerotrust.service;

import com.example.zerotrust.config.JwtProperties;
import com.example.zerotrust.exception.JwtException;
import com.example.zerotrust.model.dto.security.JwtClaims;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Clase base abstracta con métodos comunes para JWT
 */
public abstract class AbstractJwtService {

    protected static final Logger log = LoggerFactory.getLogger(AbstractJwtService.class);

    protected final JwtProperties jwtProperties;
    protected final SecretService secretService;

    protected AbstractJwtService(JwtProperties jwtProperties, SecretService secretService) {
        this.jwtProperties = jwtProperties;
        this.secretService = secretService;
    }

    /**
     * Construye un token JWT con los claims proporcionados
     */
    protected String buildToken(JwtClaims claims, java.time.Duration duration) {
        Instant now = Instant.now();
        Instant expiration = now.plus(duration);

        return Jwts.builder()
                .subject(claims.getSubject())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .id(UUID.randomUUID().toString()) // jti (JWT ID)

                // Claims personalizados Zero Trust
                .claim("username", claims.getUsername())
                .claim("roles", claims.getRoles())
                .claim("permissions", claims.getPermissions())
                .claim("deviceId", claims.getDeviceId())
                .claim("sessionId", claims.getSessionId())
                .claim("riskScore", claims.getRiskScore())
                .claim("ipAddress", claims.getIpAddress())
                .claim("tokenType", claims.getTokenType())
                .claim("context", claims.getContext())

                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parsea los claims de un token JWT
     */
    protected Claims parseTokenClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token expired for user: {}", e.getClaims().getSubject());
            throw new JwtException("Token expired", e);
        } catch (io.jsonwebtoken.JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    /**
     * Obtiene la clave de firma desde el SecretService
     */
    protected SecretKey getSigningKey() {
        String secret = secretService.getJwtSigningKey();
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Enriquece los claims con metadata de seguridad
     */
    protected void enrichClaimsWithSecurityData(JwtClaims claims) {
        // Generar session ID único si no existe
        if (claims.getSessionId() == null) {
            claims.setSessionId(UUID.randomUUID().toString());
        }

        // Inicializar risk score si no existe
        if (claims.getRiskScore() == null) {
            claims.setRiskScore(0.1); // Risk score bajo por defecto
        }

        // Timestamp actual
        claims.setIssuedAt(Instant.now());
    }

    /**
     * Convierte Claims de JWT a nuestro modelo JwtClaims
     */
    protected JwtClaims claimsToJwtClaims(Claims claims) {
        JwtClaims jwtClaims = new JwtClaims();

        jwtClaims.setSubject(claims.getSubject());
        jwtClaims.setUsername((String) claims.get("username"));
        jwtClaims.setRoles((List<String>) claims.get("roles"));
        jwtClaims.setPermissions((List<String>) claims.get("permissions"));
        jwtClaims.setDeviceId((String) claims.get("deviceId"));
        jwtClaims.setSessionId((String) claims.get("sessionId"));

        // Manejar conversión segura de riskScore
        Object riskScoreObj = claims.get("riskScore");
        if (riskScoreObj instanceof Number) {
            jwtClaims.setRiskScore(((Number) riskScoreObj).doubleValue());
        }

        jwtClaims.setIpAddress((String) claims.get("ipAddress"));
        jwtClaims.setTokenType((String) claims.get("tokenType"));
        jwtClaims.setContext((Map<String, Object>) claims.get("context"));
        jwtClaims.setIssuedAt(claims.getIssuedAt().toInstant());
        jwtClaims.setExpiresAt(claims.getExpiration().toInstant());

        return jwtClaims;
    }

    /**
     * Validaciones específicas Zero Trust
     */
    protected void validateZeroTrustClaims(JwtClaims claims) {
        // Validaciones específicas Zero Trust
        if (claims.getRiskScore() != null && claims.getRiskScore() > 0.9) {
            throw new JwtException("Risk score too high: " + claims.getRiskScore());
        }

        if (claims.getTokenType() == null) {
            throw new JwtException("Token type not specified");
        }
    }

    /**
     * Crea claims mínimos para refresh token
     */
    protected JwtClaims createRefreshClaims(JwtClaims accessClaims) {
        JwtClaims refreshClaims = new JwtClaims();

        // Solo información mínima en refresh token
        refreshClaims.setSubject(accessClaims.getSubject());
        refreshClaims.setUsername(accessClaims.getUsername());
        refreshClaims.setSessionId(accessClaims.getSessionId());
        refreshClaims.setDeviceId(accessClaims.getDeviceId());

        return refreshClaims;
    }

    /**
     * Crea claims de access token desde refresh token
     */
    protected JwtClaims createAccessClaimsFromRefresh(JwtClaims refreshClaims) {
        JwtClaims accessClaims = new JwtClaims();

        // Copiar información básica
        accessClaims.setSubject(refreshClaims.getSubject());
        accessClaims.setUsername(refreshClaims.getUsername());
        accessClaims.setSessionId(refreshClaims.getSessionId());
        accessClaims.setDeviceId(refreshClaims.getDeviceId());

        // TODO: Aquí deberíamos cargar roles/permisos actualizados desde la BD
        // Por ahora usamos valores por defecto
        accessClaims.setRoles(List.of("USER"));
        accessClaims.setPermissions(List.of("READ"));
        accessClaims.setRiskScore(0.1);

        return accessClaims;
    }
    /**
     * Genera refresh token con claims mínimos
     */
    protected String generateRefreshToken(JwtClaims claims) {
        // Crear claims mínimos para refresh token
        JwtClaims refreshClaims = createRefreshClaims(claims);
        refreshClaims.setTokenType("refresh");
        refreshClaims.setIssuedAt(Instant.now());
        refreshClaims.setExpiresAt(Instant.now().plus(jwtProperties.getRefreshTokenDuration()));

        // Construir token usando método base
        return buildToken(refreshClaims, jwtProperties.getRefreshTokenDuration());
    }
}