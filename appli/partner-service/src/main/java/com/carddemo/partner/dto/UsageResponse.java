package com.carddemo.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageResponse {

    private Integer partnerId;
    private String partnerName;

    // Current limits
    private Integer rateLimitPerMinute;
    private Integer dailyQuota;

    // Current usage
    private Long requestsThisMinute;
    private Long requestsToday;
    private Long remainingToday;

    // Stats
    private Double avgResponseTimeMs;
    private Map<String, Long> topEndpoints;

    // Daily breakdown
    private List<DailyUsage> dailyUsage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyUsage {
        private LocalDate date;
        private Integer totalRequests;
        private Integer successfulRequests;
        private Integer failedRequests;
    }
}
