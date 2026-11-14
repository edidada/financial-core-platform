package com.fcp.service.fee;

public record FeeQuote(long amountMinor, String currency, long feeMinor, long netMinor, String ruleCode) {}