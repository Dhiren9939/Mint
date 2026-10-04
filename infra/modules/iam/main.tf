resource "aws_iam_role" "mint_api_role" {
  name = "mint-fast-api-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "mint-fast-ec2-instance-profile"
  role = aws_iam_role.mint_api_role.name
}

resource "aws_iam_role_policy_attachment" "attach_mint_api_policy" {
  role       = aws_iam_role.mint_api_role.name
  policy_arn = aws_iam_policy.mint_api_role_policy.arn
}

resource "aws_iam_role_policy_attachment" "cloudwatch_agent" {
  role       = aws_iam_role.mint_api_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

resource "aws_iam_policy" "mint_api_role_policy" {
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject"
        ]
        Resource = "${var.user_files_bucket_arn}/*"
      },
      {
        Effect   = "Allow",
        Action   = ["s3:GetObject", "s3:PutObject"]
        Resource = "arn:aws:s3:::${var.state_bucket}/${var.cert_prefix}/*"
      },
      {
        Effect   = "Allow",
        Action   = "s3:ListBucket"
        Resource = "arn:aws:s3:::${var.state_bucket}"
        Condition = {
          StringLike = { "s3:prefix" = ["${var.cert_prefix}/*"] }
        }
      }
    ]
  })
}
