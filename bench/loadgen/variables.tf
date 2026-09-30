variable "region" {
  type        = string
  description = "The AWS region"
  default     = "ap-south-1"
}

variable "name" {
  type        = string
  description = "Name prefix for the load generator's resources"
  default     = "mint-loadgen"
}

variable "instance_type" {
  type = string
  # Compute-optimized instance type. k6 generating a real ramping-arrival-rate load is
  # CPU-bound, and a burstable family (t3/t3a) would throttle mid-run once its credit
  # balance runs out -- c7i.large (2 vCPU, sustained, no credit ceiling) is sized for the
  # few-hundred-rps GET scenarios this bench targets; revisit the size, not just maxVUs,
  # if a run needs much more than that.
  description = "Compute-optimized instance type for k6 (sustained CPU, no burst credit ceiling)"
  default     = "c7i.large"
}

variable "results_bucket_name" {
  type        = string
  description = "S3 bucket used to stage k6 scripts for a run and to collect each run's summary.json (must be globally unique)"
  default     = "dhiren9939-mint-loadgen-results"
}
