resource "aws_s3_bucket" "frontend_bucket" {
  count = var.frontend_enabled ? 1 : 0

  bucket        = "${var.name}-frontend-bucket"
  force_destroy = true
}

resource "aws_s3_bucket" "user_files" {
  count = var.user_files_enabled ? 1 : 0

  bucket        = "${var.name}-user-files-bucket"
  force_destroy = true
}

resource "aws_s3_bucket_lifecycle_configuration" "remove_dead_files" {
  count  = var.user_files_enabled ? 1 : 0
  bucket = aws_s3_bucket.user_files[0].bucket

  rule {
    id     = "expiry"
    status = "Enabled"

    expiration {
      days = 1
    }
  }
}

resource "aws_s3_bucket_cors_configuration" "user_files_cors" {
  count  = var.user_files_enabled ? 1 : 0
  bucket = aws_s3_bucket.user_files[0].id
  cors_rule {
    allowed_origins = ["https://${var.subdomain}"]
    allowed_methods = ["GET", "PUT", "HEAD"]
    allowed_headers = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3600
  }
}
