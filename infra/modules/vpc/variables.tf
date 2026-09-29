variable "name" {
  type        = string
  description = "Name prefix for this environment's resources"
}

variable "cloudfront_origin" {
  type        = bool
  description = "The API sits behind CloudFront, so the ALB is internal and only CloudFront may reach it"
}

variable "api_ingress_cidrs" {
  type        = list(string)
  description = "CIDRs allowed to reach the public ALB when there is no CloudFront"
  default     = []
}
