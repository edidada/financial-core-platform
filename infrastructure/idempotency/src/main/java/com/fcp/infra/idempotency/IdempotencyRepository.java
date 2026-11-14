package com.fcp.infra.idempotency;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class IdempotencyRepository {
    private static final String COLUMNS =
            "idempotency_key, tenant_id, transaction_id, request_hash, response_status, response_body, created_at";

    private static final RowMapper<IdempotencyRecord> MAPPER = (rs, i) -> new IdempotencyRecord(
            rs.getString("idempotency_key"),
            rs.getLong("tenant_id"),
            rs.getLong("transaction_id"),
            rs.getString("request_hash"),
            (Integer) rs.getObject("response_status"),
            rs.getString("response_body"),
            rs.getTimestamp("created_at"));

    private final JdbcTemplate jdbc;

    public IdempotencyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @return true 表示插入成功；false 表示唯一键冲突（并发重复请求）。 */
    public boolean insertIfAbsent(IdempotencyRecord r) {
        try {
            jdbc.update("INSERT INTO idempotency_record (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?)",
                    r.idempotencyKey(), r.tenantId(), r.transactionId(), r.requestHash(),
                    r.responseStatus(), r.responseBody(), r.createdAt());
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public Optional<IdempotencyRecord> find(long tenantId, String key) {
        return jdbc.query("SELECT " + COLUMNS
                        + " FROM idempotency_record WHERE tenant_id = ? AND idempotency_key = ?",
                MAPPER, tenantId, key).stream().findFirst();
    }

    public void complete(long tenantId, String key, int status, String body) {
        jdbc.update("UPDATE idempotency_record SET response_status = ?, response_body = ? "
                + "WHERE tenant_id = ? AND idempotency_key = ?", status, body, tenantId, key);
    }
}