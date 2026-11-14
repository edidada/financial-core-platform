variable "region" {
  type        = string
  description = "部署区域"
  default     = "ap-guangzhou"
}

variable "environment" {
  type        = string
  description = "dev / staging / prod"
  default     = "dev"
}

variable "replica_count" {
  type        = number
  description = "应用副本数，生产建议 >= 3"
  default     = 3
}

variable "db_engine" {
  type        = string
  description = "mysql8 | postgresql（对应 ADR-0006 双库支持）"
  default     = "mysql8"
  validation {
    condition     = contains(["mysql8", "postgresql"], var.db_engine)
    error_message = "db_engine must be mysql8 or postgresql."
  }
}
