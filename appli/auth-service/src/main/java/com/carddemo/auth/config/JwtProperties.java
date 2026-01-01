package com.carddemo.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT Configuration Properties
 *
 * Binds to application.yml jwt.* properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Secret key for signing JWT tokens (HS256)
     * Must be at least 256 bits (32 characters)
     */
    private String secret = "your-256-bit-secret-key-change-this-in-production";

    /**
     * Access token expiration time in milliseconds
     * Default: 15 minutes (900000ms)
     */
    private long expiration = 900000;

    /**
     * Refresh token expiration time in milliseconds
     * Default: 7 days (604800000ms)
     */
    private long refreshExpiration = 604800000;

    /**
     * Token issuer
     */
    private String issuer = "carddemo-auth-service";

    /**
     * Token audience
     */
    private String audience = "carddemo-app";
}
