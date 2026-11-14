# database/schemas

Flyway 实际执行的 DDL 位于 `app/financial-core-app/src/main/resources/db/migration/{mysql,postgresql}/`，
按厂商拆分。`V1__init.sql` 建核心账务表，`V2__distributed_services.sql` 建阶段二~五表。

本目录存放**评审用**的完整逻辑模型与 ER 说明，见 `docs/database-design.md` 与 `docs/03-database-design/`。
不要在应用启动时重复执行本目录脚本；它们仅用于 DBA 评审与执行计划分析。