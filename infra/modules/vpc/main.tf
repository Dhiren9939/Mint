resource "aws_vpc" "main_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
}

resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.main_vpc.id
}

resource "aws_subnet" "public_subnet" {
  cidr_block = "10.0.0.0/24"

  vpc_id            = aws_vpc.main_vpc.id
  availability_zone = "ap-south-1a"
}

resource "aws_route_table" "public_rt" {

  vpc_id = aws_vpc.main_vpc.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.gw.id
  }

  route {
    cidr_block = "10.0.0.0/16"
    gateway_id = "local"
  }
}

resource "aws_route_table_association" "public_rt_a" {
  subnet_id      = aws_subnet.public_subnet.id
  route_table_id = aws_route_table.public_rt.id
}

resource "aws_security_group" "ec2_sg" {
  vpc_id = aws_vpc.main_vpc.id
}

data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_security_group_rule" "cloudfront_to_ec2" {
  count = var.cloudfront_origin ? 1 : 0

  type              = "ingress"
  security_group_id = aws_security_group.ec2_sg.id
  protocol          = "tcp"
  from_port         = 80
  to_port           = 80
  prefix_list_ids   = [data.aws_ec2_managed_prefix_list.cloudfront.id]
}

resource "aws_security_group_rule" "api_to_ec2" {
  count = !var.cloudfront_origin && length(var.api_ingress_cidrs) > 0 ? 1 : 0

  type              = "ingress"
  security_group_id = aws_security_group.ec2_sg.id
  protocol          = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_blocks       = var.api_ingress_cidrs
}

resource "aws_security_group_rule" "allow_ssh" {
  type              = "ingress"
  security_group_id = aws_security_group.ec2_sg.id
  protocol          = "tcp"
  from_port         = 22
  to_port           = 22
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "ec2_to_internet" {
  type              = "egress"
  security_group_id = aws_security_group.ec2_sg.id
  protocol          = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_subnet" "private_a" {
  cidr_block        = "10.0.10.0/24"
  vpc_id            = aws_vpc.main_vpc.id
  availability_zone = "ap-south-1a"
}

resource "aws_subnet" "private_b" {
  cidr_block        = "10.0.11.0/24"
  vpc_id            = aws_vpc.main_vpc.id
  availability_zone = "ap-south-1b"
}

resource "aws_elasticache_subnet_group" "cache" {
  name       = "${var.name}-cache-subnets"
  subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_b.id]
}

resource "aws_security_group" "cache_sg" {
  name   = "${var.name}-cache"
  vpc_id = aws_vpc.main_vpc.id
}

resource "aws_security_group_rule" "ec2_to_cache_ingress" {
  type                     = "ingress"
  security_group_id        = aws_security_group.cache_sg.id
  protocol                 = "tcp"
  from_port                = 6379
  to_port                  = 6379
  source_security_group_id = aws_security_group.ec2_sg.id
}

resource "aws_security_group_rule" "ec2_to_cache_egress" {
  type                     = "egress"
  security_group_id        = aws_security_group.ec2_sg.id
  protocol                 = "tcp"
  from_port                = 6379
  to_port                  = 6379
  source_security_group_id = aws_security_group.cache_sg.id
}
