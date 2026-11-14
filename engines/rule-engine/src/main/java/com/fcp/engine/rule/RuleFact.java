package com.fcp.engine.rule;

import java.util.HashMap;
import java.util.Map;

/** 规则事实：金额、账户属性、设备、历史统计等特征的键值容器。 */
public final class RuleFact {
    private final Map<String, Object> values = new HashMap<>();

    public RuleFact put(String k, Object v) { values.put(k, v); return this; }
    public Object get(String k) { return values.get(k); }

    public long amountMinor() { return ((Number) values.getOrDefault("amountMinor", 0L)).longValue(); }
    public String currency() { return (String) values.getOrDefault("currency", "CNY"); }
    public int intValue(String k, int def) {
        Object v = values.get(k);
        return v instanceof Number n ? n.intValue() : def;
    }
}