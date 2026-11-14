package com.fcp.common.api;

public record TransferRequest(
        Long tenantId,
        String idempotencyKey,
        Long payerAccountId,
        Long payeeAccountId,
        Long amountMinor,
        String currency) {

    /** 参与幂等指纹的规范化请求串（不含幂等键本身）。 */
    public String canonical() {
        return tenantId + "|" + payerAccountId + "|" + payeeAccountId + "|" + amountMinor + "|" + currency;
    }
}