output "server_instance_id" {
  value = aws_instance.server.id
}

output "server_instance_arn" {
  value = aws_instance.server.arn
}

output "server_public_ip" {
  value = aws_instance.server.public_ip
}

output "server_public_dns" {
  value = aws_instance.server.public_dns
}
