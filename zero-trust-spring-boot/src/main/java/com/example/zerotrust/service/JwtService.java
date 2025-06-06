package com.example.zerotrust.service;

import com.example.zerotrust.config.JwtProperties;
import com.example.zerotrust.exception.JwtException;
import com.example.zerotrust.model.dto.security.JwtClaims;
import com.example.zerotrust.model.dto.security.TokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Implementación del servicio JWT Zero Trust
 * Extiende AbstractJwtService e implementa IJwtService
 */
@Service
public class JwtService extends com.example.zerotrust.service.AbstractJwtService implements com.example.zerotrust.service.IJwtService {

    private final com.example.zerotrust.service.TokenRevocationService tokenRevocationService;

    @Autowired
    public JwtService(JwtProperties jwtProperties,
                      com.example.zerotrust.service.SecretService secretService,
                      com.example.zerotrust.service.TokenRevocationService tokenRevocationService) {
        super(jwtProperties, secretService);
        this.tokenRevocationService = tokenRevocationService;
    }



    @Override
    public String generateAccessToken(JwtClaims claims) {
        claims.setTokenType("access");
        claims.setIssuedAt(Instant.now());
        claims.setExpiresAt(Instant.now().plus(jwtProperties.getAccessTokenDuration()));

        return buildToken(claims, jwtProperties.getAccessTokenDuration());
    }

    @Override
    public JwtClaims validateAndParseToken(String token) {
        try {
            // Verificar si está revocado
            if (tokenRevocationService.isTokenRevoked(token)) {
                throw new JwtException("Token has been revoked");
            }

            // Parsear y validar token (usando método de clase abstracta)
            Claims claims = parseTokenClaims(token);

            // Convertir a nuestro modelo (usando método de clase abstracta)
            JwtClaims jwtClaims = claimsToJwtClaims(claims);

            // Validaciones adicionales Zero Trust (usando método de clase abstracta)
            validateZeroTrustClaims(jwtClaims);

            log.debug("Token validated successfully for user: {}", jwtClaims.getUsername());
            return jwtClaims;

        } catch (JwtException e) {
            // Re-lanzar nuestras excepciones personalizadas
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error validating token", e);
            throw new JwtException("Token validation failed", e);
        }
    }

    @Override
    public TokenResponse generateTokenPair(JwtClaims userClaims) {
        log.debug("Generating token pair for user: {}", userClaims.getUsername());

        // Enriquecer claims con metadata de seguridad
        enrichClaimsWithSecurityData(userClaims);

        // Generar access token (corta duración)
        String accessToken = generateAccessToken(userClaims);

        // Generar refresh token (larga duración) ← USAR MÉTODO PROTEGIDO
        String refreshToken = generateRefreshToken(userClaims);

        // Calcular expiración
        Instant expiresAt = Instant.now().plus(jwtProperties.getAccessTokenDuration());
        long expiresIn = jwtProperties.getAccessTokenDuration().getSeconds();

        log.info("Token pair generated successfully for user: {} (session: {})",
                userClaims.getUsername(), userClaims.getSessionId());

        return new TokenResponse(accessToken, refreshToken, expiresIn, expiresAt);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        log.debug("Refreshing access token");

        // Validar refresh token
        JwtClaims refreshClaims = validateAndParseToken(refreshToken);

        if (!"refresh".equals(refreshClaims.getTokenType())) {
            throw new JwtException("Invalid token type for refresh");
        }

        // Crear nuevos claims para access token
        JwtClaims newClaims = createAccessClaimsFromRefresh(refreshClaims);

        // Generar nuevo access token
        String newAccessToken = generateAccessToken(newClaims);

        String newRefreshToken = refreshToken; // Reutilizar por defecto

        // Rotar refresh token si está habilitado
        if (jwtProperties.isEnableRefreshTokenRotation()) {
            newRefreshToken = generateRefreshToken(newClaims); // ← USAR MÉTODO PROTEGIDO
            // Revocar el refresh token anterior
            tokenRevocationService.revokeToken(refreshToken, "rotated");
        }

        Instant expiresAt = Instant.now().plus(jwtProperties.getAccessTokenDuration());
        long expiresIn = jwtProperties.getAccessTokenDuration().getSeconds();

        log.info("Token refreshed successfully for user: {}", newClaims.getUsername());

        return new TokenResponse(newAccessToken, newRefreshToken, expiresIn, expiresAt);
    }

    @Override
    public void revokeToken(String token, String reason) {
        try {
            JwtClaims claims = validateAndParseToken(token);
            tokenRevocationService.revokeToken(token, reason);
            log.info("Token revoked for user: {} (reason: {})", claims.getUsername(), reason);
        } catch (Exception e) {
            log.warn("Failed to revoke token: {}", e.getMessage());
            // Aún así lo agregamos a blacklist por seguridad
            tokenRevocationService.revokeToken(token, "failed_validation_" + reason);
        }
    }

    @Override
    public String extractTokenId(String token) {
        try {
            Claims claims = parseTokenClaims(token);
            return claims.getId();
        } catch (Exception e) {
            log.warn("Failed to extract token ID: {}", e.getMessage());
            return null;
        }
    }

}