package com.fcp.engine.recon;

import java.util.List;

public record MatchResult(long leftCount, long rightCount, long matched, List<ReconciliationDiff> diffs) {
    public boolean clean() { return diffs.isEmpty(); }
}