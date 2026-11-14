package com.fcp.engine.rule;

import java.util.List;

public record RuleDecision(String outcome, List<String> hitRules, long evaluatedAtMillis) {
    public boolean isBlocked() { return "REJECT".equals(outcome) || "REVIEW".equals(outcome); }
}