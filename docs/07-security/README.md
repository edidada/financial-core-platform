# 07 安全

- `../security-design.md`：认证、签名、密钥管理、审计、权限。
- `../../gateway/authentication`：HMAC-SHA256 请求签名、常量时间比较、时间戳窗口 + nonce 防重放。
- `../../deploy/kubernetes/secret.yaml`：仅占位符模板；真实凭据由 CI/Sealed Secrets 注入。
- `../../.github/workflows/security-scan.yml`：依赖漏洞扫描 + gitleaks + 明文口令检查。
- `../../tests/security/`：安全冒烟用例。