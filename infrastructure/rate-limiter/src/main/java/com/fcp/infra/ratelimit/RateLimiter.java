package com.fcp.infra.ratelimit;

public interface RateLimiter {
    boolean tryAcquire(String key);
}