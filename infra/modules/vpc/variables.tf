variable "name" {
  type        = string
  description = "Name prefix for this environment's resources"
}

variable "cloudfront_origin" {
  type        = bool
  description = "The API sits behind CloudFront, so only CloudFront may reach port 80"
}

variable "api_ingress_cidrs" {
  type        = list(string)
  description = "CIDRs allowed to reach port 80 when there is no CloudFront"
  default     = []
}
