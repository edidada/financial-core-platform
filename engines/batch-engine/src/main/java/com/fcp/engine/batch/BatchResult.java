package com.fcp.engine.batch;

public record BatchResult(long total, long succeeded, long failed, long elapsedMillis) {
    public boolean allSucceeded() { return failed == 0; }
}