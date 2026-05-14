variable "domain_name" {
  description = "The target domain name"
  type        = string
}

variable "zone_id" {
  description = "The id of the hosted zone"
  type = string
}

variable "cdn_domain" {
  description = "The domain name of the cloudfront distribution"
  type = string
}