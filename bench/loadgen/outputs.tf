output "instance_id" {
  value = aws_instance.loadgen.id
}

output "public_ip" {
  description = "The EIP. Add this to each bench arm's api_ingress_cidrs (infra/envs/*.tfvars)."
  value       = aws_eip.loadgen.public_ip
}

output "results_bucket_name" {
  value = aws_s3_bucket.results.bucket
}
