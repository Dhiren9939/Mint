terraform {
  backend "s3" {
    bucket       = "dhiren9939-state-bucket"
    key          = "projects/mint-fast-deploy-infra.tfstate"
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
  name        = var.environment == "prod" ? "mint" : "mint-${var.environment}"
  app_domain  = "${var.app_subdomain}.${var.domain_name}"
  user_files  = "${local.name}-user-files-bucket"
  cert_prefix = "mint-fast-deploy/caddy"
}

module "iam" {
  source                = "./modules/iam"
  user_files_bucket_arn = module.s3.user_files_bucket_arn
  state_bucket          = var.state_bucket
  cert_prefix           = local.cert_prefix
}

module "ec2" {
  source                         = "./modules/ec2"
  ec2_sg_id                      = module.vpc.ec2_sg_id
  public_subnet_id               = module.vpc.public_subnet_id
  iam_role_instance_profile_name = module.iam.iam_instance_profile_name
  ssh_public_key                 = var.ssh_public_key
  db_username                    = var.db_username
  db_password                    = var.db_password
  domain                         = local.app_domain
  repo_url                       = var.repo_url
  repo_branch                    = var.repo_branch
  user_files_bucket              = local.user_files
  image                          = var.image
  cert_backup_uri                = "s3://${var.state_bucket}/${local.cert_prefix}"
}

module "s3" {
  source      = "./modules/s3"
  bucket_name = local.user_files
  domain      = local.app_domain
}

module "vpc" {
  source = "./modules/vpc"
}

module "route53" {
  source        = "./modules/route53"
  record_name   = local.app_domain
  zone_id       = var.zone_id
  ec2_public_ip = module.ec2.server_public_ip
}
