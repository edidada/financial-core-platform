package com.fcp.infra.consumer;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

/** 消费侧去重：以 message_key 唯一约束挡住 Kafka 至少一次投递带来的重复。 */
@Repository
public class MessageDeduplicator {
    private final JdbcTemplate jdbc;

    public MessageDeduplicator(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** @return true 表示首次消费，false 表示重复消息应跳过 */
    public boolean tryClaim(String messageKey) {
        try {
            return jdbc.update("INSERT INTO consumed_message (message_key, consumed_at) VALUES (?, ?)",
                    messageKey, Timestamp.from(Instant.now())) == 1;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public long consumedCount() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM consumed_message", Long.class);
        return n == null ? 0L : n;
    }
}