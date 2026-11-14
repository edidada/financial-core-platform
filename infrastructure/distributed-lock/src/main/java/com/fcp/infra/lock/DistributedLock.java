package com.fcp.infra.lock;

import java.time.Duration;

/** 基于关系数据库唯一约束的租约锁，MySQL / PostgreSQL 通用（阶段二不引入 Redis 依赖）。 */
public interface DistributedLock {
    boolean tryLock(String lockKey, String owner, Duration lease);

    boolean renew(String lockKey, String owner, Duration lease);

    void unlock(String lockKey, String owner);
}