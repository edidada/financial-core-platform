package com.fcp.infra.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxRepository {
    private final JdbcTemplate jdbc;

    public OutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(OutboxEvent e) {
        jdbc.update("INSERT INTO outbox_event "
                        + "(event_id, aggregate_id, event_type, payload, published, created_at) "
                        + "VALUES (?,?,?,?,?,?)",
                e.eventId(), e.aggregateId(), e.eventType(), e.payload(), e.published(), e.createdAt());
    }
}