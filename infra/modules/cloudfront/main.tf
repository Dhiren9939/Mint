resource "aws_cloudfront_distribution" "cdn" {
  enabled = true

  viewer_certificate {
    acm_certificate_arn      = var.acm_certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  origin {
    domain_name              = var.frontend_bucket_domain_name
    origin_id                = "S3-${var.frontend_bucket_domain_name}"
    origin_access_control_id = aws_cloudfront_origin_access_control.oac.id
  }

  origin {
    domain_name = var.backend_ec2_domain_name
    origin_id   = "EC2-${var.backend_ec2_domain_name}"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  ordered_cache_behavior {
    path_pattern             = "/api/*"
    allowed_methods          = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods           = ["GET", "HEAD"]
    target_origin_id         = "EC2-${var.backend_ec2_domain_name}"
    viewer_protocol_policy   = "redirect-to-https"
    // AWS Managed Policies for Caching Optimized and Caching Disabled
    cache_policy_id          = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "S3-${var.frontend_bucket_domain_name}"
    viewer_protocol_policy = "redirect-to-https"
    // AWS Managed Policy for Caching Optimized
    cache_policy_id        = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  }
}

resource "aws_cloudfront_origin_access_control" "oac" {
  name                              = "oac-for-s3-origin"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_s3_bucket_policy" "cloudfront_access" {
  bucket = var.frontend_bucket_id

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      { Sid    = "AllowCloudFrontServicePrincipalReadOnly",
        Effect = "Allow",
        Principal = {
          Service = "cloudfront.amazonaws.com"
        },
        Action   = "s3:GetObject",
        Resource = "${var.frontend_bucket_arn}/*",
        Condition = {
          StringEquals = {
            "AWS:SourceArn" = aws_cloudfront_distribution.cdn.arn
          }
        }
      }
    ]
  })
}


