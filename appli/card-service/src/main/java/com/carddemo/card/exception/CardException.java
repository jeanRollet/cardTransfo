package com.carddemo.card.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Card Service Exception
 *
 * Maps to CICS RESP codes from COCRDLIC/COCRDSLC transactions.
 */
@Getter
public class CardException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public CardException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    // Factory methods for common errors

    public static CardException cardNotFound(String cardNumber) {
        return new CardException(
                "CARD_NOT_FOUND",
                "Card not found: ****" + cardNumber.substring(cardNumber.length() - 4),
                HttpStatus.NOT_FOUND
        );
    }

    public static CardException accountNotFound(Long accountId) {
        return new CardException(
                "ACCOUNT_NOT_FOUND",
                "Account not found: " + accountId,
                HttpStatus.NOT_FOUND
        );
    }

    public static CardException cardAlreadyBlocked(String cardNumber) {
        return new CardException(
                "CARD_ALREADY_BLOCKED",
                "Card is already blocked",
                HttpStatus.BAD_REQUEST
        );
    }

    public static CardException cardExpired(String cardNumber) {
        return new CardException(
                "CARD_EXPIRED",
                "Card has expired",
                HttpStatus.BAD_REQUEST
        );
    }

    public static CardException invalidOperation(String message) {
        return new CardException(
                "INVALID_OPERATION",
                message,
                HttpStatus.BAD_REQUEST
        );
    }

    public static CardException unauthorized() {
        return new CardException(
                "UNAUTHORIZED",
                "Authentication required",
                HttpStatus.UNAUTHORIZED
        );
    }

    public static CardException forbidden() {
        return new CardException(
                "FORBIDDEN",
                "Access denied",
                HttpStatus.FORBIDDEN
        );
    }
}
