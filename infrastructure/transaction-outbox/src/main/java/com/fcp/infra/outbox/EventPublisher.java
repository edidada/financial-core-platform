package com.fcp.infra.outbox;

/** 事件投递端口：阶段二默认 Kafka，测试可用内存实现。 */
public interface EventPublisher {
    void publish(String topic, String key, String payload);
}