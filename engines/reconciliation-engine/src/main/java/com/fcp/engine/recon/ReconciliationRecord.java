package com.fcp.engine.recon;

public record ReconciliationRecord(String bizKey, long amountMinor, String currency, String source) {}