package com.carddemo.partner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Partner Daily Usage entity - tracks daily API usage for quota enforcement.
 */
@Entity
@Table(name = "partner_daily_usage",
       uniqueConstraints = @UniqueConstraint(columnNames = {"partner_id", "usage_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerDailyUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Integer usageId;

    @Column(name = "partner_id", nullable = false)
    private Integer partnerId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "request_count")
    @Builder.Default
    private Integer requestCount = 0;

    @Column(name = "successful_count")
    @Builder.Default
    private Integer successfulCount = 0;

    @Column(name = "failed_count")
    @Builder.Default
    private Integer failedCount = 0;

    public void incrementRequest(boolean success) {
        this.requestCount++;
        if (success) {
            this.successfulCount++;
        } else {
            this.failedCount++;
        }
    }
}
