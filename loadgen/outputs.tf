output "loadgen_public_ip" {
  value = aws_instance.loadgen.public_ip
}

output "loadgen_instance_id" {
  value = aws_instance.loadgen.id
}

output "ssh_command" {
  value = "ssh -i mintkey.pem admin@${aws_instance.loadgen.public_ip}"
}
