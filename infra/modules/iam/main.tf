resource "aws_iam_role" "mint_api_role" {
  name = "${var.name}-api-role"

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
  name = "${var.name}-instance-profile"
  role = aws_iam_role.mint_api_role.name
}

resource "aws_iam_role_policy_attachment" "attach_mint_api_policy" {
  role       = aws_iam_role.mint_api_role.name
  policy_arn = aws_iam_policy.mint_api_role_policy.arn
}

resource "aws_iam_policy" "mint_api_role_policy" {
  name_prefix = "${var.name}-api-"

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = concat(
      var.user_files_bucket_arn == null ? [] : [
        {
          Effect = "Allow",
          Action = [
            "s3:GetObject",
            "s3:PutObject",
            "s3:DeleteObject"
          ]
          Resource = "${var.user_files_bucket_arn}/*"
        }
      ],
      [
        {
          Effect = "Allow",
          Action = [
            "dynamodb:GetItem",
            "dynamodb:PutItem"
          ],
          Resource = var.file_meta_data_table_arn
        }
      ]
    )
  })
}
