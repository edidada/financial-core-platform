# 09 运维

- `../../deploy/`：Docker / K8s / Helm / Terraform / Ansible / 监控。
- `../../deploy/monitoring/alerts.yml`：SLO 告警（P99、不变量违规、Outbox 积压、DB 不可用）。
- `ops-runbook.md`（本目录）：常见故障处置。

## 运行手册（摘要）

| 现象 | 处置 |
| --- | --- |
| `/v1/invariants` 不为 healthy | **立即停止发布**，离线重算余额与分录，定位差异凭证 |
| Outbox 积压 | 检查 Kafka Broker 与 `OutboxRelay`；恢复后重投，消费侧去重保证不重复入账 |
| 网关 429 | 检查限流容量；确认非攻击性突发流量后再扩容 |
| 数据库主库不可用 | 触发主从切换；切换后重放在途交易并做多方对账 |
| 发布异常 | 按 `deploy/ansible/playbook.yml` 回滚到上一稳定版本，查发布审计日志 |