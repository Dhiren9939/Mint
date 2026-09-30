variable "name" {
  type        = string
  description = "Dashboard name, one per environment"
}

variable "region" {
  type        = string
  description = "Region the dashboard's metric widgets query"
}

variable "namespace" {
  type        = string
  description = "Custom Micrometer metric namespace the app publishes to"
  default     = "Mint"
}

variable "log_group_name" {
  type        = string
  description = "Log group the Logs Insights widgets query, for EC2 arms that don't pass ecs.log_group_name"
  default     = null
}

variable "alb" {
  type = object({
    arn_suffix              = string
    target_group_arn_suffix = string
  })
  description = "ALB identifiers for RED-row latency/4xx/5xx widgets, present on ECS arms"
  default     = null
}

variable "ecs" {
  type = object({
    cluster_name   = string
    service_name   = string
    log_group_name = string
  })
  description = "ECS cluster/service for the compute section and its log group for the logs section"
  default     = null
}

variable "ec2" {
  type = object({
    instance_id       = string
    has_redis_sidecar = optional(bool, false)
  })
  description = "EC2 instance for the compute section, bench arms"
  default     = null
}

variable "rds" {
  type = object({
    db_instance_id = string
  })
  description = "RDS instance for the datastore section and Hikari pool widgets, bench-sql only"
  default     = null
}

variable "dynamo" {
  type = object({
    table_name = string
  })
  description = "DynamoDB table for the datastore section"
  default     = null
}

variable "valkey" {
  type = object({
    replication_group_id = string
    member_cluster_ids   = list(string)
  })
  description = "ElastiCache/Valkey replication group for the datastore section"
  default     = null
}

variable "nat" {
  type = object({
    nat_gateway_ids = map(string)
  })
  description = "NAT gateways (by AZ key) for the compute section's ErrorPortAllocation widget"
  default     = null
}

variable "loadgen" {
  type = object({
    enabled     = bool
    instance_id = optional(string)
    arm_name    = optional(string)
  })
  description = "Whether to add the load generator section, its EC2 instance id if known, and the Arm dimension value bench-run.yml publishes k6 metrics under (matches --arm_name on that workflow) so the widgets pick up this env's runs specifically"
  default     = null
}
