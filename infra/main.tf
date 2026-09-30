terraform {
  backend "s3" {
    bucket       = "dhiren9939-state-bucket"
    key          = "projects/mint-bench-sql-infra.tfstate"
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

module "iam" {
  source                = "./modules/iam"
  ec2_arn               = module.ec2.server_instance_arn
  user_files_bucket_arn = "arn:aws:s3:::${var.mint_user_files}"
}

module "ec2" {
  source                         = "./modules/ec2"
  ec2_sg_id                      = module.vpc.ec2_sg_id
  public_subnet_id               = module.vpc.public_subnet_id
  iam_role_instance_profile_name = module.iam.iam_instance_profile_name
  ssh_public_key                 = var.ssh_public_key
  db_host                        = module.rds.db_address
  db_username                    = var.db_username
  db_password                    = var.db_password
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
  db_name              = var.db_name
}

module "route53" {
  source         = "./modules/route53"
  domain_name    = var.domain_name
  zone_id        = var.zone_id
  ec2_public_ip  = module.ec2.server_public_ip
}
