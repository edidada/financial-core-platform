package com.fcp.gateway.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 时间戳窗口 + nonce 去重的防重放组件。 */
public final class ReplayGuard {
    private final Duration window;
    private final Map<String, Instant> seen = new ConcurrentHashMap<>();

    public ReplayGuard(Duration window) { this.window = window; }

    public boolean accept(String nonce, long timestampMillis, long nowMillis) {
        Instant now = Instant.ofEpochMilli(nowMillis);
        Instant ts = Instant.ofEpochMilli(timestampMillis);
        if (ts.isBefore(now.minus(window)) || ts.isAfter(now.plus(window))) return false;
        if (seen.putIfAbsent(nonce, ts) != null) return false;
        seen.entrySet().removeIf(e -> e.getValue().isBefore(now.minus(window)));
        return true;
    }
}