# 安全设计（阶段四）

> 对应 `README.txt` 第五节「工程交付能力」、`07-security/`、`.github/workflows/security-scan.yml`。

## 一、认证与请求完整性

`gateway/authentication` 提供三合一认证：

| 机制 | 实现 | 防护目标 |
| --- | --- | --- |
| API Key | `ApiKeyAuthenticator.allowedKeys` | 调用方身份 |
| 请求签名 | HMAC-SHA256 over `method|path|timestamp|nonce|body` | 篡改、中间人 |
| 防重放 | 时间戳窗口（5 min）+ nonce 去重 | 重放攻击 |

- 签名比较使用 `MessageDigest.isEqual`（常量时间），避免时序侧信道。
- **幂等键与防重放是两层**：`Idempotency-Key` 防重复业务生效，nonce/签名防请求被重放，
  二者不可互相替代（ADR-0005）。

## 二、密钥管理

- 所有密钥、数据库口令通过环境变量 / Kubernetes Secret 注入。
- `deploy/kubernetes/secret.yaml` 只含 `REPLACE_ME` 占位符；仓库内不含任何真实凭据。
- `.github/workflows/security-scan.yml` 执行明文口令检查；`tests/security/security-tests.sh` 本地复核。
- 生产应接入 KMS / Sealed Secrets 并定期轮换；轮换期间双密钥并存平滑过渡。

## 三、数据与权限

- 金额与账务数据不写入日志；审计日志 `audit_log` 只记录操作者、动作、资源、TraceId、摘要。
- 账务表 `ledger_entry` 只追加，无 UPDATE / DELETE 授权（I-5）。
- 容器以非 root（uid 10001）运行；`NetworkPolicy` 限制最小出入站。
- 越权防护：账户查询按 `tenant_id` 隔离（阶段二扩展点）。

## 四、审计与可追溯

- `infrastructure/audit-log`：只追加审计记录，与业务写入同事务边界。
- 全局 `TraceId` 由网关生成、全链路透传（`infrastructure/observability/TraceContext`）。
- 每次发布写入 `/var/log/fcp/deploy-audit.log`（`deploy/ansible/playbook.yml`），含版本、主机、结果。
- 冲正通过反向分录实现，原凭证保留（ADR-0004），满足监管审计与事后追溯。

## 五、CI 安全门禁

| 检查 | 工作流 |
| --- | --- |
| OWASP Dependency-Check | `security-scan.yml` |
| gitleaks 密钥泄露扫描 | `security-scan.yml` |
| 明文口令检查 | `security-scan.yml` + `tests/security/` |
| 双库集成 + 不变量门禁 | `test.yml` |
| 发布前 `mvn verify` | `release.yml` |

## 六、范围外（阶段五专题）

mTLS、硬件安全模块（HSM）、字段级加密、密钥托管服务集成、渗透测试报告与合规认证材料。
