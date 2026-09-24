output "main_vpc_id" {
  value = aws_vpc.main_vpc.id
}

output "public_subnet_id" {
  value = aws_subnet.public_subnet.id
}

output "ec2_sg_id" {
  value = aws_security_group.ec2_sg.id
}