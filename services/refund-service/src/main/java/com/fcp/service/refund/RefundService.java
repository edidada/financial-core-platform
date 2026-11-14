package com.fcp.service.refund;

import com.fcp.common.exception.IdempotencyConflictException;
import com.fcp.common.exception.InvalidStateTransitionException;
import com.fcp.common.domain.TransactionStatus;
import com.fcp.service.transaction.Transaction;
import com.fcp.service.transaction.TransactionRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

/** 退款/冲正：必须基于已入账交易，走反向分录，且自身幂等（ADR-0004）。 */
@Service
public class RefundService {
    private final JdbcTemplate jdbc;
    private final TransactionRepository transactions;
    private final com.fcp.common.utils.IdGenerator ids;

    public RefundService(JdbcTemplate jdbc, TransactionRepository transactions,
                         com.fcp.common.utils.IdGenerator ids) {
        this.jdbc = jdbc; this.transactions = transactions; this.ids = ids;
    }

    public long createRefundOrder(long originalTransactionId, String idempotencyKey, String currency) {
        Transaction original = transactions.findById(originalTransactionId)
                .orElseThrow(() -> new com.fcp.common.exception.ResourceNotFoundException("Transaction", originalTransactionId));
        if (original.status() != TransactionStatus.SUCCEEDED && original.status() != TransactionStatus.REVERSED) {
            throw new InvalidStateTransitionException(original.status(), TransactionStatus.REVERSED);
        }
        long id = ids.nextId();
        try {
            jdbc.update("INSERT INTO refund_order (refund_id, original_transaction_id, amount_minor, currency, "
                            + "status, idempotency_key, created_at) VALUES (?,?,?,?,?,?,?)",
                    id, originalTransactionId, original.amountMinor(), currency, "CREATED",
                    idempotencyKey, Timestamp.from(Instant.now()));
        } catch (DuplicateKeyException e) {
            throw new IdempotencyConflictException(idempotencyKey);
        }
        return id;
    }
}