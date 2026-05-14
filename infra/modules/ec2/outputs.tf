output "mint_key_arn" {
  value = aws_key_pair.mint_key.arn
}

output "mint_key_id" {
  value = aws_key_pair.mint_key.id
}

output "server_instance_id" {
  value = aws_instance.server.id
}

output "server_instance_arn" {
  value = aws_instance.server.arn
}

output "server_public_ip" {
  value = aws_instance.server.public_ip
}

output "server_private_ip" {
  value = aws_instance.server.private_ip
}

output "server_public_dns" {
  value = aws_instance.server.public_dns
}
