# 08 测试

- `../test-strategy.md`：TC-1~TC-9 验收矩阵、CI 门禁、测试金字塔。
- `../../tests/`：8 类测试目录 + `platform-tests` Maven 模块（18 个纯逻辑单测已纳入 `mvn test`）。
- `../../.github/workflows/test.yml`：MySQL 8.4 / PostgreSQL 16 双库集成测试矩阵（TC-9）。

当前 `mvn test` 共 28 个用例通过（10 个阶段一 + 18 个阶段二~五）。
需要真实数据库/K8s 的用例由 CI 与 chaos-test 驱动，本地构建不依赖外部服务。