terraform {
  backend "s3" {
    bucket       = "dhiren9939-state-bucket"
    region       = "ap-south-1"
    use_lockfile = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.39.0"
    }
  }
}

provider "aws" {
  region = var.region
}

locals {
  name      = var.environment == "prod" ? "mint" : "mint-${var.environment}"
  subdomain = "${local.name}.${var.domain_name}"
}

module "iam" {
  source                   = "./modules/iam"
  name                     = local.name
  user_files_bucket_arn    = module.s3.user_files_bucket_arn
  file_meta_data_table_arn = module.dynamodb.file_metadata_table_arn
  secret_parameter_arns    = [module.ecs.redis_auth_token_parameter_arn]
}

module "route53" {
  source         = "./modules/route53"
  subdomain      = local.subdomain
  zone_id        = var.zone_id
  cdn_domain     = one(module.cloudfront[*].cdn_domain_name)
  alb_dns_name   = module.ecs.alb_dns_name
  alb_zone_id    = module.ecs.alb_zone_id
  use_cloudfront = var.frontend_enabled
}

module "s3" {
  source             = "./modules/s3"
  name               = local.name
  subdomain          = local.subdomain
  frontend_enabled   = var.frontend_enabled
  user_files_enabled = var.user_files_enabled
}

module "ecs" {
  source = "./modules/ecs"
  name   = local.name

  // Behind CloudFront the ALB is internal and reached through a VPC origin
  internal          = var.frontend_enabled
  vpc_id            = module.vpc.main_vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
  app_subnet_ids    = module.vpc.app_subnet_ids
  alb_sg_id         = module.vpc.alb_sg_id
  task_sg_id        = module.vpc.task_sg_id

  task_role_arn      = module.iam.task_role_arn
  execution_role_arn = module.iam.execution_role_arn

  redis_host        = module.elasticache.primary_endpoint_address
  redis_auth_token  = var.REDIS_AUTH_TOKEN
  dynamo_table      = module.dynamodb.table_name
  user_files_bucket = coalesce(module.s3.user_files_bucket_name, "none")
}

module "vpc" {
  source            = "./modules/vpc"
  name              = local.name
  cloudfront_origin = var.frontend_enabled
  api_ingress_cidrs = var.api_ingress_cidrs
}

module "dynamodb" {
  source     = "./modules/dynamodb"
  table_name = "${local.name}-file-metadata"
}

module "cloudfront" {
  source = "./modules/cloudfront"
  count  = var.frontend_enabled ? 1 : 0

  name                        = local.name
  subdomain                   = local.subdomain
  frontend_bucket_arn         = module.s3.frontend_bucket_arn
  frontend_bucket_name        = module.s3.frontend_bucket_name
  frontend_bucket_domain_name = module.s3.frontend_bucket_domain_name
  vpc_id                      = module.vpc.main_vpc_id
  alb_arn                     = module.ecs.alb_arn
  alb_dns_name                = module.ecs.alb_dns_name
  alb_sg_id                   = module.vpc.alb_sg_id
  acm_certificate_arn         = var.acm_certificate_arn
}

module "elasticache" {
  source             = "./modules/elasticache"
  name               = "${local.name}-cache"
  subnet_group_name  = module.vpc.cache_subnet_group_name
  security_group_id  = module.vpc.cache_sg_id
  availability_zones = module.vpc.private_subnet_azs
  auth_token         = var.REDIS_AUTH_TOKEN
}

module "monitoring" {
  source = "./modules/monitoring"
  name   = "${local.name}-dashboard"
  region = var.region

  ecs = {
    cluster_name   = module.ecs.cluster_name
    service_name   = module.ecs.service_name
    log_group_name = module.ecs.log_group_name
  }

  alb = {
    arn_suffix              = module.ecs.alb_arn_suffix
    target_group_arn_suffix = module.ecs.target_group_arn_suffix
  }

  dynamo = {
    table_name = module.dynamodb.table_name
  }

  valkey = {
    replication_group_id = module.elasticache.replication_group_id
    member_cluster_ids   = module.elasticache.member_cluster_ids
  }

  nat = {
    nat_gateway_ids = module.vpc.nat_gateway_ids
  }
}
