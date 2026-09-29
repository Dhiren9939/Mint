// Both wait for their policies, so no task starts before it's allowed to do its job

output "task_role_arn" {
  value      = aws_iam_role.mint_api_role.arn
  depends_on = [aws_iam_role_policy_attachment.attach_mint_api_policy]
}

output "execution_role_arn" {
  value = aws_iam_role.execution_role.arn
  depends_on = [
    aws_iam_role_policy_attachment.execution_role_ecs,
    aws_iam_role_policy.execution_role_secrets
  ]
}
