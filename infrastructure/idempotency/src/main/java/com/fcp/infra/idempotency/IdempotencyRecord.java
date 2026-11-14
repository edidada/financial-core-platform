package com.fcp.infra.idempotency;

import java.sql.Timestamp;

/** 幂等记录；responseStatus 为 null 表示业务仍在处理中（ADR-0005）。 */
public record IdempotencyRecord(
        String idempotencyKey,
        long tenantId,
        long transactionId,
        String requestHash,
        Integer responseStatus,
        String responseBody,
        Timestamp createdAt) {

    public boolean completed() {
        return responseStatus != null;
    }
}