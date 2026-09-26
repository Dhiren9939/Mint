resource "aws_elasticache_replication_group" "cache" {
  replication_group_id = var.name
  description          = "Mint Valkey cache with one replica"

  engine               = "valkey"
  engine_version       = var.engine_version
  node_type            = var.node_type
  parameter_group_name = var.parameter_group_name
  port                 = 6379

  num_cache_clusters          = 2
  preferred_cache_cluster_azs = var.availability_zones
  automatic_failover_enabled  = true
  multi_az_enabled            = true

  subnet_group_name  = var.subnet_group_name
  security_group_ids = [var.security_group_id]

  transit_encryption_enabled = true
  at_rest_encryption_enabled = true
  auth_token                 = var.auth_token

  snapshot_retention_limit   = 0
  auto_minor_version_upgrade = true
}
