package com.carddemo.transaction.dto;

import com.carddemo.transaction.entity.BillPayment;
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
public class BillPaymentResponse {
    private Long paymentId;
    private Long accountId;
    private Long payeeId;
    private String payeeName;
    private BigDecimal amount;
    private String amountFormatted;
    private LocalDate paymentDate;
    private LocalDate scheduledDate;
    private String status;
    private String statusName;
    private String confirmationNumber;
    private String memo;
    private Boolean isRecurring;
    private String recurringFrequency;
    private LocalDate nextPaymentDate;

    public static BillPaymentResponse fromEntity(BillPayment entity, String payeeName) {
        return BillPaymentResponse.builder()
                .paymentId(entity.getPaymentId())
                .accountId(entity.getAccountId())
                .payeeId(entity.getPayeeId())
                .payeeName(payeeName)
                .amount(entity.getAmount())
                .amountFormatted(entity.getAmountFormatted())
                .paymentDate(entity.getPaymentDate())
                .scheduledDate(entity.getScheduledDate())
                .status(entity.getStatus())
                .statusName(entity.getStatusName())
                .confirmationNumber(entity.getConfirmationNumber())
                .memo(entity.getMemo())
                .isRecurring(entity.getIsRecurring())
                .recurringFrequency(entity.getRecurringFrequency())
                .nextPaymentDate(entity.getNextPaymentDate())
                .build();
    }
}
