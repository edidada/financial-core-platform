package com.fcp.engine.accounting;

import com.fcp.common.domain.Currency;
import com.fcp.common.domain.JournalType;

import java.util.List;

/** 入账命令：bizId 全局唯一，保证同一业务交易不重复入账 (I-3)。 */
public record PostingCommand(
        String bizId,
        JournalType type,
        Currency currency,
        String ledgerSet,
        Long reversalOf,
        List<EntryLine> lines) {

    public PostingCommand {
        lines = List.copyOf(lines);
    }
}