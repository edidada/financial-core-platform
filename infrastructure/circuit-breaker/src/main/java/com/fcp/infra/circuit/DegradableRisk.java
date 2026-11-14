package com.fcp.infra.circuit;

import java.util.function.Supplier;

/** 风控等旁路依赖的降级包装：主路径异常时返回保守默认值（README 第四节 Risk 降级策略）。 */
public final class DegradableRisk {
    private DegradableRisk() {}

    public static <T> T call(CircuitBreaker breaker, Supplier<T> primary, T fallback) {
        try {
            return breaker.execute(primary);
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}