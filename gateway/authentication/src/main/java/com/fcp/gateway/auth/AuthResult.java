package com.fcp.gateway.auth;

public record AuthResult(boolean authenticated, String principal, String reason) {
    public static AuthResult ok(String principal) { return new AuthResult(true, principal, null); }
    public static AuthResult reject(String reason) { return new AuthResult(false, null, reason); }
}