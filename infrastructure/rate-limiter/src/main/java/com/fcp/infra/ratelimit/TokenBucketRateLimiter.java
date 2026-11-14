package com.fcp.infra.ratelimit;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 单机令牌桶；分布式限量在阶段四换 Redis 实现，接口不变。 */
public final class TokenBucketRateLimiter implements RateLimiter {
    private final long capacity;
    private final double refillPerNanos;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(long capacity, long refillTokens, Duration refillPeriod) {
        this.capacity = capacity;
        this.refillPerNanos = (double) refillTokens / refillPeriod.toNanos();
    }

    @Override
    public boolean tryAcquire(String key) {
        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket(capacity, System.nanoTime()));
        synchronized (b) {
            long now = System.nanoTime();
            double tokens = Math.min(capacity, b.tokens + (now - b.updatedAt) * refillPerNanos);
            b.updatedAt = now;
            if (tokens < 1.0) { b.tokens = tokens; return false; }
            b.tokens = tokens - 1.0;
            return true;
        }
    }

    private static final class Bucket {
        double tokens;
        long updatedAt;
        Bucket(double tokens, long updatedAt) { this.tokens = tokens; this.updatedAt = updatedAt; }
    }
}