variable "name" {
  type        = string
  description = "Name prefix for this environment's resources"
}

variable "user_files_bucket_arn" {
  description = "The user files bucket arn, null when there is no bucket"
  type        = string
  default     = null
}

variable "file_meta_data_table_arn" {
  type = string
}