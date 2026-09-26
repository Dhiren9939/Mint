variable "subdomain" {
  description = "The environment's domain, e.g. mint.dhiren.xyz"
  type        = string
}

variable "zone_id" {
  description = "The id of the hosted zone"
  type        = string
}

variable "use_cloudfront" {
  description = "Point the domain at CloudFront, otherwise straight at the server"
  type        = bool
}

variable "cdn_domain" {
  description = "The domain name of the cloudfront distribution"
  type        = string
  default     = null
}

variable "server_ip" {
  description = "The public IP of the API server"
  type        = string
}
