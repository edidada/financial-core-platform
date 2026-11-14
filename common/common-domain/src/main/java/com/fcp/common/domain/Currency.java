package com.fcp.common.domain;

/** ISO-4217 币种与最小货币单位精度。阶段一仅启用 CNY（2 位小数）。 */
public enum Currency {
    CNY("CNY", 2),
    USD("USD", 2),
    JPY("JPY", 0);

    private final String code;
    private final int exponent;

    Currency(String code, int exponent) {
        this.code = code;
        this.exponent = exponent;
    }

    public String code() { return code; }
    public int exponent() { return exponent; }

    public static Currency of(String code) {
        for (Currency c : values()) {
            if (c.code.equalsIgnoreCase(code)) return c;
        }
        throw new IllegalArgumentException("Unsupported currency: " + code);
    }
}