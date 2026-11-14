package com.fcp.engine.accounting;

import com.fcp.common.domain.Currency;
import com.fcp.common.domain.Direction;
import com.fcp.common.domain.JournalType;
import com.fcp.common.exception.UnbalancedJournalException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JournalValidatorTest {

    @Test
    void balancedTransferPasses() {
        PostingCommand cmd = new PostingCommand("TX-1", JournalType.TRANSFER, Currency.CNY, "DEFAULT", null,
                List.of(new EntryLine(1L, Direction.DEBIT, 10000L),
                        new EntryLine(2L, Direction.CREDIT, 10000L)));
        assertDoesNotThrow(() -> JournalValidator.validate(cmd));
    }

    @Test
    void unbalancedJournalRejected() {
        PostingCommand cmd = new PostingCommand("TX-2", JournalType.TRANSFER, Currency.CNY, "DEFAULT", null,
                List.of(new EntryLine(1L, Direction.DEBIT, 10000L),
                        new EntryLine(2L, Direction.CREDIT, 9000L)));
        assertThrows(UnbalancedJournalException.class, () -> JournalValidator.validate(cmd));
    }

    @Test
    void singleEntryRejected() {
        PostingCommand cmd = new PostingCommand("TX-3", JournalType.TRANSFER, Currency.CNY, "DEFAULT", null,
                List.of(new EntryLine(1L, Direction.CREDIT, 10000L)));
        assertThrows(UnbalancedJournalException.class, () -> JournalValidator.validate(cmd));
    }

    @Test
    void overflowIsRejected() {
        PostingCommand cmd = new PostingCommand("TX-4", JournalType.TRANSFER, Currency.CNY, "DEFAULT", null,
                List.of(new EntryLine(1L, Direction.DEBIT, Long.MAX_VALUE),
                        new EntryLine(1L, Direction.DEBIT, 1L),
                        new EntryLine(2L, Direction.CREDIT, Long.MAX_VALUE),
                        new EntryLine(2L, Direction.CREDIT, 1L)));
        assertThrows(ArithmeticException.class, () -> JournalValidator.validate(cmd));
    }
}