resource "aws_route53_record" "mint_a" {
  zone_id = var.zone_id
  name    = "mint-bench-sql.${var.domain_name}"
  type    = "A"
  ttl     = 60
  records = [var.ec2_private_ip]
}
