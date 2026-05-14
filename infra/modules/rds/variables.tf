variable "db_username" {
  type        = string
  description = "The database username"
}

variable "db_password" {
  type        = string
  description = "The database password"
}

variable "db_subnet_group_name" {
  type = string
}

variable "rds_sg_id" {
  type = string
}
