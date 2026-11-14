package com.fcp.gateway.api;

public record GatewayResponse(int status, String body, String traceId) {
    public static GatewayResponse reject(int status, String reason, String traceId) {
        return new GatewayResponse(status, reason, traceId);
    }
}