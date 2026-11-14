package com.fcp.tests;

import com.fcp.engine.recon.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReconciliationEngineTest {
    @Test void cleanWhenEqual() {
        var r = new ReconciliationMatcher().match(
                List.of(new ReconciliationRecord("A", 100L, "CNY", "L")),
                List.of(new ReconciliationRecord("A", 100L, "CNY", "R")));
        assertTrue(r.clean());
        assertEquals(1, r.matched());
    }

    @Test void detectsMissingAmountMismatchAndDuplicate() {
        var r = new ReconciliationMatcher().match(
                List.of(new ReconciliationRecord("A", 100L, "CNY", "L"),
                        new ReconciliationRecord("B", 50L, "CNY", "L"),
                        new ReconciliationRecord("B", 50L, "CNY", "L")),
                List.of(new ReconciliationRecord("A", 90L, "CNY", "R"),
                        new ReconciliationRecord("C", 70L, "CNY", "R")));
        assertFalse(r.clean());
        assertTrue(r.diffs().stream().anyMatch(d -> d.type() == DiffType.AMOUNT_MISMATCH));
        assertTrue(r.diffs().stream().anyMatch(d -> d.type() == DiffType.MISSING_IN_RIGHT));
        assertTrue(r.diffs().stream().anyMatch(d -> d.type() == DiffType.MISSING_IN_LEFT));
        assertTrue(r.diffs().stream().anyMatch(d -> d.type() == DiffType.DUPLICATE));
    }
}