output "primary_endpoint_address" {
  value = aws_elasticache_replication_group.cache.primary_endpoint_address
}

output "port" {
  value = aws_elasticache_replication_group.cache.port
}

output "replication_group_id" {
  value = aws_elasticache_replication_group.cache.replication_group_id
}

output "member_cluster_ids" {
  value = sort(tolist(aws_elasticache_replication_group.cache.member_clusters))
}
