resource "aws_s3_bucket" "frontend_bucket" {
  bucket = var.mint_frontend
  force_destroy = true
}

resource "aws_s3_bucket" "user_files" {
  bucket = var.mint_user_files
  force_destroy = true
}
