package com.fcp.infra.outbox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Outbox 投递器：扫描未发布事件 -> 投递 -> 标记 published，全部在同一数据源上。
 * 不承诺「恰好一次」投递；消费侧靠 infra-message-consumer 去重实现最终一致（README 6.1）。
 */
@Service
public class OutboxRelay {
    private static final String DEFAULT_TOPIC = "fcp.domain-events";

    private final JdbcTemplate jdbc;
    private final EventPublisher publisher;
    private final String topic;

    @Autowired
    public OutboxRelay(JdbcTemplate jdbc, EventPublisher publisher) {
        this(jdbc, publisher, DEFAULT_TOPIC);
    }

    public OutboxRelay(JdbcTemplate jdbc, EventPublisher publisher, String topic) {
        this.jdbc = jdbc;
        this.publisher = publisher;
        this.topic = topic;
    }

    /** @return 本次成功投递并标记的事件数 */
    public int relayOnce(int batchSize) {
        List<Pending> pending = jdbc.query(
                "SELECT event_id, aggregate_id, event_type, payload FROM outbox_event "
                        + "WHERE published = FALSE ORDER BY event_id LIMIT ?",
                (rs, i) -> new Pending(rs.getLong("event_id"), rs.getLong("aggregate_id"),
                        rs.getString("event_type"), rs.getString("payload")),
                batchSize);
        int n = 0;
        for (Pending e : pending) {
            publisher.publish(topic, String.valueOf(e.aggregateId()), e.payload());
            int updated = jdbc.update("UPDATE outbox_event SET published = TRUE WHERE event_id = ? AND published = FALSE",
                    e.eventId());
            if (updated == 1) n++;
        }
        return n;
    }

    private record Pending(long eventId, long aggregateId, String eventType, String payload) {}
}