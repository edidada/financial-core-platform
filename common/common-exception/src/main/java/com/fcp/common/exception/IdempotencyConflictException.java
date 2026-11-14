package com.fcp.common.exception;

public class IdempotencyConflictException extends DomainException {
    public IdempotencyConflictException(String key) {
        super("IDEMPOTENCY_CONFLICT", "Idempotency key reused with different payload: " + key);
    }
}