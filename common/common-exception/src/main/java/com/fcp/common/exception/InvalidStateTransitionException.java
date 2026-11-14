package com.fcp.common.exception;

import com.fcp.common.domain.TransactionStatus;

public class InvalidStateTransitionException extends DomainException {
    public InvalidStateTransitionException(TransactionStatus from, TransactionStatus to) {
        super("INVALID_STATE_TRANSITION", "Illegal transition " + from + " -> " + to);
    }
}