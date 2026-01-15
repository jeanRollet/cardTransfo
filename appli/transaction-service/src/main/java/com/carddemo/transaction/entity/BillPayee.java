package com.carddemo.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Bill Payee Entity
 *
 * Stores saved payees for bill payment functionality.
 * Part of COBIL00C (Bill Payment) CICS transaction migration.
 */
@Entity
@Table(name = "bill_payees")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillPayee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payee_id")
    private Long payeeId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "payee_name", length = 100, nullable = false)
    private String payeeName;

    @Column(name = "payee_type", length = 20, nullable = false)
    private String payeeType;

    @Column(name = "payee_account_number", length = 50)
    private String payeeAccountNumber;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public String getPayeeTypeName() {
        if (payeeType == null) return "Other";
        return switch (payeeType) {
            case "UTILITY" -> "Utility";
            case "CREDIT_CARD" -> "Credit Card";
            case "LOAN" -> "Loan";
            case "INSURANCE" -> "Insurance";
            case "TELECOM" -> "Telecom";
            case "SUBSCRIPTION" -> "Subscription";
            default -> payeeType;
        };
    }
}
