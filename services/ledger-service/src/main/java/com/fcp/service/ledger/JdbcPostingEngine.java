package com.fcp.service.ledger;

import com.fcp.common.domain.Direction;
import com.fcp.common.domain.JournalType;
import com.fcp.common.exception.DuplicateTransactionException;
import com.fcp.common.exception.InsufficientBalanceException;
import com.fcp.common.exception.ResourceNotFoundException;
import com.fcp.common.utils.IdGenerator;
import com.fcp.engine.accounting.EntryLine;
import com.fcp.engine.accounting.Journal;
import com.fcp.engine.accounting.JournalValidator;
import com.fcp.engine.accounting.LedgerEntry;
import com.fcp.engine.accounting.PostingCommand;
import com.fcp.engine.posting.PostingEngine;
import com.fcp.service.account.AccountBalance;
import com.fcp.service.account.AccountRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 入账引擎实现：校验平衡、加锁、写分录、更新余额，全部在调用方本地事务内 (ADR-0003)。 */
@Component
public class JdbcPostingEngine implements PostingEngine {
    private final JournalRepository journals;
    private final AccountRepository accounts;
    private final IdGenerator ids;

    public JdbcPostingEngine(JournalRepository journals, AccountRepository accounts, IdGenerator ids) {
        this.journals = journals;
        this.accounts = accounts;
        this.ids = ids;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Journal post(PostingCommand cmd) {
        JournalValidator.validate(cmd);

        List<Long> accountIds = cmd.lines().stream().map(EntryLine::accountId).distinct().sorted().toList();
        Map<Long, AccountBalance> locked = new LinkedHashMap<>();
        for (AccountBalance b : accounts.lockForUpdate(accountIds)) {
            locked.put(b.accountId(), b);
        }
        for (Long id : accountIds) {
            if (!locked.containsKey(id)) throw new ResourceNotFoundException("Account balance", id);
        }

        Timestamp now = Timestamp.from(Instant.now());
        long journalId = ids.nextId();
        Map<Long, Long> running = new HashMap<>();
        for (Long id : accountIds) running.put(id, locked.get(id).availableBalance());

        List<EntryLine> lines = new ArrayList<>(cmd.lines());
        lines.sort(Comparator.comparingLong(EntryLine::accountId));
        List<LedgerEntry> entries = new ArrayList<>();
        for (EntryLine line : lines) {
            long before = running.get(line.accountId());
            long after = line.direction() == Direction.DEBIT
                    ? Math.subtractExact(before, line.amountMinor())
                    : Math.addExact(before, line.amountMinor());
            if (after < 0) throw new InsufficientBalanceException(line.accountId());
            running.put(line.accountId(), after);
            entries.add(new LedgerEntry(ids.nextId(), journalId, line.accountId(), line.direction(),
                    line.amountMinor(), before, after, now));
        }

        Journal journal = new Journal(journalId, cmd.bizId(), cmd.type(), cmd.currency(),
                cmd.ledgerSet(), cmd.reversalOf(), null, now, entries);
        try {
            journals.insertJournal(journal);
        } catch (DuplicateKeyException e) {
            throw new DuplicateTransactionException(cmd.bizId());
        }
        journals.insertEntries(entries);

        // 余额更新：扣款用条件更新再次兜底，防止加锁与更新之间被其他事务改动
        for (Long id : accountIds) {
            long delta = running.get(id) - locked.get(id).availableBalance();
            if (delta < 0) {
                if (accounts.conditionalDebit(id, -delta, now) != 1) {
                    throw new InsufficientBalanceException(id);
                }
            } else if (delta > 0) {
                if (accounts.credit(id, delta, now) != 1) {
                    throw new ResourceNotFoundException("Account balance", id);
                }
            }
        }
        return journal;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Journal reverse(long originalJournalId, String newBizId, String reason) {
        Objects.requireNonNull(reason, "reason");
        Journal original = journals.findById(originalJournalId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal", originalJournalId));
        if (original.reversedBy() != null) {
            throw new DuplicateTransactionException("journal already reversed: " + originalJournalId);
        }
        List<EntryLine> reversed = original.entries().stream()
                .map(e -> new EntryLine(e.accountId(),
                        e.direction() == Direction.DEBIT ? Direction.CREDIT : Direction.DEBIT,
                        e.amountMinor()))
                .toList();
        PostingCommand cmd = new PostingCommand(newBizId, JournalType.REVERSAL, original.currency(),
                original.ledgerSet(), originalJournalId, reversed);
        Journal reversal = post(cmd);
        journals.markReversed(originalJournalId, reversal.journalId());
        return reversal;
    }
}