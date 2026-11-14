package com.fcp.engine.routing;

import com.fcp.engine.rule.RuleFact;

import java.util.List;

/** 交易路由引擎：按币种/金额/渠道提示选择处理通道，带明确降级顺序。 */
public final class RoutingEngine {
    private final List<Route> routes;

    public RoutingEngine() {
        this.routes = List.of(
                new Route("INTERNAL_LEDGER", "same-currency internal transfer", 10),
                new Route("CLEARING_NETWORK", "cross-institution clearing", 20),
                new Route("MANUAL_REVIEW", "fallback", 99));
    }

    public Route route(RoutingContext ctx) {
        if (ctx.facts() != null && ctx.facts().intValue("riskScore", 0) >= 80) {
            return new Route("MANUAL_REVIEW", "high risk score", 1);
        }
        if ("CNY".equalsIgnoreCase(ctx.currency()) && ctx.amountMinor() <= 5_000_000L) {
            return routes.get(0);
        }
        return routes.get(1);
    }

    public List<Route> routes() { return routes; }
}