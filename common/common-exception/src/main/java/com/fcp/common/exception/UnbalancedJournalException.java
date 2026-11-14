package com.fcp.common.exception;

public class UnbalancedJournalException extends DomainException {
    public UnbalancedJournalException(String detail) {
        super("UNBALANCED_JOURNAL", "Journal is not balanced: " + detail);
    }
}