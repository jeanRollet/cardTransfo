package com.carddemo.transaction.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new transaction
 * Replaces CICS COTRN02C input map
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransactionRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "SALE|PYMT|RFND|CASH|FEE", message = "Invalid transaction type")
    private String transactionType;

    @NotBlank(message = "Transaction category is required")
    private String transactionCategory;

    private String transactionSource;

    @Size(max = 100, message = "Description cannot exceed 100 characters")
    private String transactionDesc;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 9, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    private String merchantId;

    @Size(max = 50, message = "Merchant name cannot exceed 50 characters")
    private String merchantName;

    private String merchantCity;

    private String merchantZip;

    // Card number (last 4 digits or full)
    private String cardNumber;
}
