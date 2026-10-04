resource "aws_s3_bucket" "user_files" {
  bucket        = var.bucket_name
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "user_files" {
  bucket                  = aws_s3_bucket.user_files.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "remove_dead_files" {
  bucket = aws_s3_bucket.user_files.bucket

  rule {
    id     = "expiry"
    status = "Enabled"

    filter {}

    expiration {
      days = 1
    }
  }
}

# the browser puts and gets the files with presigned urls, from the app's own domain
resource "aws_s3_bucket_cors_configuration" "user_files_cors" {
  bucket = aws_s3_bucket.user_files.id

  cors_rule {
    allowed_origins = ["https://${var.domain}"]
    allowed_methods = ["GET", "PUT", "HEAD"]
    allowed_headers = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3600
  }
}
