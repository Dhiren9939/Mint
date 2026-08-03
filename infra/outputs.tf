output "vpc_id" {
  value = module.vpc.main_vpc_id
}

output "public_subnet_id" {
  value = module.vpc.public_subnet_id
}

output "server_public_ip" {
  value = module.ec2.server_public_ip
}

output "ec2_public_ip" {
  value = module.ec2.server_public_ip
}

output "ec2_public_dns" {
  value = module.ec2.server_public_dns
}

output "frontend_bucket_name" {
  value = module.s3.frontend_bucket_name
}

output "cloudfront_distribution_id" {
  value = module.cloudfront.distribution_id
}

output "server_instance_id" {
  value = module.ec2.server_instance_id
}

output "db_endpoint" {
  value = module.rds.db_endpoint
}
