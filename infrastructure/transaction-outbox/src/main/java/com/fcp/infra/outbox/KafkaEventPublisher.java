package com.fcp.infra.outbox;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

/** 同步发送并等待 ack，保证「标记 published」之前消息已进入 broker（可靠事件传递）。 */
public final class KafkaEventPublisher implements EventPublisher, AutoCloseable {
    private final KafkaProducer<String, String> producer;

    public KafkaEventPublisher(String bootstrapServers, String clientId) {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        p.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        this.producer = new KafkaProducer<>(p);
    }

    @Override
    public void publish(String topic, String key, String payload) {
        try {
            producer.send(new ProducerRecord<>(topic, key, payload)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing event", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to publish event " + key, e.getCause());
        }
    }

    @Override public void close() { producer.close(); }
}