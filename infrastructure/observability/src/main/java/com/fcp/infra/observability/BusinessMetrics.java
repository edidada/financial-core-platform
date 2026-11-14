package com.fcp.infra.observability;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/** 轻量指标门面；生产接 Micrometer / Prometheus（阶段四 deploy/monitoring）。 */
public final class BusinessMetrics {
    private final ConcurrentMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DoubleAdder> sums = new ConcurrentHashMap<>();

    public void increment(String name) { counters.computeIfAbsent(name, k -> new AtomicLong()).incrementAndGet(); }
    public long counter(String name) {
        AtomicLong c = counters.get(name);
        return c == null ? 0L : c.get();
    }
    public void observeLatency(String name, long nanos) {
        sums.computeIfAbsent(name, k -> new DoubleAdder()).add(nanos / 1_000_000.0);
    }
    public double totalMillis(String name) {
        DoubleAdder d = sums.get(name);
        return d == null ? 0.0 : d.sum();
    }
}