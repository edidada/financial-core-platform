package com.fcp.common.api;

public record AccountResponse(
        Long accountId,
        String accountNo,
        String currency,
        long openingBalanceMinor,
        long availableBalanceMinor,
        long frozenBalanceMinor,
        long version) {}