package com.fcp.common.exception;

public class DuplicateTransactionException extends DomainException {
    public DuplicateTransactionException(String bizId) {
        super("DUPLICATE_TRANSACTION", "Transaction already posted: " + bizId);
    }
}