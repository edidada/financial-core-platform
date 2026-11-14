package com.fcp.engine.rule;

import java.util.function.Predicate;

public record Rule(String code, int priority, String description, Predicate<RuleFact> condition) {
    public boolean matches(RuleFact fact) { return condition.test(fact); }
}