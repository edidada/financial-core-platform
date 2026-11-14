package com.fcp.engine.accounting;

import com.fcp.common.domain.Direction;

/** 一条待入账分录（尚不含 journalId 与余额快照）。 */
public record EntryLine(long accountId, Direction direction, long amountMinor) {}