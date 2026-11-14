package com.fcp.app;

import com.fcp.common.utils.IdGenerator;
import com.fcp.gateway.api.GatewayFilterChain;
import com.fcp.gateway.auth.ApiKeyAuthenticator;
import com.fcp.infra.audit.AuditLogger;
import com.fcp.infra.consumer.IdempotentConsumer;
import com.fcp.infra.consumer.MessageDeduplicator;
import com.fcp.infra.consumer.RetryPolicy;
import com.fcp.infra.observability.BusinessMetrics;
import com.fcp.infra.outbox.EventPublisher;
import com.fcp.infra.outbox.InMemoryEventPublisher;
import com.fcp.infra.outbox.KafkaEventPublisher;
import com.fcp.infra.ratelimit.RateLimiter;
import com.fcp.infra.ratelimit.TokenBucketRateLimiter;
import com.fcp.infra.sharding.ShardRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** 阶段二~五组件的显式装配；Kafka 未配置时默认内存投递，保证开箱可跑。 */
@Configuration
public class PlatformConfiguration {

    @Bean
    public InMemoryEventPublisher inMemoryEventPublisher() {
        return new InMemoryEventPublisher();
    }

    @Bean
    @Primary
    public EventPublisher eventPublisher(Environment env, InMemoryEventPublisher inMemory) {
        String bootstrap = env.getProperty("fcp.kafka.bootstrap", "");
        if (bootstrap.isBlank()) return inMemory;
        return new KafkaEventPublisher(bootstrap, "fcp-outbox-relay");
    }

    @Bean
    public RateLimiter rateLimiter(Environment env) {
        long cap = env.getProperty("fcp.ratelimit.capacity", Long.class, 200L);
        return new TokenBucketRateLimiter(cap, cap, Duration.ofSeconds(1));
    }

    @Bean
    public ApiKeyAuthenticator apiKeyAuthenticator(Environment env) {
        String keys = env.getProperty("fcp.gateway.api-keys", "dev-key");
        Set<String> allowed = Arrays.stream(keys.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        return new ApiKeyAuthenticator(allowed,
                env.getProperty("fcp.gateway.secret", "dev-secret"), Duration.ofMinutes(5));
    }

    @Bean
    public GatewayFilterChain gatewayFilterChain(RateLimiter rateLimiter,
                                                 ApiKeyAuthenticator authenticator,
                                                 AuditLogger auditLogger,
                                                 IdGenerator ids) {
        return new GatewayFilterChain(rateLimiter, authenticator, auditLogger, ids);
    }

    @Bean
    public ShardRouter shardRouter(Environment env) {
        return new ShardRouter(env.getProperty("fcp.sharding.count", Integer.class, 1));
    }

    @Bean
    public BusinessMetrics businessMetrics() {
        return new BusinessMetrics();
    }

    @Bean
    public RetryPolicy retryPolicy() {
        return RetryPolicy.defaults();
    }

    @Bean
    public IdempotentConsumer idempotentConsumer(MessageDeduplicator dedup, RetryPolicy policy,
                                                 BusinessMetrics metrics) {
        return new IdempotentConsumer(dedup, policy, key -> metrics.increment("deadLetter"));
    }
}