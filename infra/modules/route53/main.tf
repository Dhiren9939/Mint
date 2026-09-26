locals {
  // Hosted zone id shared by every CloudFront distribution
  cloudfront_zone_id = "Z2FDTNDATAQYW2"
}

resource "aws_route53_record" "mint_a" {
  count = var.use_cloudfront ? 1 : 0

  alias {
    evaluate_target_health = false
    name                   = var.cdn_domain
    zone_id                = local.cloudfront_zone_id
  }

  name    = var.subdomain
  type    = "A"
  zone_id = var.zone_id
}

resource "aws_route53_record" "mint_aaaa" {
  count = var.use_cloudfront ? 1 : 0

  alias {
    evaluate_target_health = false
    name                   = var.cdn_domain
    zone_id                = local.cloudfront_zone_id
  }

  name    = var.subdomain
  type    = "AAAA"
  zone_id = var.zone_id
}

resource "aws_route53_record" "api_a" {
  count = var.use_cloudfront ? 0 : 1

  name    = var.subdomain
  type    = "A"
  ttl     = 60
  records = [var.server_ip]
  zone_id = var.zone_id
}
