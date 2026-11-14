package com.fcp.common.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 金额值对象：以最小货币单位（分）存储，禁止 double/float。
 * 借贷平衡因此可用整数相等严格判定（ADR-0002）。
 */
public final class Money {
    private final long minor;
    private final Currency currency;

    private Money(long minor, Currency currency) {
        this.minor = minor;
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    public static Money ofMinor(long minor, Currency currency) {
        return new Money(minor, currency);
    }

    public static Money ofMajor(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "amount");
        return new Money(amount.movePointRight(currency.exponent()).longValueExact(), currency);
    }

    public long minor() { return minor; }
    public Currency currency() { return currency; }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(minor, other.minor), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(minor, other.minor), currency);
    }

    public boolean isPositive() { return minor > 0; }

    private void requireSameCurrency(Money other) {
        if (currency != other.currency) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money m)) return false;
        return minor == m.minor && currency == m.currency;
    }

    @Override
    public int hashCode() { return Objects.hash(minor, currency); }

    @Override
    public String toString() { return minor + " " + currency.code(); }
}