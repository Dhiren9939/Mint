terraform {
  # backend "s3" {
  #   bucket       = "dhiren9939-state-bucket"
  #   key          = "terraform.tfstate"
  #   region       = "ap-south-1"
  #   use_lockfile = true
  # }

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

module "iam" {
  source                = "./modules/iam"
  ec2_arn               = module.ec2.server_instance_arn
  user_files_bucket_arn = module.s3.user_files_bucket_arn
}

module "route53" {
  source      = "./modules/route53"
  domain_name = var.domain_name
  cdn_domain  = module.cloudfront.cdn_domain_name
  zone_id     = var.zone_id
}

module "s3" {
  source          = "./modules/s3"
  mint_frontend   = var.mint_frontend_bucket
  mint_user_files = var.mint_user_files
}

module "ec2" {
  source                         = "./modules/ec2"
  ec2_sg_id                      = module.vpc.ec2_sg_id
  public_subnet_id               = module.vpc.public_subnet_id
  iam_role_instance_profile_name = module.iam.iam_instance_profile_name
  ssh_public_key                 = var.ssh_public_key
}

module "vpc" {
  source = "./modules/vpc"
}

module "rds" {
  source               = "./modules/rds"
  db_username          = var.db_username
  db_password          = var.db_password
  db_subnet_group_name = module.vpc.rds_subnet_group_name
  rds_sg_id            = module.vpc.rds_sg_id
}

module "cloudfront" {
  source                      = "./modules/cloudfront"
  frontend_bucket_arn         = module.s3.frontend_bucket_arn
  frontend_bucket_id          = module.s3.frontend_bucket_id
  frontend_bucket_domain_name = module.s3.frontend_bucket_domain_name
  backend_ec2_domain_name     = module.ec2.server_public_dns
  acm_certificate_arn         = var.acm_certificate_arn
}
