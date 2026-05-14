resource "aws_route53_record" "mint_aaaa" {
  alias {
    evaluate_target_health = false
    name                   = var.cdn_domain
    zone_id                = "Z2FDTNDATAQYW2"
  }

  name    = "mint.${var.domain_name}"
  type    = "AAAA"
  zone_id = var.zone_id
}

resource "aws_route53_record" "mint_a" {
  alias {
    evaluate_target_health = false
    name                   = var.cdn_domain
    zone_id                = "Z2FDTNDATAQYW2"
  }

  name    = "mint.${var.domain_name}"
  type    = "A"
  zone_id = var.zone_id
}
