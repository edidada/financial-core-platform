package com.fcp.service.reconciliation;

import com.fcp.engine.recon.MatchResult;

public record ReconciliationReport(long taskId, String window, MatchResult result, long elapsedMillis) {
    public boolean clean() { return result.clean(); }
}