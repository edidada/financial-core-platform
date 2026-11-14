package com.fcp.app;

import com.fcp.common.utils.IdGenerator;
import com.fcp.common.utils.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {
    @Bean
    public IdGenerator idGenerator() {
        return new SnowflakeIdGenerator(1L);
    }
}