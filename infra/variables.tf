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

variable "db_username" {
  type        = string
  description = "The username for the RDS database"
  sensitive   = true
}

variable "db_password" {
  type        = string
  description = "The password for the RDS database"
  sensitive   = true
}

variable "mint_frontend_bucket" {
  type    = string
  default = "mint-frontend-bucket"
}

variable "mint_user_files" {
  type    = string
  default = "mint-user-files-bucket"
}

variable "acm_certificate_arn" {
  type        = string
  description = "Certificate for this domain"
}

variable "zone_id" {
  type = string
  description = "The id of the route53 hosted zone"
}

variable "ssh_public_key" {
  type = string
  description = "The public key for Mint Key Pair"
}