package com.fcp.gateway.api;

import java.util.Map;

public record GatewayRequest(String method, String path, String apiKey, String timestamp,
                             String nonce, String signature, String body, Map<String, String> headers) {}