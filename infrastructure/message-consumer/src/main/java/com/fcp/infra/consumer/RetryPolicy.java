package com.fcp.infra.consumer;

import java.time.Duration;

/** 指数退避重试；超过最大次数进死信队列（阶段二）。 */
public final class RetryPolicy {
    private final int maxAttempts;
    private final Duration initialBackoff;
    private final double multiplier;

    public RetryPolicy(int maxAttempts, Duration initialBackoff, double multiplier) {
        this.maxAttempts = maxAttempts;
        this.initialBackoff = initialBackoff;
        this.multiplier = multiplier;
    }

    public static RetryPolicy defaults() { return new RetryPolicy(5, Duration.ofMillis(100), 2.0); }

    public Duration backoffFor(int attempt) {
        long millis = (long) (initialBackoff.toMillis() * Math.pow(multiplier, Math.max(0, attempt - 1)));
        return Duration.ofMillis(millis);
    }

    public boolean shouldRetry(int attempt) { return attempt < maxAttempts; }
    public int maxAttempts() { return maxAttempts; }
}