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
  type = string
  description = "The public key for Mint Key Pair"
}