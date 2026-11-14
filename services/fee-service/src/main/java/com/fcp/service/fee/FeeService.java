package com.fcp.service.fee;

import com.fcp.engine.rule.Rule;
import com.fcp.engine.rule.RuleEngine;
import com.fcp.engine.rule.RuleFact;
import org.springframework.stereotype.Service;

import java.util.List;

/** 手续费计算：基点费率 + 上下限，整数运算避免浮点误差 (ADR-0002)。 */
@Service
public class FeeService {
    private static final long MIN_FEE = 0L;
    private static final long MAX_FEE = 5000L;  // 50.00 元封顶
    private final RuleEngine ruleEngine;

    public FeeService() {
        this.ruleEngine = new RuleEngine(List.of(
                new Rule("FEE_HIGH_VALUE", 10, "large transfer",
                        f -> f.amountMinor() > 1_000_000L)));
    }

    public FeeQuote quote(long amountMinor, String currency) {
        if (amountMinor <= 0) throw new IllegalArgumentException("amount must be positive");
        long fee = amountMinor * 10 / 10_000;   // 10 bps = 0.1%
        fee = Math.max(MIN_FEE, Math.min(MAX_FEE, fee));
        var decision = ruleEngine.evaluate(new RuleFact().put("amountMinor", amountMinor).put("currency", currency));
        return new FeeQuote(amountMinor, currency, fee, amountMinor - fee,
                decision.hitRules().isEmpty() ? "FEE_STANDARD" : decision.hitRules().get(0));
    }
}