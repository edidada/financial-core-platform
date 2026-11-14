package com.fcp.service.limit;

public record LimitCheck(boolean allowed, String reason, long remainingMinor) {}