package com.fcp.service.settlement;

public record SettlementInstruction(long settlementId, long clearingBatchId, long accountId,
                                    long amountMinor, String currency, String channel,
                                    SettlementStatus status) {}