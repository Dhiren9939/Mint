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

variable "vpc_id" {
  type = string
}

variable "alb_arn" {
  type = string
}

variable "alb_dns_name" {
  type = string
}

variable "alb_sg_id" {
  type        = string
  description = "The ALB's security group, CloudFront's VPC origin is admitted into it"
}
