variable "name" {
  type        = string
  description = "Name prefix for this environment's resources"
}

variable "acm_certificate_arn" {
  type = string
}

variable "frontend_bucket_domain_name" {
  type = string
}

variable "subdomain" {
  type = string
}

variable "frontend_bucket_arn" {
  type = string
}

variable "frontend_bucket_name" {
  type = string
}

variable "backend_ec2_domain_name" {
  type = string
}
