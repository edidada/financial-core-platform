package com.fcp.common.api;

public record CreateAccountRequest(
        Long tenantId,
        String accountNo,
        String currency,
        Long initialBalanceMinor) {}