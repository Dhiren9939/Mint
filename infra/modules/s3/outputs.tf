output "frontend_bucket_arn" {
  value = aws_s3_bucket.frontend_bucket.arn
}

output "frontend_bucket_name" {
  value = aws_s3_bucket.frontend_bucket.bucket
}

output "frontend_bucket_domain_name" {
  value = aws_s3_bucket.frontend_bucket.bucket_regional_domain_name
}

output "user_files_bucket_arn" {
  value = aws_s3_bucket.user_files.arn
}
