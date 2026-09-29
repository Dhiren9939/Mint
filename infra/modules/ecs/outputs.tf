output "cluster_name" {
  value = aws_ecs_cluster.cluster.name
}

output "service_name" {
  value = aws_ecs_service.api.name
}

output "task_family" {
  value = aws_ecs_task_definition.api.family
}

output "redis_auth_token_parameter_arn" {
  value = aws_ssm_parameter.redis_auth_token.arn
}

output "alb_arn" {
  value = aws_lb.api.arn
}

output "alb_dns_name" {
  value = aws_lb.api.dns_name
}

output "alb_zone_id" {
  value = aws_lb.api.zone_id
}
