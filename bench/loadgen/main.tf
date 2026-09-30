# Standalone, throwaway Terraform root: one load-gen EC2 instance, created once and
# reused across every bench arm. Uses the account's default VPC/subnets rather than
# provisioning its own network -- this is intentionally the simplest possible setup.

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# ---------- Results/scripts staging bucket ----------

resource "aws_s3_bucket" "results" {
  bucket = var.results_bucket_name
}

resource "aws_s3_bucket_public_access_block" "results" {
  bucket = aws_s3_bucket.results.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# ---------- Security group ----------

resource "aws_security_group" "loadgen" {
  name   = "${var.name}-sg"
  vpc_id = data.aws_vpc.default.id
}

// Egress only: the instance calls out to the target API and to AWS APIs (SSM, S3,
// CloudWatch). SSM Session Manager is outbound-only over the public internet through the
// default VPC's IGW, so no inbound rule is needed at all.
resource "aws_security_group_rule" "egress_all" {
  type              = "egress"
  security_group_id = aws_security_group.loadgen.id
  protocol          = "-1"
  from_port         = 0
  to_port           = 0
  cidr_blocks       = ["0.0.0.0/0"]
}

# ---------- Instance ----------

resource "aws_instance" "loadgen" {
  ami                         = data.aws_ami.ubuntu.id
  instance_type               = var.instance_type
  subnet_id                   = data.aws_subnets.default.ids[0]
  vpc_security_group_ids      = [aws_security_group.loadgen.id]
  iam_instance_profile        = aws_iam_instance_profile.loadgen.name
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/user_data.sh", {
    results_bucket = aws_s3_bucket.results.bucket
  })

  tags = {
    Name = var.name
  }
}

resource "aws_eip" "loadgen" {
  instance = aws_instance.loadgen.id
  domain   = "vpc"
}
