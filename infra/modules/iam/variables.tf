variable "user_files_bucket_arn" {
  description = "The user files bucket arn"
  type        = string
}

variable "state_bucket" {
  description = "The bucket that holds the caddy cert backup"
  type        = string
}

variable "cert_prefix" {
  description = "The prefix in that bucket for the caddy data"
  type        = string
}
