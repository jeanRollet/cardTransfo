package com.carddemo.partner.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PartnerException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public PartnerException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static PartnerException partnerNotFound(Integer partnerId) {
        return new PartnerException(
                "Partner not found: " + partnerId,
                HttpStatus.NOT_FOUND,
                "PARTNER_NOT_FOUND"
        );
    }

    public static PartnerException partnerNameExists(String name) {
        return new PartnerException(
                "Partner name already exists: " + name,
                HttpStatus.CONFLICT,
                "PARTNER_NAME_EXISTS"
        );
    }

    public static PartnerException emailExists(String email) {
        return new PartnerException(
                "Email already registered: " + email,
                HttpStatus.CONFLICT,
                "EMAIL_EXISTS"
        );
    }

    public static PartnerException apiKeyNotFound(Integer keyId) {
        return new PartnerException(
                "API key not found: " + keyId,
                HttpStatus.NOT_FOUND,
                "API_KEY_NOT_FOUND"
        );
    }

    public static PartnerException invalidScope(String scope) {
        return new PartnerException(
                "Scope not allowed for this partner: " + scope,
                HttpStatus.BAD_REQUEST,
                "INVALID_SCOPE"
        );
    }

    public static PartnerException invalidApiKey() {
        return new PartnerException(
                "Invalid or expired API key",
                HttpStatus.UNAUTHORIZED,
                "INVALID_API_KEY"
        );
    }

    public static PartnerException rateLimitExceeded(long retryAfterSeconds) {
        return new PartnerException(
                "Rate limit exceeded. Try again in " + retryAfterSeconds + " seconds",
                HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMIT_EXCEEDED"
        );
    }

    public static PartnerException dailyQuotaExceeded() {
        return new PartnerException(
                "Daily quota exceeded. Try again tomorrow",
                HttpStatus.TOO_MANY_REQUESTS,
                "DAILY_QUOTA_EXCEEDED"
        );
    }

    public static PartnerException insufficientScope(String requiredScope) {
        return new PartnerException(
                "Insufficient scope. Required: " + requiredScope,
                HttpStatus.FORBIDDEN,
                "INSUFFICIENT_SCOPE"
        );
    }
}
