variable "subdomain" {
  description = "The environment's domain, e.g. mint.dhiren.xyz"
  type        = string
}

variable "zone_id" {
  description = "The id of the hosted zone"
  type        = string
}

variable "use_cloudfront" {
  description = "Point the domain at CloudFront, otherwise straight at the public ALB"
  type        = bool
}

variable "cdn_domain" {
  description = "The domain name of the cloudfront distribution"
  type        = string
  default     = null
}

variable "alb_dns_name" {
  description = "The API load balancer's DNS name"
  type        = string
}

variable "alb_zone_id" {
  description = "The API load balancer's hosted zone id"
  type        = string
}
