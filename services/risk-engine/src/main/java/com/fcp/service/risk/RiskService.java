package com.fcp.service.risk;

import com.fcp.engine.rule.Rule;
import com.fcp.engine.rule.RuleDecision;
import com.fcp.engine.rule.RuleEngine;
import com.fcp.engine.rule.RuleFact;
import com.fcp.engine.routing.RoutingEngine;
import com.fcp.infra.circuit.CircuitBreaker;
import com.fcp.infra.circuit.DegradableRisk;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/** 实时风控：确定延迟预算 + 降级策略（README 第四节 Risk）。 */
@Service
public class RiskService {
    private static final long LATENCY_BUDGET_MS = 20;
    private final RuleEngine engine;
    private final RoutingEngine routing = new RoutingEngine();
    private final CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofSeconds(30));

    public RiskService() {
        this.engine = new RuleEngine(List.of(
                new Rule("BLOCK_LARGE", 1, "single transfer > 1,000,000 minor",
                        f -> f.amountMinor() > 1_000_000L),
                new Rule("BLOCK_HIGH_RISK", 2, "riskScore >= 80",
                        f -> f.intValue("riskScore", 0) >= 80),
                new Rule("REVIEW_NEW_DEVICE", 50, "new device",
                        f -> f.intValue("deviceAgeDays", 999) < 1)));
    }

    public RiskDecision evaluate(long amountMinor, String currency, int riskScore, int deviceAgeDays) {
        long start = System.currentTimeMillis();
        RuleFact fact = new RuleFact()
                .put("amountMinor", amountMinor).put("currency", currency)
                .put("riskScore", riskScore).put("deviceAgeDays", deviceAgeDays);
        RuleDecision d = DegradableRisk.call(breaker,
                () -> engine.evaluate(fact),
                new RuleDecision("REVIEW", List.of("DEGRADED"), System.currentTimeMillis()));
        long latency = System.currentTimeMillis() - start;
        boolean degraded = d.hitRules().contains("DEGRADED");
        String route = routing.route(new com.fcp.engine.routing.RoutingContext(currency, amountMinor, null, fact)).channel();
        return new RiskDecision(d.outcome(), d.outcome().equals("REJECT") ? 100 : riskScore,
                route, latency, degraded || latency > LATENCY_BUDGET_MS);
    }
}