locals {
  // One entry per availability zone: a public subnet (NAT, public ALB), an app subnet
  // (ECS tasks, internal ALB) and a cache subnet (Valkey)
  zones = {
    a = { az = "ap-south-1a", public = "10.0.0.0/24", app = "10.0.20.0/24", cache = "10.0.10.0/24" }
    b = { az = "ap-south-1b", public = "10.0.1.0/24", app = "10.0.21.0/24", cache = "10.0.11.0/24" }
  }
}

data "aws_region" "current" {}

resource "aws_vpc" "main_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
}

// Also required for CloudFront VPC origins, even though they don't route through it
resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.main_vpc.id
}

# ---------- Public subnets ----------

resource "aws_subnet" "public" {
  for_each = local.zones

  vpc_id            = aws_vpc.main_vpc.id
  cidr_block        = each.value.public
  availability_zone = each.value.az
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

resource "aws_route_table_association" "public" {
  for_each = aws_subnet.public

  subnet_id      = each.value.id
  route_table_id = aws_route_table.public_rt.id
}

# ---------- App subnets (private, outbound through a NAT in the same zone) ----------

resource "aws_eip" "nat" {
  for_each = local.zones

  domain = "vpc"
}

resource "aws_nat_gateway" "nat" {
  for_each = local.zones

  allocation_id = aws_eip.nat[each.key].id
  subnet_id     = aws_subnet.public[each.key].id

  depends_on = [aws_internet_gateway.gw]
}

resource "aws_subnet" "app" {
  for_each = local.zones

  vpc_id            = aws_vpc.main_vpc.id
  cidr_block        = each.value.app
  availability_zone = each.value.az
}

resource "aws_route_table" "app_rt" {
  for_each = local.zones

  vpc_id = aws_vpc.main_vpc.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat[each.key].id
  }
}

resource "aws_route_table_association" "app" {
  for_each = aws_subnet.app

  subnet_id      = each.value.id
  route_table_id = aws_route_table.app_rt[each.key].id
}

// Free gateway endpoints: DynamoDB and S3 calls from the tasks skip the NAT and its per-GB charge
resource "aws_vpc_endpoint" "dynamodb" {
  vpc_id            = aws_vpc.main_vpc.id
  service_name      = "com.amazonaws.${data.aws_region.current.region}.dynamodb"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = [for rt in aws_route_table.app_rt : rt.id]
}

resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.main_vpc.id
  service_name      = "com.amazonaws.${data.aws_region.current.region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = [for rt in aws_route_table.app_rt : rt.id]
}

# ---------- Cache subnets ----------

resource "aws_subnet" "cache" {
  for_each = local.zones

  vpc_id            = aws_vpc.main_vpc.id
  cidr_block        = each.value.cache
  availability_zone = each.value.az
}

resource "aws_elasticache_subnet_group" "cache" {
  name       = "${var.name}-cache-subnets"
  subnet_ids = [for subnet in aws_subnet.cache : subnet.id]
}

# ---------- Security groups ----------

resource "aws_security_group" "alb_sg" {
  name   = "${var.name}-alb"
  vpc_id = aws_vpc.main_vpc.id
}

resource "aws_security_group" "task_sg" {
  name   = "${var.name}-task"
  vpc_id = aws_vpc.main_vpc.id
}

resource "aws_security_group" "cache_sg" {
  name   = "${var.name}-cache"
  vpc_id = aws_vpc.main_vpc.id
}

// CloudFront's ingress to the ALB lives in the cloudfront module: it comes from the security
// group CloudFront creates along with the VPC origin, which only exists after that is created
resource "aws_security_group_rule" "api_to_alb" {
  count = !var.cloudfront_origin && length(var.api_ingress_cidrs) > 0 ? 1 : 0

  type              = "ingress"
  security_group_id = aws_security_group.alb_sg.id
  protocol          = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_blocks       = var.api_ingress_cidrs
}

// 8080 serves the API, 8081 the health checks
resource "aws_security_group_rule" "alb_to_task_egress" {
  for_each = toset(["8080", "8081"])

  type                     = "egress"
  security_group_id        = aws_security_group.alb_sg.id
  protocol                 = "tcp"
  from_port                = each.value
  to_port                  = each.value
  source_security_group_id = aws_security_group.task_sg.id
}

resource "aws_security_group_rule" "alb_to_task_ingress" {
  for_each = toset(["8080", "8081"])

  type                     = "ingress"
  security_group_id        = aws_security_group.task_sg.id
  protocol                 = "tcp"
  from_port                = each.value
  to_port                  = each.value
  source_security_group_id = aws_security_group.alb_sg.id
}

// Image pulls, AWS APIs (SSM, logs, Dynamo and S3 through the endpoints)
resource "aws_security_group_rule" "task_to_internet" {
  type              = "egress"
  security_group_id = aws_security_group.task_sg.id
  protocol          = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "task_to_cache_egress" {
  type                     = "egress"
  security_group_id        = aws_security_group.task_sg.id
  protocol                 = "tcp"
  from_port                = 6379
  to_port                  = 6379
  source_security_group_id = aws_security_group.cache_sg.id
}

resource "aws_security_group_rule" "task_to_cache_ingress" {
  type                     = "ingress"
  security_group_id        = aws_security_group.cache_sg.id
  protocol                 = "tcp"
  from_port                = 6379
  to_port                  = 6379
  source_security_group_id = aws_security_group.task_sg.id
}
