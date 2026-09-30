resource "aws_key_pair" "mint_key" {
  key_name   = "mint-bench-sql-key"
  public_key = var.ssh_public_key
}

resource "aws_instance" "server" {
  instance_type               = "t3.micro"
  key_name                    = aws_key_pair.mint_key.key_name
  ami                         = "ami-0ad737a8b58b3fb92"
  associate_public_ip_address = true
  iam_instance_profile        = var.iam_role_instance_profile_name
  monitoring                  = true

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  credit_specification {
    cpu_credits = "unlimited"
  }

  root_block_device {
    delete_on_termination = true
    volume_size           = 8
    volume_type           = "gp3"
  }

  subnet_id              = var.public_subnet_id
  vpc_security_group_ids = [var.ec2_sg_id]
}

