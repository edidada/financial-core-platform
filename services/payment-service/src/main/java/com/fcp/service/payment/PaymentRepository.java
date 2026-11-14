package com.fcp.service.payment;

import com.fcp.common.domain.Currency;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class PaymentRepository {
    private static final RowMapper<PaymentInstruction> MAPPER = (rs, i) -> new PaymentInstruction(
            rs.getLong("payment_id"), rs.getLong("transaction_id"), rs.getLong("payer_account_id"),
            rs.getLong("payee_account_id"), rs.getLong("amount_minor"),
            Currency.of(rs.getString("currency")), rs.getString("channel"),
            rs.getString("external_ref"), PaymentStatus.valueOf(rs.getString("status")),
            rs.getLong("version"));

    private final JdbcTemplate jdbc;
    private final com.fcp.common.utils.IdGenerator ids;

    public PaymentRepository(JdbcTemplate jdbc, com.fcp.common.utils.IdGenerator ids) {
        this.jdbc = jdbc; this.ids = ids;
    }

    public long create(long transactionId, long payer, long payee, long amountMinor,
                       Currency currency, String channel, String externalRef) {
        long id = ids.nextId();
        jdbc.update("INSERT INTO payment_instruction (payment_id, transaction_id, payer_account_id, "
                        + "payee_account_id, amount_minor, currency, channel, external_ref, status, version, created_at, updated_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                id, transactionId, payer, payee, amountMinor, currency.code(), channel, externalRef,
                PaymentStatus.CREATED.name(), 0L, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
        return id;
    }

    public Optional<PaymentInstruction> findById(long id) {
        return jdbc.query("SELECT * FROM payment_instruction WHERE payment_id = ?", MAPPER, id).stream().findFirst();
    }

    /** CAS 状态迁移，影响行数必须为 1 */
    public int casStatus(long id, PaymentStatus expected, PaymentStatus target, long version) {
        return jdbc.update("UPDATE payment_instruction SET status = ?, version = version + 1, updated_at = ? "
                        + "WHERE payment_id = ? AND status = ? AND version = ?",
                target.name(), Timestamp.from(Instant.now()), id, expected.name(), version);
    }
}