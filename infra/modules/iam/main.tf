data "aws_caller_identity" "current" {}

locals {
  // Only ECS tasks in this account may assume the roles below
  ecs_tasks_trust_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow",
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
        Action = "sts:AssumeRole"
        Condition = {
          StringEquals = {
            "aws:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      }
    ]
  })
}

# ---------- Task role: what the app's own AWS calls run as ----------

resource "aws_iam_role" "mint_api_role" {
  name               = "${var.name}-api-role"
  assume_role_policy = local.ecs_tasks_trust_policy
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
        },
        {
          Effect   = "Allow",
          Action   = "cloudwatch:PutMetricData",
          Resource = "*",
          Condition = {
            StringEquals = {
              "cloudwatch:namespace" = "Mint"
            }
          }
        }
      ]
    )
  })
}

# ---------- Execution role: what ECS uses to start the task ----------

resource "aws_iam_role" "execution_role" {
  name               = "${var.name}-api-execution-role"
  assume_role_policy = local.ecs_tasks_trust_policy
}

// Pull images and write logs
resource "aws_iam_role_policy_attachment" "execution_role_ecs" {
  role       = aws_iam_role.execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

// Read the secrets injected into the container. They use the AWS managed SSM key,
// which needs no extra KMS permission
resource "aws_iam_role_policy" "execution_role_secrets" {
  name = "read-secrets"
  role = aws_iam_role.execution_role.id

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect   = "Allow",
        Action   = "ssm:GetParameters",
        Resource = var.secret_parameter_arns
      }
    ]
  })
}
