variable "domain_name" {
  description = "The target domain name"
  type        = string
}

variable "zone_id" {
  description = "The id of the hosted zone"
  type        = string
}

variable "ec2_public_ip" {
  description = "The public IP of the backend EC2"
  type        = string
}
