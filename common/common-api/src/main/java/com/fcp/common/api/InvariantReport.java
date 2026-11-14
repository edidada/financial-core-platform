package com.fcp.common.api;

import java.util.Map;

/** 资金不变量 I-1~I-4 的离线校验结果；violations 全为 0 才 healthy。 */
public record InvariantReport(Map<String, Long> violations, boolean healthy) {

    public static InvariantReport of(Map<String, Long> violations) {
        boolean healthy = violations.values().stream().allMatch(c -> c == 0L);
        return new InvariantReport(violations, healthy);
    }
}