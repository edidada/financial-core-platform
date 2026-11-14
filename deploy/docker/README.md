# deploy/docker

`docker build -f deploy/docker/Dockerfile -t fcp/app:1.0.0 .`

镜像以非 root（uid 10001）运行，含健康检查。配置全部走环境变量：
`FCP_DB_*`、`FCP_SERVER_PORT`、`FCP_KAFKA_BOOTSTRAP`、`FCP_WORKER_ID`（多实例唯一）、
`FCP_GATEWAY_SECRET`、`FCP_API_KEYS`。凭据不得写入镜像或仓库。
