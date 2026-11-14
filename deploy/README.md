# deploy

阶段四「高可用与生产工程」交付物。

| 目录 | 内容 | 对应能力 |
| --- | --- | --- |
| `docker/` | 多阶段 Dockerfile、非 root 运行、健康检查 | 工程交付 |
| `kubernetes/` | Deployment/Service/HPA/PDB/NetworkPolicy/ConfigMap/Secret 模板 | 多节点部署、故障隔离 |
| `helm/` | Chart + values + 模板（灰度/回滚参数化） | 发布工程 |
| `terraform/` | 命名空间 + 双库引擎变量 | IaC |
| `ansible/` | 30% 灰度、健康检查、不变量校验、发布审计、回滚 | 灰度/回滚/审计 |
| `monitoring/` | Prometheus 抓取 + SLO 告警 + Grafana 面板 | 监控告警、SLO |

**安全约定**：所有凭据通过环境变量 / Secret 注入，`secret.yaml` 只含占位符；
仓库内不含任何账号口令（含本机 MySQL 口令）。

**恢复目标**（README 第一节）：阶段四目标是同城 RTO < 5 min、RPO ≤ 30 s；
阶段一/二为 RTO < 30 min、RPO ≤ 5 min。不同业务关键性模块不应要求同样的可用性等级
（Google SRE / AWS 金融行业架构）。
