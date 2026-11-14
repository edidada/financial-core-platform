package com.fcp.service.liquidity;

public record LiquiditySnapshot(long snapshotId, long accountId, long availableMinor,
                                long pendingOutMinor, String currency, long asOfMillis) {
    public long projectedMinor() { return availableMinor - pendingOutMinor; }
}