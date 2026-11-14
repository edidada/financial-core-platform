package com.fcp.infra.consumer;

import java.util.function.Consumer;

/** 去重 + 重试 + 死信的统一消费模板。 */
public final class IdempotentConsumer {
    private final MessageDeduplicator deduplicator;
    private final RetryPolicy retryPolicy;
    private final Consumer<String> deadLetter;

    public IdempotentConsumer(MessageDeduplicator deduplicator, RetryPolicy retryPolicy,
                              Consumer<String> deadLetter) {
        this.deduplicator = deduplicator;
        this.retryPolicy = retryPolicy;
        this.deadLetter = deadLetter;
    }

    public void accept(String messageKey, Runnable handler) {
        if (!deduplicator.tryClaim(messageKey)) return;   // 重复投递，直接跳过
        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            try {
                handler.run();
                return;
            } catch (RuntimeException e) {
                if (!retryPolicy.shouldRetry(attempt)) {
                    deadLetter.accept(messageKey);
                    return;
                }
                try {
                    Thread.sleep(retryPolicy.backoffFor(attempt).toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    deadLetter.accept(messageKey);
                    return;
                }
            }
        }
    }
}