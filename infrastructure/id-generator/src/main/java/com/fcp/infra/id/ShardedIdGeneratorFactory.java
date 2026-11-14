package com.fcp.infra.id;

import com.fcp.common.utils.IdGenerator;
import com.fcp.common.utils.SnowflakeIdGenerator;

/** 集中创建 IdGenerator，避免各模块各自 new 出相同 workerId 导致主键冲突。 */
public final class ShardedIdGeneratorFactory {
    private ShardedIdGeneratorFactory() {}

    public static IdGenerator create(WorkerIdProvider provider) {
        return new SnowflakeIdGenerator(provider.workerId());
    }
}