package com.carddemo.partner.service;

import com.carddemo.partner.dto.UsageResponse;
import com.carddemo.partner.entity.Partner;
import com.carddemo.partner.entity.PartnerDailyUsage;
import com.carddemo.partner.exception.PartnerException;
import com.carddemo.partner.repository.PartnerApiLogRepository;
import com.carddemo.partner.repository.PartnerDailyUsageRepository;
import com.carddemo.partner.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageService {

    private final PartnerRepository partnerRepository;
    private final PartnerDailyUsageRepository dailyUsageRepository;
    private final PartnerApiLogRepository apiLogRepository;
    private final RateLimitService rateLimitService;

    @Transactional(readOnly = true)
    public UsageResponse getUsage(Integer partnerId, int days) {
        Partner partner = partnerRepository.findByPartnerIdAndIsActiveTrue(partnerId)
                .orElseThrow(() -> PartnerException.partnerNotFound(partnerId));

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        LocalDateTime since = startDate.atStartOfDay();

        // Get daily usage breakdown
        List<PartnerDailyUsage> dailyUsages = dailyUsageRepository
                .findByPartnerIdAndUsageDateBetweenOrderByUsageDateDesc(partnerId, startDate, endDate);

        List<UsageResponse.DailyUsage> dailyBreakdown = dailyUsages.stream()
                .map(u -> UsageResponse.DailyUsage.builder()
                        .date(u.getUsageDate())
                        .totalRequests(u.getRequestCount())
                        .successfulRequests(u.getSuccessfulCount())
                        .failedRequests(u.getFailedCount())
                        .build())
                .collect(Collectors.toList());

        // Get real-time stats from Redis
        long requestsThisMinute = partner.getRateLimitPerMinute() -
                rateLimitService.getRemainingRequests(partnerId, partner.getRateLimitPerMinute());
        long requestsToday = rateLimitService.getDailyUsage(partnerId);
        long remainingToday = Math.max(0, partner.getDailyQuota() - requestsToday);

        // Get average response time
        Double avgResponseTime = apiLogRepository.avgResponseTimeSince(partnerId, since);

        // Get top endpoints
        List<Object[]> topEndpointsRaw = apiLogRepository.topEndpointsSince(partnerId, since);
        Map<String, Long> topEndpoints = new LinkedHashMap<>();
        for (Object[] row : topEndpointsRaw) {
            if (topEndpoints.size() >= 5) break;
            topEndpoints.put((String) row[0], (Long) row[1]);
        }

        return UsageResponse.builder()
                .partnerId(partnerId)
                .partnerName(partner.getPartnerName())
                .rateLimitPerMinute(partner.getRateLimitPerMinute())
                .dailyQuota(partner.getDailyQuota())
                .requestsThisMinute(requestsThisMinute)
                .requestsToday(requestsToday)
                .remainingToday(remainingToday)
                .avgResponseTimeMs(avgResponseTime)
                .topEndpoints(topEndpoints)
                .dailyUsage(dailyBreakdown)
                .build();
    }
}
