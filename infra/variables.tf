variable "environment" {
  type        = string
  description = "The deployment environment, prod or the name of the environment branch"
}

variable "frontend_enabled" {
  type        = bool
  description = "Deploy the frontend bucket and CloudFront, backend-only environments set this to false"
  default     = true
}

variable "user_files_enabled" {
  type        = bool
  description = "Deploy the user files bucket"
  default     = true
}

variable "api_ingress_cidrs" {
  type        = list(string)
  description = "CIDRs allowed to reach the API directly when there is no CloudFront"
  default     = []

  validation {
    condition     = var.frontend_enabled || length(var.api_ingress_cidrs) > 0
    error_message = "Backend-only environments need api_ingress_cidrs, otherwise nothing can reach the API."
  }
}

variable "region" {
  description = "The AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "domain_name" {
  description = "The target domain name"
  type        = string
  default     = "dhiren.xyz"
}

variable "acm_certificate_arn" {
  type        = string
  description = "Certificate for this domain"
  default     = "arn:aws:acm:us-east-1:502008133422:certificate/d2a95e5c-f98e-48b1-8ae1-a55269a1e5c2"
}

variable "zone_id" {
  type        = string
  description = "The id of the route53 hosted zone"
  default     = "Z08728257AAJ6Q96KGZY"
}

variable "REDIS_AUTH_TOKEN" {
  type        = string
  sensitive   = true
  description = "The Valkey AUTH token"
}

variable "loadgen_instance_id" {
  type        = string
  description = "Instance id of the bench load generator, adds its CPU, memory and network to the dashboard. The load generator is a separate Terraform root, so set this when it is up"
  default     = null
}
