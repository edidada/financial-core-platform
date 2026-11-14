package com.fcp.service.reporting;

public record DailyReport(String reportDate, String currency, long totalDebitMinor,
                          long totalCreditMinor, long transactionCount, boolean balanced) {}