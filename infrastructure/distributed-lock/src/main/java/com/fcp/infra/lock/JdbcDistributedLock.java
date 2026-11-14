package com.fcp.infra.lock;

import com.fcp.common.utils.IdGenerator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

@Repository
public class JdbcDistributedLock implements DistributedLock {
    private final JdbcTemplate jdbc;
    private final IdGenerator ids;

    public JdbcDistributedLock(JdbcTemplate jdbc, IdGenerator ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    @Override
    public boolean tryLock(String lockKey, String owner, Duration lease) {
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp expires = Timestamp.from(Instant.now().plus(lease));
        // 先清理已过期租约；两库均可移植
        jdbc.update("DELETE FROM distributed_lock WHERE lock_key = ? AND expires_at <= ?", lockKey, now);
        try {
            return jdbc.update("INSERT INTO distributed_lock "
                            + "(lock_key, owner, expires_at, acquired_at) VALUES (?,?,?,?)",
                    lockKey, owner, expires, now) == 1;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public boolean renew(String lockKey, String owner, Duration lease) {
        Timestamp expires = Timestamp.from(Instant.now().plus(lease));
        return jdbc.update("UPDATE distributed_lock SET expires_at = ? "
                        + "WHERE lock_key = ? AND owner = ? AND expires_at > ?",
                expires, lockKey, owner, Timestamp.from(Instant.now())) == 1;
    }

    @Override
    public void unlock(String lockKey, String owner) {
        jdbc.update("DELETE FROM distributed_lock WHERE lock_key = ? AND owner = ?", lockKey, owner);
    }
}