package com.carddemo.auth.service;

import com.carddemo.auth.config.JwtProperties;
import com.carddemo.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Service
 *
 * Handles JWT token generation, validation, and blacklisting.
 * Uses Redis for token blacklist (logout invalidation).
 *
 * @author CardDemo Transformation Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String REFRESH_PREFIX = "jwt:refresh:";

    /**
     * Generate access token for user
     *
     * @param user The authenticated user
     * @param sessionId Unique session identifier
     * @return JWT access token
     */
    public String generateAccessToken(User user, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        claims.put("userType", user.getUserType());
        claims.put("customerId", user.getCustomerId()); // null for admins
        claims.put("sessionId", sessionId);
        claims.put("type", "access");

        return buildToken(claims, user.getUserId(), jwtProperties.getExpiration());
    }

    /**
     * Generate refresh token
     *
     * @param userId User ID
     * @param sessionId Session identifier
     * @return JWT refresh token
     */
    public String generateRefreshToken(String userId, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sessionId", sessionId);
        claims.put("type", "refresh");

        String token = buildToken(claims, userId, jwtProperties.getRefreshExpiration());

        // Store refresh token in Redis for validation
        String key = REFRESH_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, token,
                Duration.ofMillis(jwtProperties.getRefreshExpiration()));

        return token;
    }

    /**
     * Build JWT token with claims
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate access token
     *
     * @param token JWT token
     * @return true if valid
     */
    public boolean validateToken(String token) {
        try {
            log.info("Validating token: {}...", token.substring(0, Math.min(50, token.length())));

            // Check if token is blacklisted
            String jti = extractJti(token);
            if (jti != null && isBlacklisted(jti)) {
                log.warn("Token is blacklisted: {}", jti);
                return false;
            }

            // Parse and validate token
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            log.info("Token is valid");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate refresh token
     *
     * @param refreshToken Refresh token
     * @return true if valid and stored in Redis
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Claims claims = extractAllClaims(refreshToken);
            String sessionId = claims.get("sessionId", String.class);

            if (sessionId == null) {
                return false;
            }

            // Check if refresh token matches stored token
            String key = REFRESH_PREFIX + sessionId;
            String storedToken = redisTemplate.opsForValue().get(key);

            return refreshToken.equals(storedToken);
        } catch (JwtException e) {
            log.debug("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract user ID from token
     */
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract session ID from token
     */
    public String extractSessionId(String token) {
        return extractAllClaims(token).get("sessionId", String.class);
    }

    /**
     * Extract user type from token
     */
    public String extractUserType(String token) {
        return extractAllClaims(token).get("userType", String.class);
    }

    /**
     * Extract customer ID from token (null for admins)
     */
    public Integer extractCustomerId(String token) {
        return extractAllClaims(token).get("customerId", Integer.class);
    }

    /**
     * Extract token ID (jti)
     */
    public String extractJti(String token) {
        try {
            return extractAllClaims(token).getId();
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * Extract all claims from token
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Blacklist token (for logout)
     *
     * @param token JWT token to blacklist
     */
    public void blacklistToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String jti = claims.getId();
            Date expiration = claims.getExpiration();

            if (jti != null && expiration != null) {
                // Store in blacklist until token expires
                long ttl = expiration.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    String key = BLACKLIST_PREFIX + jti;
                    redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(ttl));
                    log.debug("Token blacklisted: {}", jti);
                }
            }

            // Also delete refresh token
            String sessionId = claims.get("sessionId", String.class);
            if (sessionId != null) {
                redisTemplate.delete(REFRESH_PREFIX + sessionId);
            }
        } catch (JwtException e) {
            log.warn("Failed to blacklist token: {}", e.getMessage());
        }
    }

    /**
     * Check if token is blacklisted
     */
    private boolean isBlacklisted(String jti) {
        String key = BLACKLIST_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Remove Bearer prefix from token
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}
