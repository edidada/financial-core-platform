package com.fcp.gateway.api;

@FunctionalInterface
public interface GatewayHandler {
    GatewayResponse handle(GatewayRequest request, String traceId);
}