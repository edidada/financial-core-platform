package com.fcp.infra.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

/** 只追加审计日志；与业务写入同一事务边界由调用方保证。 */
@Service
public class AuditLogger {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final com.fcp.common.utils.IdGenerator ids;

    public AuditLogger(JdbcTemplate jdbc, ObjectMapper mapper, com.fcp.common.utils.IdGenerator ids) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.ids = ids;
    }

    public void record(AuditEntry e) {
        String detail = e.detail();
        jdbc.update("INSERT INTO audit_log "
                        + "(audit_id, actor, action, resource_type, resource_id, trace_id, detail, happened_at) "
                        + "VALUES (?,?,?,?,?,?,?,?)",
                ids.nextId(), e.actor(), e.action(), e.resourceType(), e.resourceId(),
                e.traceId(), detail, new Timestamp(e.happenedAtMillis()));
    }
}