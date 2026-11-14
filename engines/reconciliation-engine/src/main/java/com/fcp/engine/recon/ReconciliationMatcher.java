package com.fcp.engine.recon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 差异匹配引擎：哈希连接 O(n+m)，可重算、可复现（README 第四节 Reconciliation）。
 * 不漏单：左右两侧任一缺失都会产生 diff；重复键也会被识别。
 */
public final class ReconciliationMatcher {
    public MatchResult match(List<ReconciliationRecord> left, List<ReconciliationRecord> right) {
        Map<String, ReconciliationRecord> leftMap = new HashMap<>();
        Map<String, Integer> leftDup = new HashMap<>();
        for (ReconciliationRecord r : left) {
            if (leftMap.putIfAbsent(r.bizKey(), r) != null) leftDup.merge(r.bizKey(), 1, Integer::sum);
        }
        Map<String, ReconciliationRecord> rightMap = new HashMap<>();
        Map<String, Integer> rightDup = new HashMap<>();
        for (ReconciliationRecord r : right) {
            if (rightMap.putIfAbsent(r.bizKey(), r) != null) rightDup.merge(r.bizKey(), 1, Integer::sum);
        }
        List<ReconciliationDiff> diffs = new ArrayList<>();
        long matched = 0;
        for (Map.Entry<String, ReconciliationRecord> e : leftMap.entrySet()) {
            ReconciliationRecord l = e.getValue();
            ReconciliationRecord r = rightMap.get(e.getKey());
            if (r == null) { diffs.add(new ReconciliationDiff(e.getKey(), DiffType.MISSING_IN_RIGHT, l.amountMinor(), null)); continue; }
            if (!l.currency().equals(r.currency())) {
                diffs.add(new ReconciliationDiff(e.getKey(), DiffType.CURRENCY_MISMATCH, l.amountMinor(), r.amountMinor()));
            } else if (l.amountMinor() != r.amountMinor()) {
                diffs.add(new ReconciliationDiff(e.getKey(), DiffType.AMOUNT_MISMATCH, l.amountMinor(), r.amountMinor()));
            } else { matched++; }
        }
        for (String k : rightMap.keySet()) {
            if (!leftMap.containsKey(k)) diffs.add(new ReconciliationDiff(k, DiffType.MISSING_IN_LEFT, null, rightMap.get(k).amountMinor()));
        }
        for (String k : leftDup.keySet()) diffs.add(new ReconciliationDiff(k, DiffType.DUPLICATE, leftMap.get(k).amountMinor(), null));
        for (String k : rightDup.keySet()) diffs.add(new ReconciliationDiff(k, DiffType.DUPLICATE, null, rightMap.get(k).amountMinor()));
        return new MatchResult(left.size(), right.size(), matched, List.copyOf(diffs));
    }
}