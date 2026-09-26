variable "name" {
  type        = string
  description = "Replication group id"
  default     = "mint-cache"
}

variable "subnet_group_name" {
  type        = string
  description = "The cache subnet group, spanning at least two AZs"
}

variable "security_group_id" {
  type        = string
  description = "The security group attached to the cache nodes"
}

variable "availability_zones" {
  type        = list(string)
  description = "AZs for the nodes, the first one hosts the initial primary"
}

variable "node_type" {
  type    = string
  default = "cache.t4g.micro"
}

variable "engine_version" {
  type    = string
  default = "9.1"
}

variable "parameter_group_name" {
  type    = string
  default = "default.valkey9"
}

variable "auth_token" {
  type        = string
  sensitive   = true
  description = "The Valkey AUTH token, 16 to 128 characters"
}
