variable "record_name" {
  description = "The full record name, like mint.example.com"
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
