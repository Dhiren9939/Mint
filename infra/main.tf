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
}

module "route53" {
  source         = "./modules/route53"
  subdomain      = local.subdomain
  zone_id        = var.zone_id
  cdn_domain     = one(module.cloudfront[*].cdn_domain_name)
  server_ip      = module.ec2.server_public_ip
  use_cloudfront = var.frontend_enabled
}

module "s3" {
  source             = "./modules/s3"
  name               = local.name
  subdomain          = local.subdomain
  frontend_enabled   = var.frontend_enabled
  user_files_enabled = var.user_files_enabled
}

module "ec2" {
  source                         = "./modules/ec2"
  name                           = local.name
  ec2_sg_id                      = module.vpc.ec2_sg_id
  public_subnet_id               = module.vpc.public_subnet_id
  iam_role_instance_profile_name = module.iam.iam_instance_profile_name
  ssh_public_key                 = var.ssh_public_key
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
  backend_ec2_domain_name     = module.ec2.server_public_dns
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
