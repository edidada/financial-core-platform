package com.fcp.service.clearing;

public record ClearingResult(long batchId, int itemCount, long netSum, boolean balanced, long elapsedMillis) {}