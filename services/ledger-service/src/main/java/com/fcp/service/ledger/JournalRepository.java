package com.fcp.service.ledger;

import com.fcp.common.domain.Currency;
import com.fcp.common.domain.Direction;
import com.fcp.common.domain.JournalType;
import com.fcp.engine.accounting.Journal;
import com.fcp.engine.accounting.LedgerEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JournalRepository {
    private static final String JOURNAL_COLUMNS =
            "journal_id, biz_id, type, currency, ledger_set, reversal_of, reversed_by, posted_at";
    private static final String ENTRY_COLUMNS =
            "entry_id, journal_id, account_id, direction, amount_minor, balance_before, balance_after, created_at";

    private static final RowMapper<Journal> JOURNAL_MAPPER = (rs, i) -> new Journal(
            rs.getLong("journal_id"), rs.getString("biz_id"), JournalType.valueOf(rs.getString("type")),
            Currency.of(rs.getString("currency")), rs.getString("ledger_set"),
            (Long) rs.getObject("reversal_of"), (Long) rs.getObject("reversed_by"),
            rs.getTimestamp("posted_at"), List.of());

    private static final RowMapper<LedgerEntry> ENTRY_MAPPER = (rs, i) -> new LedgerEntry(
            rs.getLong("entry_id"), rs.getLong("journal_id"), rs.getLong("account_id"),
            Direction.valueOf(rs.getString("direction")), rs.getLong("amount_minor"),
            rs.getLong("balance_before"), rs.getLong("balance_after"), rs.getTimestamp("created_at"));

    private final JdbcTemplate jdbc;

    public JournalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 账务唯一写入口：只有本仓储能写 journal / ledger_entry。 */
    public void insertJournal(Journal j) {
        jdbc.update("INSERT INTO journal (" + JOURNAL_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?)",
                j.journalId(), j.bizId(), j.type().name(), j.currency().code(), j.ledgerSet(),
                j.reversalOf(), j.reversedBy(), j.postedAt());
    }

    public void insertEntries(List<LedgerEntry> entries) {
        for (LedgerEntry e : entries) {
            jdbc.update("INSERT INTO ledger_entry (" + ENTRY_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?)",
                    e.entryId(), e.journalId(), e.accountId(), e.direction().name(), e.amountMinor(),
                    e.balanceBefore(), e.balanceAfter(), e.createdAt());
        }
    }

    public void markReversed(long originalJournalId, long reversalJournalId) {
        jdbc.update("UPDATE journal SET reversed_by = ? WHERE journal_id = ?",
                reversalJournalId, originalJournalId);
    }

    public Optional<Journal> findById(long journalId) {
        List<Journal> found = jdbc.query("SELECT " + JOURNAL_COLUMNS + " FROM journal WHERE journal_id = ?",
                JOURNAL_MAPPER, journalId);
        if (found.isEmpty()) return Optional.empty();
        Journal j = found.get(0);
        return Optional.of(withEntries(j));
    }

    public Optional<Journal> findByBizId(String bizId) {
        List<Journal> found = jdbc.query("SELECT " + JOURNAL_COLUMNS + " FROM journal WHERE biz_id = ?",
                JOURNAL_MAPPER, bizId);
        if (found.isEmpty()) return Optional.empty();
        return Optional.of(withEntries(found.get(0)));
    }

    public long countReversedBy(long journalId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM journal WHERE reversed_by IS NOT NULL AND journal_id = ?",
                Long.class, journalId);
    }

    private Journal withEntries(Journal j) {
        List<LedgerEntry> entries = jdbc.query("SELECT " + ENTRY_COLUMNS
                + " FROM ledger_entry WHERE journal_id = ? ORDER BY entry_id", ENTRY_MAPPER, j.journalId());
        return new Journal(j.journalId(), j.bizId(), j.type(), j.currency(), j.ledgerSet(),
                j.reversalOf(), j.reversedBy(), j.postedAt(), entries);
    }
}