package com.fcp.engine.rule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 按优先级顺序执行规则，命中即短路；用于风控、限额、手续费。 */
public final class RuleEngine {
    private final List<Rule> rules;

    public RuleEngine(List<Rule> rules) {
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(Rule::priority))
                .toList();
    }

    public RuleDecision evaluate(RuleFact fact) {
        List<String> hits = new ArrayList<>();
        for (Rule r : rules) {
            if (r.matches(fact)) {
                hits.add(r.code());
                if ("REJECT".equals(r.code()) || r.code().startsWith("BLOCK")) {
                    return new RuleDecision("REJECT", List.copyOf(hits), System.currentTimeMillis());
                }
            }
        }
        return new RuleDecision(hits.isEmpty() ? "PASS" : "REVIEW", List.copyOf(hits), System.currentTimeMillis());
    }
}