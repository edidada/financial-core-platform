package com.fcp.common.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void majorToMinorIsExact() {
        assertEquals(10000L, Money.ofMajor(new BigDecimal("100.00"), Currency.CNY).minor());
    }

    @Test
    void arithmeticStaysInMinorUnits() {
        Money a = Money.ofMinor(10000L, Currency.CNY);
        Money b = Money.ofMinor(8000L, Currency.CNY);
        assertEquals(2000L, a.subtract(b).minor());
        assertEquals(18000L, a.add(b).minor());
    }

    @Test
    void currencyMismatchRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.ofMinor(1L, Currency.CNY).add(Money.ofMinor(1L, Currency.USD)));
    }
}