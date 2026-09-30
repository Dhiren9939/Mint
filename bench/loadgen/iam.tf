resource "aws_iam_role" "loadgen" {
  name = "${var.name}-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "ec2.amazonaws.com" }
        Action    = "sts:AssumeRole"
      }
    ]
  })
}

// Session Manager access, no SSH/key pair
resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.loadgen.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

// Lets the CloudWatch agent installed by user_data ship host-level metrics/logs
resource "aws_iam_role_policy_attachment" "cloudwatch_agent" {
  role       = aws_iam_role.loadgen.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

// Stage/pull k6 scripts and push run results, and publish this run's k6 metrics
resource "aws_iam_role_policy" "loadgen_extra" {
  name = "loadgen-extra"
  role = aws_iam_role.loadgen.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:PutObject", "s3:ListBucket"]
        Resource = [aws_s3_bucket.results.arn, "${aws_s3_bucket.results.arn}/*"]
      },
      {
        Effect   = "Allow"
        Action   = "cloudwatch:PutMetricData"
        Resource = "*"
        Condition = {
          StringEquals = { "cloudwatch:namespace" = "K6" }
        }
      }
    ]
  })
}

resource "aws_iam_instance_profile" "loadgen" {
  name = "${var.name}-instance-profile"
  role = aws_iam_role.loadgen.name
}
