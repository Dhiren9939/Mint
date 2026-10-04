variable "public_subnet_id" {
  type        = string
  description = "The subnet id for EC2"
}

variable "ec2_sg_id" {
  type        = string
  description = "The SG for the EC2"
}

variable "iam_role_instance_profile_name" {
  type        = string
  description = "The IAM instance profile for the EC2"
}

variable "ssh_public_key" {
  type        = string
  description = "The public key for Mint Key Pair"
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

variable "domain" {
  type        = string
  description = "The domain Caddy gets a certificate for"
}

variable "repo_url" {
  type        = string
  description = "The repo to clone"
}

variable "repo_branch" {
  type        = string
  description = "The branch to clone"
}

variable "cert_backup_uri" {
  type        = string
  description = "The s3 uri the caddy data is backed up to"
}

variable "user_files_bucket" {
  type        = string
  description = "The user files bucket name"
}
