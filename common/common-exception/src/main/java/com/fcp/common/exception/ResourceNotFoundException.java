package com.fcp.common.exception;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String what, Object id) {
        super("NOT_FOUND", what + " not found: " + id);
    }
}