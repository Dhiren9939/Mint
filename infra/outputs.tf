output "api_domain" {
  value = local.app_domain
}

output "server_public_ip" {
  value = module.ec2.server_public_ip
}

output "server_instance_id" {
  value = module.ec2.server_instance_id
}
