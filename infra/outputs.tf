output "vpc_id" {
  value = module.vpc.main_vpc_id
}

output "ecs_cluster" {
  value = module.ecs.cluster_name
}

output "ecs_service" {
  value = module.ecs.service_name
}

output "task_family" {
  value = module.ecs.task_family
}

output "alb_dns_name" {
  value = module.ecs.alb_dns_name
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
