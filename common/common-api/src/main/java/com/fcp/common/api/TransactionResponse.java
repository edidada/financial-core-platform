package com.fcp.common.api;

public record TransactionResponse(
        Long transactionId,
        String status,
        String idempotencyKey,
        Long amountMinor,
        String currency,
        Long journalId,
        String requestHash) {}