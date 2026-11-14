package com.fcp.common.api;

public record ReverseRequest(
        Long tenantId,
        String idempotencyKey,
        Long originalTransactionId,
        String reason) {

    public String canonical() {
        return tenantId + "|" + originalTransactionId + "|" + reason;
    }
}