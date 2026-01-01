package com.carddemo.partner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Partner API Log entity - audit trail for all partner API requests.
 * Replaces SMF records from mainframe for API usage tracking.
 */
@Entity
@Table(name = "partner_api_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "partner_id")
    private Integer partnerId;

    @Column(name = "api_key_prefix", length = 12)
    private String apiKeyPrefix;

    @Column(name = "endpoint", length = 255)
    private String endpoint;

    @Column(name = "method", length = 10)
    private String method;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "request_body_size")
    private Integer requestBodySize;

    @Column(name = "response_body_size")
    private Integer responseBodySize;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "request_timestamp", updatable = false)
    private LocalDateTime requestTimestamp;
}
