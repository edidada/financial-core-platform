package com.fcp.service.clearing;

public record ClearingItem(long accountId, long debitMinor, long creditMinor, long netMinor, String currency) {}