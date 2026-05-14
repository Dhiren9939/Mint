output "main_vpc_id" {
  value = aws_vpc.main_vpc.id
}

output "main_vpc_arn" {
  value = aws_vpc.main_vpc.arn
}

output "gateway_id" {
  value = aws_internet_gateway.gw.id
}

output "gateway_arn" {
  value = aws_internet_gateway.gw.arn
}

output "public_subnet_id" {
  value = aws_subnet.public_subnet.id
}

output "public_subnet_arn" {
  value = aws_subnet.public_subnet.arn
}

output "private_subnet_a_id" {
  value = aws_subnet.private_subnet_a.id
}

output "private_subnet_a_arn" {
  value = aws_subnet.private_subnet_a.arn
}

output "private_subnet_b_id" {
  value = aws_subnet.private_subnet_b.id
}

output "private_subnet_b_arn" {
  value = aws_subnet.private_subnet_b.arn
}

output "public_route_table_id" {
  value = aws_route_table.public_rt.id
}

output "public_route_table_arn" {
  value = aws_route_table.public_rt.arn
}

output "private_route_table_id" {
  value = aws_route_table.private_rt.id
}

output "private_route_table_arn" {
  value = aws_route_table.private_rt.arn
}

output "public_rt_association_id" {
  value = aws_route_table_association.public_rt_a.id
}

output "private_rt_a_association_id" {
  value = aws_route_table_association.private_rt_a.id
}

output "private_rt_b_association_id" {
  value = aws_route_table_association.private_rt_b.id
}

output "ec2_sg_id" {
  value = aws_security_group.ec2_sg.id
}

output "ec2_sg_arn" {
  value = aws_security_group.ec2_sg.arn
}

output "rds_sg_id" {
  value = aws_security_group.rds_sg.id
}

output "rds_sg_arn" {
  value = aws_security_group.rds_sg.arn
}

output "rds_subnet_group_id" {
  value = aws_db_subnet_group.rds_subnet_group.id
}

output "rds_subnet_group_arn" {
  value = aws_db_subnet_group.rds_subnet_group.arn
}

output "rds_subnet_group_name" {
  value = aws_db_subnet_group.rds_subnet_group.name
}
