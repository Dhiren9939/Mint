locals {
  api_origin_id = "ALB-${var.name}-api"
}

// nosniff, frame options, HSTS and referrer policy on API responses
data "aws_cloudfront_response_headers_policy" "security_headers" {
  name = "Managed-SecurityHeadersPolicy"
}

// CloudFront reaches the internal ALB through a network interface in the VPC; the ALB has no
// public address. HTTP between them never leaves AWS's network, viewers still use HTTPS
resource "aws_cloudfront_vpc_origin" "api" {
  vpc_origin_endpoint_config {
    name                   = "${var.name}-api"
    arn                    = var.alb_arn
    http_port              = 80
    https_port             = 443
    origin_protocol_policy = "http-only"

    origin_ssl_protocols {
      items    = ["TLSv1.2"]
      quantity = 1
    }
  }
}

// Created by CloudFront along with the first VPC origin in the VPC. Admitting only this group
// lets only this account's distributions in, unlike the CloudFront prefix list
data "aws_security_group" "vpc_origins" {
  vpc_id = var.vpc_id

  filter {
    name   = "group-name"
    values = ["CloudFront-VPCOrigins-Service-SG*"]
  }

  depends_on = [aws_cloudfront_vpc_origin.api]
}

resource "aws_security_group_rule" "cloudfront_to_alb" {
  type                     = "ingress"
  security_group_id        = var.alb_sg_id
  protocol                 = "tcp"
  from_port                = 80
  to_port                  = 80
  source_security_group_id = data.aws_security_group.vpc_origins.id
}

resource "aws_cloudfront_distribution" "cdn" {
  enabled             = true
  default_root_object = "index.html"
  aliases             = [var.subdomain]

  custom_error_response {
    error_code         = 403
    response_code      = 404
    response_page_path = "/"
  }

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
    domain_name = var.alb_dns_name
    origin_id   = local.api_origin_id

    vpc_origin_config {
      vpc_origin_id = aws_cloudfront_vpc_origin.api.id
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  ordered_cache_behavior {
    path_pattern           = "/api/*"
    allowed_methods        = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = local.api_origin_id
    viewer_protocol_policy = "redirect-to-https"
    // AWS Managed Policies for Caching Optimized and Caching Disabled
    cache_policy_id            = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"
    origin_request_policy_id   = aws_cloudfront_origin_request_policy.api_cookies.id
    response_headers_policy_id = data.aws_cloudfront_response_headers_policy.security_headers.id
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "S3-${var.frontend_bucket_domain_name}"
    viewer_protocol_policy = "redirect-to-https"
    // AWS Managed Policy for Caching Optimized
    cache_policy_id = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  }
}

resource "aws_cloudfront_origin_access_control" "oac" {
  name                              = "${var.name}-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_s3_bucket_policy" "cloudfront_access" {
  bucket = var.frontend_bucket_name

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

resource "aws_cloudfront_origin_request_policy" "api_cookies" {
  name = "${var.name}-api-cookies"

  cookies_config {
    cookie_behavior = "whitelist"

    cookies {
      items = ["MINT_ID"]
    }
  }

  headers_config {
    header_behavior = "none"
  }

  query_strings_config {
    query_string_behavior = "all"
  }
}
