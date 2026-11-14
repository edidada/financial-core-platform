# 06 高可用

- `../disaster-recovery.md`：RTO/RPO、故障模型、恢复流程。
- `../../deploy/kubernetes,helm,terraform,ansible,monitoring`：多 AZ 部署、灰度回滚、SLO 告警。
- `../../chaos-test/`：7 类故障注入与恢复校验。

关键原则：不要求所有模块达到同样可用性等级；按业务关键性制定不同恢复目标
（Google SRE 错误预算 / AWS 金融行业架构）。账务库是唯一权威数据源，切换必须可验证。