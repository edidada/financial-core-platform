package com.fcp.tests;

import com.fcp.engine.rule.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {
    @Test void rejectOnLargeAmount() {
        RuleEngine e = new RuleEngine(List.of(
                new Rule("BLOCK_LARGE", 1, "large", f -> f.amountMinor() > 1000L)));
        assertEquals("REJECT", e.evaluate(new RuleFact().put("amountMinor", 2000L)).outcome());
        assertEquals("PASS", e.evaluate(new RuleFact().put("amountMinor", 500L)).outcome());
    }

    @Test void priorityOrdering() {
        RuleEngine e = new RuleEngine(List.of(
                new Rule("REVIEW", 50, "late", f -> true),
                new Rule("BLOCK", 1, "early", f -> false)));
        RuleDecision d = e.evaluate(new RuleFact());
        assertEquals("REVIEW", d.outcome());
    }
}