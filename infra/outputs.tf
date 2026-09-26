output "vpc_id" {
  value = module.vpc.main_vpc_id
}

output "public_subnet_id" {
  value = module.vpc.public_subnet_id
}

output "ec2_public_ip" {
  value = module.ec2.server_public_ip
}

output "ec2_public_dns" {
  value = module.ec2.server_public_dns
}

output "server_instance_id" {
  value = module.ec2.server_instance_id
}

output "redis_endpoint" {
  value = module.elasticache.primary_endpoint_address
}

output "table_name" {
  value = module.dynamodb.table_name
}

output "user_files_bucket" {
  description = "The user files bucket, none when the environment has no bucket"
  value       = coalesce(module.s3.user_files_bucket_name, "none")
}

output "frontend_enabled" {
  value = var.frontend_enabled
}

output "frontend_bucket_name" {
  value = module.s3.frontend_bucket_name
}

output "cloudfront_distribution_id" {
  value = one(module.cloudfront[*].distribution_id)
}

output "api_url" {
  value = var.frontend_enabled ? "https://${local.subdomain}" : "http://${local.subdomain}"
}
