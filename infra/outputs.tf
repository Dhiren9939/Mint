output "vpc_id" {
  value = module.vpc.main_vpc_id
}

output "public_subnet_id" {
  value = module.vpc.public_subnet_id
}

output "server_public_ip" {
  value = module.ec2.server_public_ip
}

output "server_instance_id" {
  value = module.ec2.server_instance_id
}

output "db_endpoint" {
  value = module.rds.db_endpoint
}
