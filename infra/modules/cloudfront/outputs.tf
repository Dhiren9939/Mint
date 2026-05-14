output "cdn_domain_name" {
  value = aws_cloudfront_distribution.cdn.domain_name
}

output "cdn_ip_prefix" {
  value = aws_cloudfront_distribution.cdn.anycast_ip_list_id
}