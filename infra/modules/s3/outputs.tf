output "frontend_bucket_arn" {
  value = one(aws_s3_bucket.frontend_bucket[*].arn)
}

output "frontend_bucket_name" {
  value = one(aws_s3_bucket.frontend_bucket[*].bucket)
}

output "frontend_bucket_domain_name" {
  value = one(aws_s3_bucket.frontend_bucket[*].bucket_regional_domain_name)
}

output "user_files_bucket_arn" {
  value = one(aws_s3_bucket.user_files[*].arn)
}

output "user_files_bucket_name" {
  value = one(aws_s3_bucket.user_files[*].bucket)
}
