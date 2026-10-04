variable "bucket_name" {
  type        = string
  description = "The user files bucket name"
}

variable "domain" {
  type        = string
  description = "The app domain allowed by CORS, no scheme"
}
