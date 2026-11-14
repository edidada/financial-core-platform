package com.fcp.infra.audit;

public record AuditEntry(
        String actor,
        String action,
        String resourceType,
        String resourceId,
        String traceId,
        String detail,
        long happenedAtMillis) {}