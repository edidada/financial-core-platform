# 集成测试

- 由 `.github/workflows/test.yml` 的 `dual-database-integration` 矩阵驱动：
  MySQL 8.4 与 PostgreSQL 16 分别起空库，跑全量 Flyway V1/V2 迁移。
- 验收：应用启动成功；`/v1/invariants` 返回 `healthy=true`；两套库结果一致（TC-9）。
- 本地等价命令见 `docs/README.md` 第六节（切换 `--spring.profiles.active`）。