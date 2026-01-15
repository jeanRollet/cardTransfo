package com.carddemo.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bill Payment Entity
 *
 * Records bill payments made by customers.
 * Part of COBIL00C (Bill Payment) CICS transaction migration.
 */
@Entity
@Table(name = "bill_payments")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "payee_id", nullable = false)
    private Long payeeId;

    @Column(name = "amount", precision = 11, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "confirmation_number", length = 20)
    private String confirmationNumber;

    @Column(name = "memo", length = 200)
    private String memo;

    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(name = "recurring_frequency", length = 20)
    private String recurringFrequency;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_id", insertable = false, updatable = false)
    private BillPayee payee;

    public String getStatusName() {
        if (status == null) return "Unknown";
        return switch (status) {
            case "PENDING" -> "Pending";
            case "SCHEDULED" -> "Scheduled";
            case "COMPLETED" -> "Completed";
            case "CANCELLED" -> "Cancelled";
            case "FAILED" -> "Failed";
            default -> status;
        };
    }

    public String getAmountFormatted() {
        return amount != null ? String.format("$%.2f", amount) : "$0.00";
    }
}
