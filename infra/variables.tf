variable "region" {
  description = "The AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "db_username" {
  type        = string
  description = "The Postgres username"
  sensitive   = true
}

variable "db_password" {
  type        = string
  description = "The Postgres password"
  sensitive   = true
}

variable "environment" {
  type        = string
  description = "The environment, the name of its branch. prefixes the resource names, prod gives mint-, anything else mint-<environment>-"
}

variable "ssh_public_key" {
  type        = string
  description = "The public key for Mint Key Pair"
  default     = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0uYGtqbp73M9prIVb1nGl5aCXDqGiQ6cr4E3NIUifo1Mii0Tu/8EhOIqeShfLIc9RIflFru25/0h6P5z01pqjBFEKgtp1UbWkqT/xRXjf93b/M/P7SWjvMbQB+PcLW0i8JqBJO2er+mR5XOGMZa1V3yzbV/dUaE8nYES97RQFI+V10CehvoPHgBhte/zidUUqdrppd+lgSppzst3Wq7OQK1DXXYVD5myrzY2txNaj/dzAKaPIww4HV6xaWPnYfleXwHHqN0XjS56mmw5T4TWzlV+vS2ZQyIWLMaK1OhdDZTBioKQRhSxD87MpuYAxPhnNiWZqkhVbm2NsVL9V38pT MintKey"
}

variable "domain_name" {
  description = "The target domain name"
  type        = string
  default     = "dhiren.xyz"
}

variable "zone_id" {
  type        = string
  description = "The id of the route53 hosted zone"
  default     = "Z08728257AAJ6Q96KGZY"
}

variable "app_subdomain" {
  type        = string
  description = "The app is served at <app_subdomain>.<domain_name>"
  default     = "fs"
}

variable "repo_url" {
  type        = string
  description = "The repo the box clones and builds"
  default     = "https://github.com/Dhiren9939/Mint.git"
}

variable "repo_branch" {
  type        = string
  description = "The branch the box clones and builds"
  default     = "fast-deploy-single-ec2"
}

variable "state_bucket" {
  type        = string
  description = "The bucket the caddy cert is backed up to, same one as the terraform state"
  default     = "dhiren9939-state-bucket"
}

variable "image" {
  type        = string
  description = "The api image a fresh box pulls, backend-cd pushes it"
  default     = "ghcr.io/dhiren9939/mint-backend:latest"
}
