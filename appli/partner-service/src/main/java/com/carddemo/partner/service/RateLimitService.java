package com.carddemo.partner.service;

import com.carddemo.partner.entity.PartnerDailyUsage;
import com.carddemo.partner.repository.PartnerDailyUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PartnerDailyUsageRepository dailyUsageRepository;

    @Value("${partner.api.rate-limit-window-seconds:60}")
    private int rateLimitWindowSeconds;

    private static final String RATE_LIMIT_KEY_PREFIX = "partner:ratelimit:";
    private static final String DAILY_QUOTA_KEY_PREFIX = "partner:dailyquota:";

    /**
     * Check if the partner has exceeded their rate limit (requests per minute)
     */
    public boolean isRateLimited(Integer partnerId, int limitPerMinute) {
        String key = RATE_LIMIT_KEY_PREFIX + partnerId;

        Long currentCount = redisTemplate.opsForValue().increment(key);
        if (currentCount == null) {
            currentCount = 1L;
        }

        // Set expiry on first request
        if (currentCount == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(rateLimitWindowSeconds));
        }

        return currentCount > limitPerMinute;
    }

    /**
     * Get remaining requests in current window
     */
    public long getRemainingRequests(Integer partnerId, int limitPerMinute) {
        String key = RATE_LIMIT_KEY_PREFIX + partnerId;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return limitPerMinute;
        }

        long used = Long.parseLong(value);
        return Math.max(0, limitPerMinute - used);
    }

    /**
     * Get time until rate limit resets (in seconds)
     */
    public long getResetTimeSeconds(Integer partnerId) {
        String key = RATE_LIMIT_KEY_PREFIX + partnerId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : rateLimitWindowSeconds;
    }

    /**
     * Check if the partner has exceeded their daily quota
     */
    public boolean isDailyQuotaExceeded(Integer partnerId, int dailyQuota) {
        String key = DAILY_QUOTA_KEY_PREFIX + partnerId + ":" + LocalDate.now();

        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return false;
        }

        return Long.parseLong(value) >= dailyQuota;
    }

    /**
     * Increment daily usage counter
     */
    public void incrementDailyUsage(Integer partnerId) {
        String key = DAILY_QUOTA_KEY_PREFIX + partnerId + ":" + LocalDate.now();

        Long currentCount = redisTemplate.opsForValue().increment(key);

        // Set expiry at midnight (24 hours from now as a safe default)
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, Duration.ofHours(25));
        }
    }

    /**
     * Get current daily usage
     */
    public long getDailyUsage(Integer partnerId) {
        String key = DAILY_QUOTA_KEY_PREFIX + partnerId + ":" + LocalDate.now();
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    /**
     * Record usage in database for historical tracking
     */
    @Transactional
    public void recordUsage(Integer partnerId, boolean success) {
        LocalDate today = LocalDate.now();

        // Try to update existing record
        int updated = dailyUsageRepository.incrementUsage(
                partnerId,
                today,
                success ? 1 : 0,
                success ? 0 : 1
        );

        // If no record exists, create one
        if (updated == 0) {
            PartnerDailyUsage usage = PartnerDailyUsage.builder()
                    .partnerId(partnerId)
                    .usageDate(today)
                    .requestCount(1)
                    .successfulCount(success ? 1 : 0)
                    .failedCount(success ? 0 : 1)
                    .build();
            dailyUsageRepository.save(usage);
        }
    }

    /**
     * Combined rate limit check - returns null if OK, error message if limited
     */
    public RateLimitResult checkRateLimit(Integer partnerId, int limitPerMinute, int dailyQuota) {
        // Check rate limit first
        if (isRateLimited(partnerId, limitPerMinute)) {
            long retryAfter = getResetTimeSeconds(partnerId);
            return new RateLimitResult(
                    true,
                    "RATE_LIMIT_EXCEEDED",
                    String.format("Rate limit exceeded. Try again in %d seconds.", retryAfter),
                    retryAfter,
                    limitPerMinute,
                    0
            );
        }

        // Check daily quota
        if (isDailyQuotaExceeded(partnerId, dailyQuota)) {
            return new RateLimitResult(
                    true,
                    "DAILY_QUOTA_EXCEEDED",
                    "Daily quota exceeded. Try again tomorrow.",
                    0,
                    dailyQuota,
                    0
            );
        }

        // Increment counters
        incrementDailyUsage(partnerId);

        long remaining = getRemainingRequests(partnerId, limitPerMinute);
        return new RateLimitResult(false, null, null, 0, limitPerMinute, remaining);
    }

    public record RateLimitResult(
            boolean limited,
            String errorCode,
            String message,
            long retryAfterSeconds,
            int limit,
            long remaining
    ) {}
}
