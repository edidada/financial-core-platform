package com.fcp.infra.outbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 测试 / 无 Kafka 环境用：记录已投递事件，便于断言。 */
public final class InMemoryEventPublisher implements EventPublisher {
    public record Published(String topic, String key, String payload) {}
    private final List<Published> events = Collections.synchronizedList(new ArrayList<>());

    @Override public void publish(String topic, String key, String payload) {
        events.add(new Published(topic, key, payload));
    }
    public List<Published> events() { return List.copyOf(events); }
}