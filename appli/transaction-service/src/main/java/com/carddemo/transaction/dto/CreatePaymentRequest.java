package com.carddemo.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Payee ID is required")
    private Long payeeId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    private String memo;

    private Boolean isRecurring;

    private String recurringFrequency;
}
