package com.fcp.gateway.api;

import com.fcp.gateway.auth.ApiKeyAuthenticator;
import com.fcp.gateway.auth.AuthResult;
import com.fcp.infra.audit.AuditEntry;
import com.fcp.infra.audit.AuditLogger;
import com.fcp.infra.observability.TraceContext;
import com.fcp.infra.ratelimit.RateLimiter;

import java.time.Duration;
import java.util.function.Supplier;

/** API 网关过滤链：TraceId -> 限流 -> 认证/防重放 -> 审计 -> 业务（README 第三节 IAM/限流/防重放）。 */
public final class GatewayFilterChain {
    private final RateLimiter rateLimiter;
    private final ApiKeyAuthenticator authenticator;
    private final AuditLogger auditLogger;
    private final com.fcp.common.utils.IdGenerator ids;

    public GatewayFilterChain(RateLimiter rateLimiter, ApiKeyAuthenticator authenticator,
                              AuditLogger auditLogger, com.fcp.common.utils.IdGenerator ids) {
        this.rateLimiter = rateLimiter;
        this.authenticator = authenticator;
        this.auditLogger = auditLogger;
        this.ids = ids;
    }

    public GatewayResponse execute(GatewayRequest req, GatewayHandler handler, Supplier<Long> clock) {
        String traceId = req.headers() == null ? null : req.headers().get("X-Trace-Id");
        if (traceId == null) traceId = TraceContext.newTraceId();
        TraceContext.set(traceId);
        try {
            if (!rateLimiter.tryAcquire(req.apiKey() == null ? req.path() : req.apiKey())) {
                return GatewayResponse.reject(429, "rate limit exceeded", traceId);
            }
            AuthResult auth = authenticator.authenticate(req.apiKey(), req.method(), req.path(),
                    req.timestamp(), req.nonce(), req.body(), req.signature(), clock.get());
            if (!auth.authenticated()) {
                return GatewayResponse.reject(401, auth.reason(), traceId);
            }
            auditLogger.record(new AuditEntry(auth.principal(), "GATEWAY_ACCESS", "HTTP",
                    req.path(), traceId, req.method(), clock.get()));
            return handler.handle(req, traceId);
        } finally {
            TraceContext.clear();
        }
    }
}