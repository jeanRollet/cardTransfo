package com.carddemo.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Authentication Exception
 *
 * Custom exception for authentication errors with predefined error types.
 * Maps to CICS RESP codes from the original COBOL programs.
 *
 * CICS Equivalent Error Handling:
 * - DFHRESP(NOTFND) → USER_NOT_FOUND
 * - Password mismatch → INVALID_CREDENTIALS
 * - SEC-USR-STATUS = 'L' → ACCOUNT_LOCKED
 * - SEC-USR-STATUS = 'D' → ACCOUNT_DISABLED
 *
 * @author CardDemo Transformation Team
 */
@Getter
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AuthException(String message) {
        super(message);
        this.status = HttpStatus.UNAUTHORIZED;
        this.errorCode = "AUTH_ERROR";
    }

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = "AUTH_ERROR";
    }

    public AuthException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    // ============================================================================
    // Predefined Authentication Exceptions
    // ============================================================================

    /**
     * Invalid credentials (wrong password)
     * CICS: Password comparison failure in COSGN00C
     */
    public static AuthException invalidCredentials() {
        return new AuthException(
                "Invalid user ID or password",
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS"
        );
    }

    /**
     * User not found in database
     * CICS: DFHRESP(NOTFND) when reading USRSEC file
     */
    public static AuthException userNotFound() {
        return new AuthException(
                "User not found",
                HttpStatus.UNAUTHORIZED,
                "USER_NOT_FOUND"
        );
    }

    /**
     * Account locked due to failed login attempts
     * CICS: SEC-USR-STATUS = 'L'
     */
    public static AuthException accountLocked() {
        return new AuthException(
                "Account is locked due to too many failed login attempts. Please try again later.",
                HttpStatus.FORBIDDEN,
                "ACCOUNT_LOCKED"
        );
    }

    /**
     * Account disabled by administrator
     * CICS: SEC-USR-STATUS = 'D' or is_active = false
     */
    public static AuthException accountDisabled() {
        return new AuthException(
                "Account is disabled. Please contact administrator.",
                HttpStatus.FORBIDDEN,
                "ACCOUNT_DISABLED"
        );
    }

    /**
     * Invalid or malformed JWT token
     */
    public static AuthException invalidToken() {
        return new AuthException(
                "Invalid or malformed token",
                HttpStatus.UNAUTHORIZED,
                "INVALID_TOKEN"
        );
    }

    /**
     * JWT token has expired
     */
    public static AuthException tokenExpired() {
        return new AuthException(
                "Token has expired. Please login again.",
                HttpStatus.UNAUTHORIZED,
                "TOKEN_EXPIRED"
        );
    }

    /**
     * Invalid refresh token
     */
    public static AuthException refreshTokenInvalid() {
        return new AuthException(
                "Invalid or expired refresh token",
                HttpStatus.UNAUTHORIZED,
                "INVALID_REFRESH_TOKEN"
        );
    }

    /**
     * Session not found or expired in Redis
     */
    public static AuthException sessionNotFound() {
        return new AuthException(
                "Session not found or expired",
                HttpStatus.UNAUTHORIZED,
                "SESSION_NOT_FOUND"
        );
    }

    /**
     * Token blacklisted (user logged out)
     */
    public static AuthException tokenBlacklisted() {
        return new AuthException(
                "Token has been invalidated",
                HttpStatus.UNAUTHORIZED,
                "TOKEN_BLACKLISTED"
        );
    }
}
