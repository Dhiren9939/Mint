locals {
  # EC2 arms pass log_group_name directly; ECS arms carry it on the ecs object.
  log_group_name = var.ecs != null ? var.ecs.log_group_name : var.log_group_name
  # From nullness, which is always known at plan time, even when the name itself isn't yet
  has_log_group = var.ecs != null || var.log_group_name != null

  # ---------------------------------------------------------------------
  # Section 1 — Outcome / RED
  #
  # Every ternary below that picks between widget lists has string branches,
  # jsondecode(cond ? jsonencode([...]) : "[]"), never list branches. HCL
  # requires both branches of a ternary to have the same type, and two widget
  # lists almost never do (different lengths, different attributes). Wrapping
  # each branch in jsondecode(jsonencode(...)) instead only looks like it works:
  # it passes `terraform validate`, where ids are unknown, then fails at apply,
  # where they are known and the tuples get concrete, mismatched types.
  # ---------------------------------------------------------------------
  sec_outcome = concat(
    [
      {
        type = "metric"
        properties = {
          title  = "Request rate by uri + status"
          view   = "timeSeries"
          region = var.region
          stat   = "Sum"
          period = 60
          metrics = [
            [{ expression = "SEARCH('{${var.namespace},uri,status} MetricName=\"http.server.requests\"', 'Sum', 60)", label = "" }]
          ]
        }
      }
    ],
    jsondecode(var.alb != null ? jsonencode([
      {
        type = "metric"
        properties = {
          title  = "4xx / 5xx rate (ALB)"
          view   = "timeSeries"
          region = var.region
          stat   = "Sum"
          period = 60
          metrics = [
            ["AWS/ApplicationELB", "HTTPCode_Target_4XX_Count", "LoadBalancer", var.alb.arn_suffix, { stat = "Sum" }],
            ["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "LoadBalancer", var.alb.arn_suffix, { stat = "Sum" }],
            ["AWS/ApplicationELB", "HTTPCode_ELB_5XX_Count", "LoadBalancer", var.alb.arn_suffix, { stat = "Sum" }]
          ]
        }
      }
      ]) : jsonencode([
      {
        type = "metric"
        properties = {
          title  = "4xx / 429 / 5xx rate (app)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [{ expression = "SEARCH('{${var.namespace},uri,status} MetricName=\"http.server.requests\" status=4*', 'Sum', 60)", label = "4xx" }],
            [{ expression = "SEARCH('{${var.namespace},uri,status} MetricName=\"http.server.requests\" status=5*', 'Sum', 60)", label = "5xx" }],
            [{ expression = "SEARCH('{${var.namespace},uri,status} MetricName=\"http.server.requests\" status=429', 'Sum', 60)", label = "429" }]
          ]
        }
      }
    ])),
    jsondecode(var.alb != null ? jsonencode([
      {
        type = "metric"
        properties = {
          title  = "Latency p50 / p95 / p99 (ALB TargetResponseTime)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", var.alb.arn_suffix, { stat = "p50", label = "p50" }],
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", var.alb.arn_suffix, { stat = "p95", label = "p95" }],
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", var.alb.arn_suffix, { stat = "p99", label = "p99" }]
          ]
        }
      }
      ]) : jsonencode([
      {
        # EC2 arms get their percentiles from app metrics; Micrometer percentiles are
        # per-instance only, so this is a placeholder, not a true cross-instance percentile.
        type = "metric"
        properties = {
          title  = "Latency p50 / p95 / p99 (app, per instance)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [var.namespace, "http.server.requests", { stat = "p50", label = "p50" }],
            [var.namespace, "http.server.requests", { stat = "p95", label = "p95" }],
            [var.namespace, "http.server.requests", { stat = "p99", label = "p99" }]
          ]
        }
      }
    ])),
    [
      {
        type = "metric"
        properties = {
          title  = "Cache hit / miss / error"
          view   = "timeSeries"
          region = var.region
          stat   = "Sum"
          period = 60
          metrics = [
            [var.namespace, "mint.cache.get", "result", "hit", { stat = "Sum", label = "hit" }],
            [var.namespace, "mint.cache.get", "result", "miss", { stat = "Sum", label = "miss" }],
            [var.namespace, "mint.cache.get", "result", "error", { stat = "Sum", label = "error" }],
            [var.namespace, "mint.cache.get", "result", "disconnected", { stat = "Sum", label = "disconnected" }]
          ]
        }
      }
    ]
  )

  # ---------------------------------------------------------------------
  # Section 2 — App saturation (always from the custom Mint namespace)
  # ---------------------------------------------------------------------
  sec_app_saturation = [
    {
      type = "metric"
      properties = {
        title  = "Tomcat threads busy vs max"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          [var.namespace, "tomcat.threads.busy", { stat = "Average", label = "busy" }],
          [var.namespace, "tomcat.threads.config.max", { stat = "Average", label = "max" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Tomcat connections"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          [{ expression = "SEARCH('{${var.namespace}} MetricName=\"tomcat.connections.current\"', 'Average', 60)", label = "current" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Heap used vs max"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          [var.namespace, "jvm.memory.used", { stat = "Average", label = "used" }],
          [var.namespace, "jvm.memory.max", { stat = "Average", label = "max" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "GC pause"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          [var.namespace, "jvm.gc.pause", { stat = "Average", label = "avg" }],
          [var.namespace, "jvm.gc.pause", { stat = "Maximum", label = "max" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Process CPU"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          [var.namespace, "process.cpu.usage", { stat = "Average" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Live threads"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          [var.namespace, "jvm.threads.live", { stat = "Average" }]
        ]
      }
    }
  ]

  # ---------------------------------------------------------------------
  # Section 3 — Compute (ECS + NAT, or EC2)
  # ---------------------------------------------------------------------
  sec_compute_ecs = jsondecode(var.ecs == null ? "[]" : jsonencode(concat(
    [
      {
        type = "metric"
        properties = {
          title  = "ECS service CPU / memory (Container Insights)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["ECS/ContainerInsights", "CPUUtilization", "ClusterName", var.ecs.cluster_name, "ServiceName", var.ecs.service_name, { stat = "Average", label = "CPU" }],
            ["ECS/ContainerInsights", "MemoryUtilization", "ClusterName", var.ecs.cluster_name, "ServiceName", var.ecs.service_name, { stat = "Average", label = "Memory" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "ECS running task count"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["ECS/ContainerInsights", "RunningTaskCount", "ClusterName", var.ecs.cluster_name, "ServiceName", var.ecs.service_name, { stat = "Average" }]
          ]
        }
      }
    ],
    jsondecode(var.alb == null ? "[]" : jsonencode([
      {
        type = "metric"
        properties = {
          title  = "ALB healthy hosts"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["AWS/ApplicationELB", "HealthyHostCount", "TargetGroup", var.alb.target_group_arn_suffix, "LoadBalancer", var.alb.arn_suffix, { stat = "Average" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "ALB ELB 5xx / rejected / target connection errors"
          view   = "timeSeries"
          region = var.region
          stat   = "Sum"
          period = 60
          metrics = [
            ["AWS/ApplicationELB", "HTTPCode_ELB_5XX_Count", "LoadBalancer", var.alb.arn_suffix, { stat = "Sum", label = "ELB 5xx" }],
            ["AWS/ApplicationELB", "RejectedConnectionCount", "LoadBalancer", var.alb.arn_suffix, { stat = "Sum", label = "Rejected" }],
            ["AWS/ApplicationELB", "TargetConnectionErrorCount", "LoadBalancer", var.alb.arn_suffix, { stat = "Sum", label = "Target conn errors" }]
          ]
        }
      }
    ]))
  )))

  sec_compute_nat = jsondecode(var.nat == null ? "[]" : jsonencode([
    {
      type = "metric"
      properties = {
        title  = "NAT ErrorPortAllocation"
        view   = "timeSeries"
        region = var.region
        stat   = "Sum"
        period = 60
        metrics = [
          for az, id in var.nat.nat_gateway_ids : ["AWS/NATGateway", "ErrorPortAllocation", "NatGatewayId", id, { stat = "Sum", label = az }]
        ]
      }
    }
  ]))

  sec_compute_ec2 = jsondecode(var.ec2 == null ? "[]" : jsonencode(concat(
    [
      {
        type = "metric"
        properties = {
          title  = "EC2 CPU / credits"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["AWS/EC2", "CPUUtilization", "InstanceId", var.ec2.instance_id, { stat = "Average", label = "CPU %" }],
            ["AWS/EC2", "CPUCreditBalance", "InstanceId", var.ec2.instance_id, { stat = "Average", label = "Credit balance" }],
            ["AWS/EC2", "CPUSurplusCreditsCharged", "InstanceId", var.ec2.instance_id, { stat = "Sum", label = "Surplus charged" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "Memory used % (CWAgent)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["CWAgent", "mem_used_percent", "InstanceId", var.ec2.instance_id, { stat = "Average" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "TCP established / time_wait (CWAgent)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["CWAgent", "tcp_established", "InstanceId", var.ec2.instance_id, { stat = "Average", label = "established" }],
            ["CWAgent", "tcp_time_wait", "InstanceId", var.ec2.instance_id, { stat = "Average", label = "time_wait" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "java process CPU / memory (procstat)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["CWAgent", "procstat_cpu_usage", "InstanceId", var.ec2.instance_id, "exe", "java", { stat = "Average", label = "CPU" }],
            ["CWAgent", "procstat_memory_rss", "InstanceId", var.ec2.instance_id, "exe", "java", { stat = "Average", label = "Mem RSS" }]
          ]
        }
      }
    ],
    jsondecode(!var.ec2.has_redis_sidecar ? "[]" : jsonencode([
      {
        type = "metric"
        properties = {
          title  = "redis-server process CPU / memory (procstat)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["CWAgent", "procstat_cpu_usage", "InstanceId", var.ec2.instance_id, "exe", "redis-server", { stat = "Average", label = "CPU" }],
            ["CWAgent", "procstat_memory_rss", "InstanceId", var.ec2.instance_id, "exe", "redis-server", { stat = "Average", label = "Mem RSS" }]
          ]
        }
      }
    ]))
  )))

  sec_compute = concat(local.sec_compute_ecs, local.sec_compute_nat, local.sec_compute_ec2)

  # ---------------------------------------------------------------------
  # Section 4 — Dependency timers (app view, Mint namespace)
  # ---------------------------------------------------------------------
  sec_deps = concat(
    [
      {
        type = "metric"
        properties = {
          title  = "Rate limiter duration"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [var.namespace, "mint.ratelimit.duration", { stat = "Average", label = "avg" }],
            [var.namespace, "mint.ratelimit.duration", { stat = "p95", label = "p95" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "Cache get / put duration"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [var.namespace, "mint.cache.get", { stat = "Average", label = "get avg" }],
            [var.namespace, "mint.cache.put", { stat = "Average", label = "put avg" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "DB duration by op"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [var.namespace, "mint.db.duration", "op", "find", { stat = "Average", label = "find" }],
            [var.namespace, "mint.db.duration", "op", "save", { stat = "Average", label = "save" }],
            [var.namespace, "mint.db.duration", "op", "isFree", { stat = "Average", label = "isFree" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "Redis connected"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [var.namespace, "mint.redis.connected", { stat = "Average" }]
          ]
        }
      }
    ],
    jsondecode(var.rds == null ? "[]" : jsonencode([
      {
        type = "metric"
        properties = {
          title  = "Hikari pool: pending / active / acquire"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [var.namespace, "hikaricp.connections.pending", { stat = "Average", label = "pending" }],
            [var.namespace, "hikaricp.connections.active", { stat = "Average", label = "active" }],
            [var.namespace, "hikaricp.connections.acquire", { stat = "Average", label = "acquire" }]
          ]
        }
      }
    ]))
  )

  # ---------------------------------------------------------------------
  # Section 5 — Datastores (AWS view)
  # ---------------------------------------------------------------------
  sec_dynamo = jsondecode(var.dynamo == null ? "[]" : jsonencode([
    {
      type = "metric"
      properties = {
        title  = "DynamoDB latency (Get/Put)"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          ["AWS/DynamoDB", "SuccessfulRequestLatency", "TableName", var.dynamo.table_name, "Operation", "GetItem", { stat = "Average", label = "GetItem" }],
          ["AWS/DynamoDB", "SuccessfulRequestLatency", "TableName", var.dynamo.table_name, "Operation", "PutItem", { stat = "Average", label = "PutItem" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "DynamoDB consumed capacity"
        view   = "timeSeries"
        region = var.region
        stat   = "Sum"
        period = 60
        metrics = [
          ["AWS/DynamoDB", "ConsumedReadCapacityUnits", "TableName", var.dynamo.table_name, { stat = "Sum", label = "RCU" }],
          ["AWS/DynamoDB", "ConsumedWriteCapacityUnits", "TableName", var.dynamo.table_name, { stat = "Sum", label = "WCU" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "DynamoDB throttles / system errors"
        view   = "timeSeries"
        region = var.region
        stat   = "Sum"
        period = 60
        metrics = [
          ["AWS/DynamoDB", "ThrottledRequests", "TableName", var.dynamo.table_name, { stat = "Sum", label = "Throttled" }],
          ["AWS/DynamoDB", "SystemErrors", "TableName", var.dynamo.table_name, { stat = "Sum", label = "System errors" }]
        ]
      }
    }
  ]))

  sec_valkey = jsondecode(var.valkey == null ? "[]" : jsonencode([
    {
      type = "metric"
      properties = {
        title  = "Valkey engine CPU"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          for id in var.valkey.member_cluster_ids : ["AWS/ElastiCache", "EngineCPUUtilization", "CacheClusterId", id, { stat = "Average", label = id }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Valkey command latency (Eval / Get / Set)"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          ["AWS/ElastiCache", "EvalBasedCmdsLatency", "ReplicationGroupId", var.valkey.replication_group_id, { stat = "Average", label = "EVAL" }],
          ["AWS/ElastiCache", "GetTypeCmdsLatency", "ReplicationGroupId", var.valkey.replication_group_id, { stat = "Average", label = "GET" }],
          ["AWS/ElastiCache", "SetTypeCmdsLatency", "ReplicationGroupId", var.valkey.replication_group_id, { stat = "Average", label = "SET" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Valkey connections / memory %"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          ["AWS/ElastiCache", "CurrConnections", "ReplicationGroupId", var.valkey.replication_group_id, { stat = "Average", label = "Connections" }],
          ["AWS/ElastiCache", "DatabaseMemoryUsagePercentage", "ReplicationGroupId", var.valkey.replication_group_id, { stat = "Average", label = "Memory %" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Valkey hits / misses"
        view   = "timeSeries"
        region = var.region
        stat   = "Sum"
        period = 60
        metrics = [
          ["AWS/ElastiCache", "CacheHits", "ReplicationGroupId", var.valkey.replication_group_id, { stat = "Sum", label = "Hits" }],
          ["AWS/ElastiCache", "CacheMisses", "ReplicationGroupId", var.valkey.replication_group_id, { stat = "Sum", label = "Misses" }]
        ]
      }
    }
  ]))

  sec_rds = jsondecode(var.rds == null ? "[]" : jsonencode([
    {
      type = "metric"
      properties = {
        title  = "RDS CPU / credits"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", var.rds.db_instance_id, { stat = "Average", label = "CPU %" }],
          ["AWS/RDS", "CPUCreditBalance", "DBInstanceIdentifier", var.rds.db_instance_id, { stat = "Average", label = "Credit balance" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "RDS connections"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", var.rds.db_instance_id, { stat = "Average" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "RDS read/write latency"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          ["AWS/RDS", "ReadLatency", "DBInstanceIdentifier", var.rds.db_instance_id, { stat = "Average", label = "Read" }],
          ["AWS/RDS", "WriteLatency", "DBInstanceIdentifier", var.rds.db_instance_id, { stat = "Average", label = "Write" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "RDS read/write IOPS"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          ["AWS/RDS", "ReadIOPS", "DBInstanceIdentifier", var.rds.db_instance_id, { stat = "Average", label = "Read" }],
          ["AWS/RDS", "WriteIOPS", "DBInstanceIdentifier", var.rds.db_instance_id, { stat = "Average", label = "Write" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "RDS disk queue depth / freeable memory"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          ["AWS/RDS", "DiskQueueDepth", "DBInstanceIdentifier", var.rds.db_instance_id, { stat = "Average", label = "Queue depth" }],
          ["AWS/RDS", "FreeableMemory", "DBInstanceIdentifier", var.rds.db_instance_id, { stat = "Average", label = "Freeable memory" }]
        ]
      }
    }
  ]))

  sec_datastores = concat(local.sec_dynamo, local.sec_valkey, local.sec_rds)

  # ---------------------------------------------------------------------
  # Section 6 — Load generator
  # ---------------------------------------------------------------------
  # Published post-run (not live) by bench/loadgen/run-scenario.sh, namespace "K6", one
  # data point per bench-run.yml invocation, dimension Arm=<loadgen.arm_name>. If arm_name
  # isn't set these widgets render with no matching data rather than erroring.
  sec_loadgen = jsondecode(var.loadgen == null || !var.loadgen.enabled ? "[]" : jsonencode(concat(
    [
      {
        type = "metric"
        properties = {
          title  = "k6 iteration rate / dropped iterations"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["K6", "iteration_rate", "Arm", coalesce(var.loadgen.arm_name, "unknown"), { stat = "Average", label = "Iteration rate" }],
            ["K6", "dropped_iterations", "Arm", coalesce(var.loadgen.arm_name, "unknown"), { stat = "Sum", label = "Dropped iterations", yAxis = "right" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "k6 download p95 / p99 and rejection rates"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["K6", "download_p95", "Arm", coalesce(var.loadgen.arm_name, "unknown"), { stat = "Average", label = "Download p95 (ms)" }],
            ["K6", "download_p99", "Arm", coalesce(var.loadgen.arm_name, "unknown"), { stat = "Average", label = "Download p99 (ms)" }],
            ["K6", "rate_limited_rate", "Arm", coalesce(var.loadgen.arm_name, "unknown"), { stat = "Average", label = "Rate-limited %", yAxis = "right" }],
            ["K6", "server_errors_rate", "Arm", coalesce(var.loadgen.arm_name, "unknown"), { stat = "Average", label = "Server error %", yAxis = "right" }],
            ["K6", "client_errors_rate", "Arm", coalesce(var.loadgen.arm_name, "unknown"), { stat = "Average", label = "Client error %", yAxis = "right" }]
          ]
        }
      }
    ],
    jsondecode(var.loadgen.instance_id == null ? "[]" : jsonencode([
      {
        type = "metric"
        properties = {
          title  = "Load-gen instance CPU / network"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["AWS/EC2", "CPUUtilization", "InstanceId", var.loadgen.instance_id, { stat = "Average", label = "CPU %" }],
            ["AWS/EC2", "NetworkOut", "InstanceId", var.loadgen.instance_id, { stat = "Average", label = "Network out" }]
          ]
        }
      }
    ]))
  )))

  # ---------------------------------------------------------------------
  # Generic 3-column positioning: width 8, height 6, stacked section by section
  # so any subset of sections renders without overlap.
  # ---------------------------------------------------------------------
  sections = [
    local.sec_outcome,
    local.sec_app_saturation,
    local.sec_compute,
    local.sec_deps,
    local.sec_datastores,
    local.sec_loadgen,
  ]

  section_row_counts = [for s in local.sections : ceil(length(s) / 3.0)]

  section_y_offsets = [
    for i in range(length(local.sections)) :
    sum(concat([0], [for j in range(i) : local.section_row_counts[j] * 6]))
  ]

  positioned_sections = [
    for i, s in local.sections : [
      for idx, w in s : merge(w, {
        x      = (idx % 3) * 8
        y      = local.section_y_offsets[i] + floor(idx / 3) * 6
        width  = 8
        height = 6
      })
    ]
  ]

  metrics_widgets = flatten(local.positioned_sections)

  logs_y = length(local.metrics_widgets) == 0 ? 0 : local.section_y_offsets[length(local.sections) - 1] + local.section_row_counts[length(local.sections) - 1] * 6

  log_widgets = jsondecode(!local.has_log_group ? "[]" : jsonencode([
    {
      type   = "log"
      x      = 0
      y      = local.logs_y
      width  = 24
      height = 6
      properties = {
        title  = "Errors"
        region = var.region
        view   = "table"
        query  = "SOURCE '${local.log_group_name}' | fields @timestamp, @message | filter @message like /ERROR/"
      }
    },
    {
      type   = "log"
      x      = 0
      y      = local.logs_y + 6
      width  = 24
      height = 6
      properties = {
        title  = "Rate limiter / cache fail-open warnings"
        region = var.region
        view   = "table"
        query  = "SOURCE '${local.log_group_name}' | fields @timestamp, @message | filter @message like /Rate limiter unavailable|Cache read failed/"
      }
    }
  ]))

  all_widgets = concat(local.metrics_widgets, local.log_widgets)
}

resource "aws_cloudwatch_dashboard" "this" {
  dashboard_name = var.name

  dashboard_body = jsonencode({
    widgets = local.all_widgets
  })
}
