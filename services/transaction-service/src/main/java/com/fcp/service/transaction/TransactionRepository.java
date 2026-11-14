package com.fcp.service.transaction;

import com.fcp.common.domain.Currency;
import com.fcp.common.domain.TransactionStatus;
import com.fcp.common.domain.TransactionType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class TransactionRepository {
    private static final String COLUMNS = "transaction_id, tenant_id, idempotency_key, request_hash, type, "
            + "status, payer_account_id, payee_account_id, amount_minor, currency, journal_id, "
            + "version, next_retry_at, created_at, updated_at";

    private static final RowMapper<Transaction> MAPPER = (rs, i) -> new Transaction(
            rs.getLong("transaction_id"), rs.getLong("tenant_id"), rs.getString("idempotency_key"),
            rs.getString("request_hash"), TransactionType.valueOf(rs.getString("type")),
            TransactionStatus.valueOf(rs.getString("status")),
            (Long) rs.getObject("payer_account_id"), (Long) rs.getObject("payee_account_id"),
            rs.getLong("amount_minor"), Currency.of(rs.getString("currency")),
            (Long) rs.getObject("journal_id"), rs.getLong("version"),
            rs.getTimestamp("next_retry_at"), rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"));

    private final JdbcTemplate jdbc;

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Transaction t) {
        jdbc.update("INSERT INTO transaction (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                t.transactionId(), t.tenantId(), t.idempotencyKey(), t.requestHash(),
                t.type().name(), t.status().name(), t.payerAccountId(), t.payeeAccountId(),
                t.amountMinor(), t.currency().code(), t.journalId(), t.version(),
                t.nextRetryAt(), t.createdAt(), t.updatedAt());
    }

    public Optional<Transaction> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM transaction WHERE transaction_id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public Optional<Transaction> findByKey(long tenantId, String key) {
        return jdbc.query("SELECT " + COLUMNS
                        + " FROM transaction WHERE tenant_id = ? AND idempotency_key = ?", MAPPER, tenantId, key)
                .stream().findFirst();
    }

    /** CAS 状态迁移：影响行数必须为 1，否则视为并发冲突。 */
    public int casStatus(long id, TransactionStatus expected, TransactionStatus target,
                         long expectedVersion, Timestamp now) {
        return jdbc.update("UPDATE transaction SET status = ?, version = version + 1, updated_at = ? "
                        + "WHERE transaction_id = ? AND status = ? AND version = ?",
                target.name(), now, id, expected.name(), expectedVersion);
    }

    public int markSucceeded(long id, long journalId, long expectedVersion, Timestamp now) {
        return jdbc.update("UPDATE transaction SET status = 'SUCCEEDED', journal_id = ?, "
                        + "version = version + 1, updated_at = ? "
                        + "WHERE transaction_id = ? AND status = 'PROCESSING' AND version = ?",
                journalId, now, id, expectedVersion);
    }

    public int markReversed(long id, long expectedVersion, Timestamp now) {
        return jdbc.update("UPDATE transaction SET status = 'REVERSED', version = version + 1, "
                        + "updated_at = ? WHERE transaction_id = ? AND status = 'SUCCEEDED' AND version = ?",
                now, id, expectedVersion);
    }
}