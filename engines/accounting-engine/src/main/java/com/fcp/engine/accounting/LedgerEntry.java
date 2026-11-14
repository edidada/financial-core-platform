package com.fcp.engine.accounting;

import com.fcp.common.domain.Direction;

import java.sql.Timestamp;

/** 已生效分录：只追加，禁止修改或删除 (I-5)。 */
public record LedgerEntry(
        long entryId,
        long journalId,
        long accountId,
        Direction direction,
        long amountMinor,
        long balanceBefore,
        long balanceAfter,
        Timestamp createdAt) {}