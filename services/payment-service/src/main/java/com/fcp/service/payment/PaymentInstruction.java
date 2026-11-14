package com.fcp.service.payment;

import com.fcp.common.domain.Currency;

public record PaymentInstruction(
        long paymentId,
        long transactionId,
        long payerAccountId,
        long payeeAccountId,
        long amountMinor,
        Currency currency,
        String channel,
        String externalRef,
        PaymentStatus status,
        long version) {}