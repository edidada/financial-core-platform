package com.fcp.service.transaction;

import com.fcp.common.api.ReverseRequest;
import com.fcp.common.api.TransactionResponse;
import com.fcp.common.api.TransferRequest;
import com.fcp.common.domain.Currency;
import com.fcp.common.domain.Direction;
import com.fcp.common.domain.JournalType;
import com.fcp.common.domain.TransactionStatus;
import com.fcp.common.domain.TransactionType;
import com.fcp.common.exception.DuplicateTransactionException;
import com.fcp.common.exception.IdempotencyConflictException;
import com.fcp.common.exception.ResourceNotFoundException;
import com.fcp.common.utils.Hashes;
import com.fcp.common.utils.IdGenerator;
import com.fcp.engine.accounting.EntryLine;
import com.fcp.engine.accounting.Journal;
import com.fcp.engine.accounting.PostingCommand;
import com.fcp.engine.posting.PostingEngine;
import com.fcp.infra.idempotency.IdempotencyRecord;
import com.fcp.infra.idempotency.IdempotencyRepository;
import com.fcp.infra.outbox.OutboxEvent;
import com.fcp.infra.outbox.OutboxRepository;
import com.fcp.service.account.Account;
import com.fcp.service.account.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 交易用例编排：一个方法 = 一个本地事务（ADR-0003）。 */
@Service
public class TransferService {
    private final TransactionRepository transactions;
    private final IdempotencyRepository idempotency;
    private final OutboxRepository outbox;
    private final PostingEngine postingEngine;
    private final AccountRepository accounts;
    private final IdGenerator ids;

    public TransferService(TransactionRepository transactions, IdempotencyRepository idempotency,
                           OutboxRepository outbox, PostingEngine postingEngine,
                           AccountRepository accounts, IdGenerator ids) {
        this.transactions = transactions;
        this.idempotency = idempotency;
        this.outbox = outbox;
        this.postingEngine = postingEngine;
        this.accounts = accounts;
        this.ids = ids;
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest req) {
        validateTransfer(req);
        String hash = Hashes.sha256(req.canonical());
        Timestamp now = Timestamp.from(Instant.now());

        Optional<IdempotencyRecord> existing = idempotency.find(req.tenantId(), req.idempotencyKey());
        if (existing.isPresent()) {
            return replay(existing.get(), hash);
        }

        long txId = ids.nextId();
        IdempotencyRecord rec = new IdempotencyRecord(
                req.idempotencyKey(), req.tenantId(), txId, hash, null, null, now);
        if (!idempotency.insertIfAbsent(rec)) {
            return awaitReplay(req.tenantId(), req.idempotencyKey(), hash);
        }

        Account payer = accounts.findAccount(req.payerAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Payer account", req.payerAccountId()));
        Account payee = accounts.findAccount(req.payeeAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Payee account", req.payeeAccountId()));
        Currency cur = Currency.of(req.currency());
        if (payer.currency() != cur || payee.currency() != cur) {
            throw new IllegalArgumentException("Account currency does not match request currency");
        }

        Transaction tx = new Transaction(txId, req.tenantId(), req.idempotencyKey(), hash,
                TransactionType.TRANSFER, TransactionStatus.CREATED,
                req.payerAccountId(), req.payeeAccountId(), req.amountMinor(), cur,
                null, 0L, null, now, now);
        long version = persistCreatedAndAdvance(tx, now);

        PostingCommand cmd = new PostingCommand(String.valueOf(txId), JournalType.TRANSFER, cur,
                payer.ledgerSet(), null,
                List.of(new EntryLine(req.payerAccountId(), Direction.DEBIT, req.amountMinor()),
                        new EntryLine(req.payeeAccountId(), Direction.CREDIT, req.amountMinor())));
        Journal journal = postingEngine.post(cmd);

        TransactionStateMachine.assertTransition(TransactionStatus.PROCESSING, TransactionStatus.SUCCEEDED);
        if (transactions.markSucceeded(txId, journal.journalId(), version, now) != 1) {
            throw new IllegalStateException("Concurrent state change on transaction " + txId);
        }

        outbox.insert(new OutboxEvent(ids.nextId(), txId, "TRANSACTION_SUCCEEDED",
                "{\"transactionId\":" + txId + "}", false, now));

        TransactionResponse resp = new TransactionResponse(txId, TransactionStatus.SUCCEEDED.name(),
                req.idempotencyKey(), req.amountMinor(), cur.code(), journal.journalId(), hash);
        idempotency.complete(req.tenantId(), req.idempotencyKey(), 200, resp.toString());
        return resp;
    }

    @Transactional
    public TransactionResponse reverse(ReverseRequest req) {
        Objects.requireNonNull(req.tenantId(), "tenantId");
        if (req.idempotencyKey() == null || req.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey required");
        }
        Objects.requireNonNull(req.originalTransactionId(), "originalTransactionId");
        String hash = Hashes.sha256(req.canonical());
        Timestamp now = Timestamp.from(Instant.now());

        Optional<IdempotencyRecord> existing = idempotency.find(req.tenantId(), req.idempotencyKey());
        if (existing.isPresent()) {
            return replay(existing.get(), hash);
        }
        long txId = ids.nextId();
        IdempotencyRecord rec = new IdempotencyRecord(
                req.idempotencyKey(), req.tenantId(), txId, hash, null, null, now);
        if (!idempotency.insertIfAbsent(rec)) {
            return awaitReplay(req.tenantId(), req.idempotencyKey(), hash);
        }

        Transaction original = transactions.findById(req.originalTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Original transaction", req.originalTransactionId()));
        TransactionStateMachine.assertTransition(original.status(), TransactionStatus.REVERSED);
        if (original.journalId() == null) {
            throw new IllegalStateException("Original transaction has no journal: " + original.transactionId());
        }

        Transaction reversal = new Transaction(txId, req.tenantId(), req.idempotencyKey(), hash,
                TransactionType.REVERSAL, TransactionStatus.CREATED,
                original.payeeAccountId(), original.payerAccountId(), original.amountMinor(),
                original.currency(), null, 0L, null, now, now);
        long version = persistCreatedAndAdvance(reversal, now);

        String reason = req.reason() == null ? "" : req.reason();
        Journal revJournal = postingEngine.reverse(original.journalId(), String.valueOf(txId), reason);

        if (transactions.markReversed(original.transactionId(), original.version(), now) != 1) {
            throw new IllegalStateException("Original transaction already changed: " + original.transactionId());
        }
        TransactionStateMachine.assertTransition(TransactionStatus.PROCESSING, TransactionStatus.SUCCEEDED);
        if (transactions.markSucceeded(txId, revJournal.journalId(), version, now) != 1) {
            throw new IllegalStateException("Concurrent state change on transaction " + txId);
        }

        outbox.insert(new OutboxEvent(ids.nextId(), txId, "TRANSACTION_REVERSED",
                "{\"transactionId\":" + txId + ",\"originalId\":" + original.transactionId() + "}",
                false, now));

        TransactionResponse resp = new TransactionResponse(txId, TransactionStatus.SUCCEEDED.name(),
                req.idempotencyKey(), original.amountMinor(), original.currency().code(),
                revJournal.journalId(), hash);
        idempotency.complete(req.tenantId(), req.idempotencyKey(), 200, resp.toString());
        return resp;
    }

    public static TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(tx.transactionId(), tx.status().name(), tx.idempotencyKey(),
                tx.amountMinor(), tx.currency().code(), tx.journalId(), tx.requestHash());
    }

    private long persistCreatedAndAdvance(Transaction tx, Timestamp now) {
        transactions.insert(tx);
        long v = 0L;
        TransactionStateMachine.assertTransition(TransactionStatus.CREATED, TransactionStatus.VALIDATED);
        if (transactions.casStatus(tx.transactionId(), TransactionStatus.CREATED,
                TransactionStatus.VALIDATED, v++, now) != 1) {
            throw new IllegalStateException("Concurrent state change on transaction " + tx.transactionId());
        }
        TransactionStateMachine.assertTransition(TransactionStatus.VALIDATED, TransactionStatus.PROCESSING);
        if (transactions.casStatus(tx.transactionId(), TransactionStatus.VALIDATED,
                TransactionStatus.PROCESSING, v++, now) != 1) {
            throw new IllegalStateException("Concurrent state change on transaction " + tx.transactionId());
        }
        return v;
    }

    private TransactionResponse replay(IdempotencyRecord r, String hash) {
        if (!hash.equals(r.requestHash())) {
            throw new IdempotencyConflictException(r.idempotencyKey());
        }
        Transaction tx = transactions.findById(r.transactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", r.transactionId()));
        return toResponse(tx);
    }

    private TransactionResponse awaitReplay(long tenantId, String key, String hash) {
        for (int i = 0; i < 50; i++) {
            Optional<IdempotencyRecord> r = idempotency.find(tenantId, key);
            if (r.isPresent() && r.get().completed()) {
                return replay(r.get(), hash);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new DuplicateTransactionException(key);
    }

    private void validateTransfer(TransferRequest req) {
        Objects.requireNonNull(req.tenantId(), "tenantId");
        if (req.idempotencyKey() == null || req.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey required");
        }
        Objects.requireNonNull(req.payerAccountId(), "payerAccountId");
        Objects.requireNonNull(req.payeeAccountId(), "payeeAccountId");
        if (req.payerAccountId().equals(req.payeeAccountId())) {
            throw new IllegalArgumentException("payer and payee must differ");
        }
        if (req.amountMinor() == null || req.amountMinor() <= 0) {
            throw new IllegalArgumentException("amountMinor must be positive");
        }
        Objects.requireNonNull(req.currency(), "currency");
    }
}