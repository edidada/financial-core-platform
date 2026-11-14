package com.fcp.engine.accounting;

import com.fcp.common.domain.Direction;
import com.fcp.common.exception.UnbalancedJournalException;

import java.util.List;

/** 复式记账硬约束：ΣDebit = ΣCredit，且同币种、同账套（由命令单一币种保证）。 */
public final class JournalValidator {
    private JournalValidator() {}

    public static void validate(PostingCommand cmd) {
        List<EntryLine> lines = cmd.lines();
        if (lines == null || lines.size() < 2) {
            throw new UnbalancedJournalException("a journal needs at least 2 entries");
        }
        long debit = 0L;
        long credit = 0L;
        for (EntryLine line : lines) {
            if (line.direction() == null) {
                throw new UnbalancedJournalException("direction is null");
            }
            if (line.amountMinor() <= 0) {
                throw new UnbalancedJournalException("amount must be positive: " + line.amountMinor());
            }
            if (line.direction() == Direction.DEBIT) {
                debit = Math.addExact(debit, line.amountMinor());
            } else {
                credit = Math.addExact(credit, line.amountMinor());
            }
        }
        if (debit == 0 || debit != credit) {
            throw new UnbalancedJournalException("debit=" + debit + " credit=" + credit);
        }
    }
}