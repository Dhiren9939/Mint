resource "aws_s3_bucket" "frontend_bucket" {
  bucket        = var.mint_frontend
  force_destroy = true
}

resource "aws_s3_bucket" "user_files" {
  bucket        = var.mint_user_files
  force_destroy = true
}

resource "aws_s3_bucket_cors_configuration" "user_files_cors" {
  bucket = aws_s3_bucket.user_files.id
  cors_rule {
    allowed_methods = ["GET", "PUT", "OPTIONS"]
    allowed_origins = ["https://mint.${var.domain_name}"]
    allowed_headers = ["Content-Length"]
  }
}
