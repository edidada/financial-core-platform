# deploy/kubernetes

阶段四「Kubernetes 多节点部署」。`kubectl apply -f deploy/kubernetes/`。

| 文件 | 作用 |
| --- | --- |
| `deployment.yaml` | 3 副本滚动发布、优雅停机、就绪/存活探针 |
| `hpa.yaml` / `pdb.yaml` | 弹性扩容 + 至少 2 副本可用 |
| `networkpolicy.yaml` | 故障隔离、最小网络权限 |
| `secret.yaml` | **模板**（占位符），真实凭据由 CI/Sealed Secrets 注入 |

注意：`FCP_WORKER_ID` 用 Pod 名注入——多实例雪花 ID 的 workerId 必须唯一，
否则会产生重复主键（见 `infrastructure/id-generator`）。
