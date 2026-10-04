resource "aws_key_pair" "mint_key" {
  key_name   = "mint-fast-key"
  public_key = var.ssh_public_key
}

resource "aws_instance" "server" {
  instance_type               = "t3.micro"
  key_name                    = aws_key_pair.mint_key.key_name
  ami                         = "ami-0ad737a8b58b3fb92"
  associate_public_ip_address = true
  iam_instance_profile        = var.iam_role_instance_profile_name

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    domain      = var.domain
    repo_url    = var.repo_url
    repo_branch = var.repo_branch
    cert_backup = var.cert_backup_uri
    user_files  = var.user_files_bucket
    image       = var.image
    db_username = var.db_username
    db_password = var.db_password
  })
  user_data_replace_on_change = false

  # postgres lives on this disk, a new user data must not replace the instance
  lifecycle {
    ignore_changes = [user_data]
  }

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  root_block_device {
    delete_on_termination = true
    volume_size           = 16
    volume_type           = "gp3"
  }

  subnet_id              = var.public_subnet_id
  vpc_security_group_ids = [var.ec2_sg_id]
}

