output "main_vpc_id" {
  value = aws_vpc.main_vpc.id
}

output "public_subnet_ids" {
  value = [for subnet in aws_subnet.public : subnet.id]
}

output "app_subnet_ids" {
  value = [for subnet in aws_subnet.app : subnet.id]
}

output "alb_sg_id" {
  value = aws_security_group.alb_sg.id
}

output "task_sg_id" {
  value = aws_security_group.task_sg.id
}

output "cache_subnet_group_name" {
  value = aws_elasticache_subnet_group.cache.name
}

output "cache_sg_id" {
  value = aws_security_group.cache_sg.id
}

output "private_subnet_azs" {
  value = [for subnet in aws_subnet.cache : subnet.availability_zone]
}

output "nat_gateway_ids" {
  value = { for k, n in aws_nat_gateway.nat : k => n.id }
}
