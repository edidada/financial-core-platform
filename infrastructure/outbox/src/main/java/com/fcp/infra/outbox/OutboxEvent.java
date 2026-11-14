package com.fcp.infra.outbox;

import java.sql.Timestamp;

/** 事务消息：与账务写入同一本地事务落库，阶段二再异步投递（ADR-0003）。 */
public record OutboxEvent(
        long eventId,
        long aggregateId,
        String eventType,
        String payload,
        boolean published,
        Timestamp createdAt) {}