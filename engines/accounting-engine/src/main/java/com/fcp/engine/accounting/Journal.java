package com.fcp.engine.accounting;

import com.fcp.common.domain.Currency;
import com.fcp.common.domain.JournalType;

import java.sql.Timestamp;
import java.util.List;

/** 凭证：一组借贷平衡的分录集合。 */
public record Journal(
        long journalId,
        String bizId,
        JournalType type,
        Currency currency,
        String ledgerSet,
        Long reversalOf,
        Long reversedBy,
        Timestamp postedAt,
        List<LedgerEntry> entries) {}