package com.fcp.common.exception;

public class InsufficientBalanceException extends DomainException {
    public InsufficientBalanceException(long accountId) {
        super("INSUFFICIENT_BALANCE", "Insufficient balance for account " + accountId);
    }
}