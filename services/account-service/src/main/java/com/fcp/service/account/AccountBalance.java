package com.fcp.service.account;

import java.sql.Timestamp;

/** openingBalance 为开户期初余额，是 I-2 的校验基线。 */
public record AccountBalance(
        long accountId,
        long openingBalance,
        long availableBalance,
        long frozenBalance,
        long version,
        Timestamp updatedAt) {}