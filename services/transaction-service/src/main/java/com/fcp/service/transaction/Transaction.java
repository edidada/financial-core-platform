package com.fcp.service.transaction;

import com.fcp.common.domain.Currency;
import com.fcp.common.domain.TransactionStatus;
import com.fcp.common.domain.TransactionType;

import java.sql.Timestamp;

public record Transaction(
        long transactionId,
        long tenantId,
        String idempotencyKey,
        String requestHash,
        TransactionType type,
        TransactionStatus status,
        Long payerAccountId,
        Long payeeAccountId,
        long amountMinor,
        Currency currency,
        Long journalId,
        long version,
        Timestamp nextRetryAt,
        Timestamp createdAt,
        Timestamp updatedAt) {}