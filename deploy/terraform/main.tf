# 阶段四：基础设施即代码（示意，按云厂商替换 provider）
terraform {
  required_version = ">= 1.6"
  required_providers {
    kubernetes = { source = "hashicorp/kubernetes", version = "~> 2.30" }
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

resource "kubernetes_namespace" "fcp" {
  metadata {
    name = "fcp"
    labels = { name = "fcp" }
  }
}

# 数据库/Kafka 建议使用云托管服务（托管高可用 + 自动备份），
# 不在本文件内自建有状态集群：账务库是唯一权威数据源，故障切换必须可验证。
variable "db_endpoint" {
  type        = string
  description = "MySQL 8 / PostgreSQL 主库地址"
  sensitive   = true
}

variable "kafka_bootstrap" {
  type        = string
  description = "Kafka broker 列表，空则 Outbox 使用内存投递器"
  default     = ""
}

output "namespace" {
  value = kubernetes_namespace.fcp.metadata[0].name
}
