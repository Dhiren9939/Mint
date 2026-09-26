output "main_vpc_id" {
  value = aws_vpc.main_vpc.id
}

output "public_subnet_id" {
  value = aws_subnet.public_subnet.id
}

output "ec2_sg_id" {
  value = aws_security_group.ec2_sg.id
}

output "cache_subnet_group_name" {
  value = aws_elasticache_subnet_group.cache.name
}

output "cache_sg_id" {
  value = aws_security_group.cache_sg.id
}

output "private_subnet_azs" {
  value = [aws_subnet.private_a.availability_zone, aws_subnet.private_b.availability_zone]
}
