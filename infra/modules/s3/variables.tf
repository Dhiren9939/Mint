variable "name" {
  type        = string
  description = "Name prefix for this environment's resources"
}

variable "subdomain" {
  type = string
}

variable "frontend_enabled" {
  type = bool
}

variable "user_files_enabled" {
  type = bool
}
