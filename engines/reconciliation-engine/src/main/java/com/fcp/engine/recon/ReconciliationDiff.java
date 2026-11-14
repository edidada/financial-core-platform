package com.fcp.engine.recon;

public record ReconciliationDiff(String bizKey, DiffType type, Long leftAmount, Long rightAmount) {}