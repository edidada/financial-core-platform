package com.fcp.service.refund;

public record RefundOrder(long refundId, long originalTransactionId, long amountMinor,
                          String currency, String status, String idempotencyKey) {}