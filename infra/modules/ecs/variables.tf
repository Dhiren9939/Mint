variable "name" {
  type        = string
  description = "Name prefix for this environment's resources"
}

variable "vpc_id" {
  type = string
}

variable "internal" {
  type        = bool
  description = "Internal ALB in the app subnets (behind CloudFront), otherwise internet-facing in the public subnets"
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "app_subnet_ids" {
  type = list(string)
}

variable "alb_sg_id" {
  type = string
}

variable "task_sg_id" {
  type = string
}

variable "task_role_arn" {
  type = string
}

variable "execution_role_arn" {
  type = string
}

variable "image" {
  type        = string
  description = "Image the first task definition revision runs, CI deploys the commit's tag afterwards"
  default     = "ghcr.io/dhiren9939/mint-backend:latest"
}

variable "cpu" {
  type        = number
  description = "Task CPU units, 1024 = 1 vCPU"
  default     = 512
}

variable "memory" {
  type        = number
  description = "Task memory in MiB"
  default     = 1024
}

variable "min_tasks" {
  type    = number
  default = 2
}

variable "max_tasks" {
  type    = number
  default = 4
}

variable "redis_host" {
  type = string
}

variable "redis_auth_token" {
  type      = string
  sensitive = true
}

variable "dynamo_table" {
  type = string
}

variable "user_files_bucket" {
  type = string
}

variable "container_insights_enabled" {
  type        = bool
  description = "Enable ECS Container Insights on the cluster (CPU/memory/task-count metrics for the monitoring dashboard)"
  default     = true
}

variable "extra_environment" {
  type        = map(string)
  description = "Extra environment variables merged into the api container's static list, e.g. raised MINT_CAP_* for bench arms"
  default     = {}
}
