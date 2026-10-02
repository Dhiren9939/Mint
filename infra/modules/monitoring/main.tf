locals {
  # EC2 arms pass log_group_name directly; ECS arms carry it on the ecs object.
  log_group_name = var.ecs != null ? var.ecs.log_group_name : var.log_group_name
  # From nullness, which is always known at plan time, even when the name itself isn't yet
  has_log_group = var.ecs != null || var.log_group_name != null

  # The Micrometer CloudWatch registry publishes gauges as <name>.value and timers as
  # <name>.count / .sum / .avg / .max / .percentile.value (with a phi dimension), all tagged with env.
  # Every app widget filters on env, otherwise another environment's metrics in the same namespace,
  # or an earlier run's, would show up. These are the dimension sets the app really publishes with.
  env_filter  = "env=\"${var.env}\""
  http_search = "SEARCH('{${var.namespace},env,error,exception,method,outcome,status,uri} ${local.env_filter} MetricName=\"http.server.requests.count\""
  tomcat_dims = "{${var.namespace},env,name} ${local.env_filter}"

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
  #
  # Counters that only exist once something goes wrong (errors, throttles) are wrapped in
  # FILL(m, 0), otherwise a healthy run shows "no data" instead of a flat zero.
  # ---------------------------------------------------------------------
  sec_outcome = concat(
    [
      {
        type = "metric"
        properties = {
          title  = "Requests per minute by status class (app)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [{ expression = "SUM(${local.http_search} status=2*', 'Sum', 60))", label = "2xx" }],
            [{ expression = "SUM(${local.http_search} status=4*', 'Sum', 60))", label = "4xx" }],
            [{ expression = "SUM(${local.http_search} status=5*', 'Sum', 60))", label = "5xx" }]
          ]
        }
      }
    ],
    jsondecode(var.alb != null ? jsonencode([
      {
        type = "metric"
        properties = {
          title  = "4xx / 5xx (ALB)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["AWS/ApplicationELB", "HTTPCode_Target_4XX_Count", "LoadBalancer", var.alb.arn_suffix, { id = "a4", stat = "Sum", visible = false }],
            ["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "LoadBalancer", var.alb.arn_suffix, { id = "a5", stat = "Sum", visible = false }],
            ["AWS/ApplicationELB", "HTTPCode_ELB_5XX_Count", "LoadBalancer", var.alb.arn_suffix, { id = "a6", stat = "Sum", visible = false }],
            [{ expression = "FILL(a4, 0)", label = "Target 4xx" }],
            [{ expression = "FILL(a5, 0)", label = "Target 5xx" }],
            [{ expression = "FILL(a6, 0)", label = "ELB 5xx" }]
          ]
        }
      }
      ]) : jsonencode([
      {
        type = "metric"
        properties = {
          title  = "429 / 5xx (app)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [{ expression = "SUM(${local.http_search} status=429', 'Sum', 60))", label = "429" }],
            [{ expression = "SUM(${local.http_search} status=5*', 'Sum', 60))", label = "5xx" }]
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
        # Micrometer percentiles are per instance and http.server.requests has none here, so this is
        # the average and the worst request, not a cross-instance percentile.
        type = "metric"
        properties = {
          title  = "Latency avg / max (app)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [{ expression = "AVG(SEARCH('{${var.namespace},env,error,exception,method,outcome,status,uri} ${local.env_filter} MetricName=\"http.server.requests.avg\"', 'Average', 60))", label = "avg" }],
            [{ expression = "MAX(SEARCH('{${var.namespace},env,error,exception,method,outcome,status,uri} ${local.env_filter} MetricName=\"http.server.requests.max\"', 'Maximum', 60))", label = "max" }]
          ]
        }
      }
    ])),
    [
      {
        type = "metric"
        properties = {
          title  = "Cache gets per minute (hit / miss / error)"
          view   = "timeSeries"
          region = var.region
          stat   = "Sum"
          period = 60
          metrics = [
            [var.namespace, "mint.cache.get.count", "env", var.env, "result", "hit", { stat = "Sum", label = "hit" }],
            [var.namespace, "mint.cache.get.count", "env", var.env, "result", "miss", { stat = "Sum", label = "miss" }],
            [var.namespace, "mint.cache.get.count", "env", var.env, "result", "error", { stat = "Sum", label = "error" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "Cache hit rate % (file metadata)"
          view   = "timeSeries"
          region = var.region
          period = 60
          yAxis  = { left = { min = 0, max = 100 } }
          metrics = [
            [var.namespace, "mint.cache.get.count", "env", var.env, "result", "hit", { id = "h", stat = "Sum", visible = false }],
            [var.namespace, "mint.cache.get.count", "env", var.env, "result", "miss", { id = "m", stat = "Sum", visible = false }],
            [{ expression = "100 * h / (h + m)", label = "hit rate %" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "Rate limiter decisions per minute"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [{ expression = "SEARCH('{${var.namespace},env,limit,result} ${local.env_filter} MetricName=\"mint.ratelimit.decisions.count\"', 'Sum', 60)", label = "" }]
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
        title  = "Tomcat threads busy vs max (per task)"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          [{ expression = "SEARCH('${local.tomcat_dims} MetricName=\"tomcat.threads.busy.value\"', 'Average', 60)", label = "busy avg" }],
          [{ expression = "SEARCH('${local.tomcat_dims} MetricName=\"tomcat.threads.busy.value\"', 'Maximum', 60)", label = "busy max" }],
          [{ expression = "SEARCH('${local.tomcat_dims} MetricName=\"tomcat.threads.config.max.value\"', 'Average', 60)", label = "limit" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Tomcat connections (per task)"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          [{ expression = "SEARCH('${local.tomcat_dims} MetricName=\"tomcat.connections.current.value\"', 'Average', 60)", label = "current" }],
          [{ expression = "SEARCH('${local.tomcat_dims} MetricName=\"tomcat.connections.config.max.value\"', 'Average', 60)", label = "limit" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Heap used vs old gen max (per task)"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          [{ expression = "SUM(SEARCH('{${var.namespace},area,env,id} ${local.env_filter} area=\"heap\" MetricName=\"jvm.memory.used.value\"', 'Average', 60))", label = "heap used" }],
          [{ expression = "SEARCH('{${var.namespace},area,env,id} ${local.env_filter} id=\"G1 Old Gen\" MetricName=\"jvm.memory.max.value\"', 'Average', 60)", label = "heap max" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "GC pauses per minute / longest pause"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          [{ expression = "SUM(SEARCH('{${var.namespace},action,cause,env,gc} ${local.env_filter} MetricName=\"jvm.gc.pause.count\"', 'Sum', 60))", label = "pauses / min" }],
          [{ expression = "MAX(SEARCH('{${var.namespace},action,cause,env,gc} ${local.env_filter} MetricName=\"jvm.gc.pause.max\"', 'Maximum', 60))", label = "longest pause (s)", yAxis = "right" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Process CPU (0 to 1 of the task)"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          [var.namespace, "process.cpu.usage.value", "env", var.env, { stat = "Average", label = "avg" }],
          [var.namespace, "process.cpu.usage.value", "env", var.env, { stat = "Maximum", label = "busiest task" }]
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
          [var.namespace, "jvm.threads.live.value", "env", var.env, { stat = "Average" }]
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
          title  = "ECS service CPU / memory %"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ClusterName", var.ecs.cluster_name, "ServiceName", var.ecs.service_name, { stat = "Average", label = "CPU avg" }],
            ["AWS/ECS", "CPUUtilization", "ClusterName", var.ecs.cluster_name, "ServiceName", var.ecs.service_name, { stat = "Maximum", label = "CPU max" }],
            ["AWS/ECS", "MemoryUtilization", "ClusterName", var.ecs.cluster_name, "ServiceName", var.ecs.service_name, { stat = "Average", label = "Memory avg" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "ECS tasks: desired / running / pending"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["ECS/ContainerInsights", "DesiredTaskCount", "ClusterName", var.ecs.cluster_name, "ServiceName", var.ecs.service_name, { stat = "Average", label = "desired" }],
            ["ECS/ContainerInsights", "RunningTaskCount", "ClusterName", var.ecs.cluster_name, "ServiceName", var.ecs.service_name, { stat = "Average", label = "running" }],
            ["ECS/ContainerInsights", "PendingTaskCount", "ClusterName", var.ecs.cluster_name, "ServiceName", var.ecs.service_name, { stat = "Average", label = "pending" }]
          ]
        }
      }
    ],
    jsondecode(var.alb == null ? "[]" : jsonencode([
      {
        type = "metric"
        properties = {
          title  = "ALB healthy / unhealthy hosts"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["AWS/ApplicationELB", "HealthyHostCount", "TargetGroup", var.alb.target_group_arn_suffix, "LoadBalancer", var.alb.arn_suffix, { stat = "Average", label = "healthy" }],
            ["AWS/ApplicationELB", "UnHealthyHostCount", "TargetGroup", var.alb.target_group_arn_suffix, "LoadBalancer", var.alb.arn_suffix, { stat = "Average", label = "unhealthy" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "ALB rejected / target connection errors / requests"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            ["AWS/ApplicationELB", "RejectedConnectionCount", "LoadBalancer", var.alb.arn_suffix, { id = "r1", stat = "Sum", visible = false }],
            ["AWS/ApplicationELB", "TargetConnectionErrorCount", "LoadBalancer", var.alb.arn_suffix, { id = "r2", stat = "Sum", visible = false }],
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", var.alb.arn_suffix, { id = "r3", stat = "Sum", yAxis = "right", label = "requests" }],
            [{ expression = "FILL(r1, 0)", label = "Rejected" }],
            [{ expression = "FILL(r2, 0)", label = "Target conn errors" }]
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
        period = 60
        metrics = concat(
          [for az, id in var.nat.nat_gateway_ids : ["AWS/NATGateway", "ErrorPortAllocation", "NatGatewayId", id, { id = "n_${az}", stat = "Sum", visible = false }]],
          [for az, id in var.nat.nat_gateway_ids : [{ expression = "FILL(n_${az}, 0)", label = az }]]
        )
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
  # Percentiles are per task (Micrometer), so they show the worst task rather than a fleet percentile.
  # ---------------------------------------------------------------------
  sec_deps = concat(
    [
      {
        type = "metric"
        properties = {
          title  = "Rate limiter duration (Lua call)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [var.namespace, "mint.ratelimit.duration.avg", "env", var.env, { stat = "Average", label = "avg" }],
            [var.namespace, "mint.ratelimit.duration.percentile.value", "env", var.env, "phi", "0.95", { stat = "Maximum", label = "p95 (worst task)" }],
            [var.namespace, "mint.ratelimit.duration.percentile.value", "env", var.env, "phi", "0.99", { stat = "Maximum", label = "p99 (worst task)" }]
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
            [{ expression = "AVG(SEARCH('{${var.namespace},env,result} ${local.env_filter} MetricName=\"mint.cache.get.avg\"', 'Average', 60))", label = "get avg" }],
            [{ expression = "AVG(SEARCH('{${var.namespace},env,op,result} ${local.env_filter} MetricName=\"mint.cache.put.avg\"', 'Average', 60))", label = "put avg" }],
            [{ expression = "MAX(SEARCH('{${var.namespace},env,phi,result} ${local.env_filter} phi=\"0.99\" MetricName=\"mint.cache.get.percentile.value\"', 'Maximum', 60))", label = "get p99 (worst task)" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "DB duration by op (avg and p99 of the worst task)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [{ expression = "SEARCH('{${var.namespace},env,op} ${local.env_filter} MetricName=\"mint.db.duration.avg\"', 'Average', 60)", label = "" }],
            [{ expression = "SEARCH('{${var.namespace},env,op,phi} ${local.env_filter} phi=\"0.99\" MetricName=\"mint.db.duration.percentile.value\"', 'Maximum', 60)", label = "" }]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "Redis connected (min across tasks, 1 = all connected)"
          view   = "timeSeries"
          region = var.region
          period = 60
          metrics = [
            [var.namespace, "mint.redis.connected.value", "env", var.env, { stat = "Minimum" }]
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
          ["AWS/DynamoDB", "SuccessfulRequestLatency", "TableName", var.dynamo.table_name, "Operation", "GetItem", { stat = "Average", label = "GetItem avg" }],
          ["AWS/DynamoDB", "SuccessfulRequestLatency", "TableName", var.dynamo.table_name, "Operation", "GetItem", { stat = "p99", label = "GetItem p99" }],
          ["AWS/DynamoDB", "SuccessfulRequestLatency", "TableName", var.dynamo.table_name, "Operation", "PutItem", { stat = "Average", label = "PutItem avg" }]
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
        period = 60
        metrics = [
          ["AWS/DynamoDB", "ThrottledRequests", "TableName", var.dynamo.table_name, { id = "d1", stat = "Sum", visible = false }],
          ["AWS/DynamoDB", "SystemErrors", "TableName", var.dynamo.table_name, { id = "d2", stat = "Sum", visible = false }],
          [{ expression = "FILL(d1, 0)", label = "Throttled" }],
          [{ expression = "FILL(d2, 0)", label = "System errors" }]
        ]
      }
    }
  ]))

  # ElastiCache publishes these per node (CacheClusterId), not per replication group
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
        title  = "Valkey command latency (Eval / Get / Set, µs)"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = concat(
          [for id in var.valkey.member_cluster_ids : ["AWS/ElastiCache", "EvalBasedCmdsLatency", "CacheClusterId", id, { stat = "Average", label = "EVAL ${id}" }]],
          [for id in var.valkey.member_cluster_ids : ["AWS/ElastiCache", "GetTypeCmdsLatency", "CacheClusterId", id, { stat = "Average", label = "GET ${id}" }]],
          [for id in var.valkey.member_cluster_ids : ["AWS/ElastiCache", "SetTypeCmdsLatency", "CacheClusterId", id, { stat = "Average", label = "SET ${id}" }]]
        )
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Valkey connections / memory %"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = concat(
          [for id in var.valkey.member_cluster_ids : ["AWS/ElastiCache", "CurrConnections", "CacheClusterId", id, { stat = "Average", label = "connections ${id}" }]],
          [for id in var.valkey.member_cluster_ids : ["AWS/ElastiCache", "DatabaseMemoryUsagePercentage", "CacheClusterId", id, { stat = "Average", label = "memory % ${id}", yAxis = "right" }]]
        )
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Valkey hits / misses (engine-wide, includes limiter keys)"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = concat(
          [for id in var.valkey.member_cluster_ids : ["AWS/ElastiCache", "CacheHits", "CacheClusterId", id, { stat = "Sum", label = "hits ${id}" }]],
          [for id in var.valkey.member_cluster_ids : ["AWS/ElastiCache", "CacheMisses", "CacheClusterId", id, { stat = "Sum", label = "misses ${id}" }]]
        )
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
  # The generator's own CPU, memory and network, so it can be ruled out as the bottleneck. They come
  # from the CloudWatch agent on the box (namespace MintLoadgen) and the instance's EC2 metrics.
  sec_loadgen = jsondecode(var.loadgen == null || !var.loadgen.enabled || var.loadgen.instance_id == null ? "[]" : jsonencode([
    {
      type = "metric"
      properties = {
        title  = "Load generator CPU %"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          ["MintLoadgen", "cpu_usage_user", "InstanceId", var.loadgen.instance_id, "cpu", "cpu-total", { id = "lu", stat = "Average", visible = false }],
          ["MintLoadgen", "cpu_usage_system", "InstanceId", var.loadgen.instance_id, "cpu", "cpu-total", { id = "ls", stat = "Average", visible = false }],
          [{ expression = "lu + ls", label = "user + system" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Load generator memory %"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          ["MintLoadgen", "mem_used_percent", "InstanceId", var.loadgen.instance_id, { stat = "Average" }]
        ]
      }
    },
    {
      type = "metric"
      properties = {
        title  = "Load generator network (bytes per minute)"
        view   = "timeSeries"
        region = var.region
        period = 60
        metrics = [
          ["AWS/EC2", "NetworkOut", "InstanceId", var.loadgen.instance_id, { stat = "Sum", label = "out" }],
          ["AWS/EC2", "NetworkIn", "InstanceId", var.loadgen.instance_id, { stat = "Sum", label = "in" }]
        ]
      }
    }
  ]))

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
