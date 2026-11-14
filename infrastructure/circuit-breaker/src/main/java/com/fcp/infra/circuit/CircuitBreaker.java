package com.fcp.infra.circuit;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** 轻量熔断器：失败率阈值 -> OPEN -> 冷却后 HALF_OPEN 试探。 */
public final class CircuitBreaker {
    private final int failureThreshold;
    private final Duration openDuration;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicReference<Instant> openedAt = new AtomicReference<>(Instant.EPOCH);

    public CircuitBreaker(int failureThreshold, Duration openDuration) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
    }

    public CircuitState state() {
        CircuitState s = state.get();
        if (s == CircuitState.OPEN && Instant.now().isAfter(openedAt.get().plus(openDuration))) {
            if (state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) return CircuitState.HALF_OPEN;
        }
        return state.get();
    }

    public <T> T execute(Supplier<T> action) {
        CircuitState s = state();
        if (s == CircuitState.OPEN) throw new IllegalStateException("Circuit is OPEN");
        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (RuntimeException e) {
            onFailure();
            throw e;
        }
    }

    private void onSuccess() {
        consecutiveFailures.set(0);
        state.set(CircuitState.CLOSED);
    }

    private void onFailure() {
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            openedAt.set(Instant.now());
            state.set(CircuitState.OPEN);
        }
    }
}