data "aws_region" "current" {}

locals {
  container_name = "api"
}

resource "aws_ecs_cluster" "cluster" {
  name = var.name
}

resource "aws_cloudwatch_log_group" "api" {
  name              = "/ecs/${var.name}-api"
  retention_in_days = 7
}

// Injected into the container by ECS at start, never stored in the task definition
resource "aws_ssm_parameter" "redis_auth_token" {
  name  = "/${var.name}/redis-auth-token"
  type  = "SecureString"
  value = var.redis_auth_token
}

# ---------- Task definition ----------

// CI registers new revisions with the commit's image; this one only seeds the family
resource "aws_ecs_task_definition" "api" {
  family                   = "${var.name}-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.task_role_arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = local.container_name
      image     = var.image
      essential = true

      portMappings = [
        { containerPort = 8080, protocol = "tcp" },
        { containerPort = 8081, protocol = "tcp" }
      ]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "REDIS_HOST", value = var.redis_host },
        { name = "DYNAMO_TABLE", value = var.dynamo_table },
        { name = "USER_FILES_BUCKET", value = var.user_files_bucket },
        { name = "JAVA_TOOL_OPTIONS", value = "-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=40 -XX:+UseG1GC" }
      ]

      secrets = [
        { name = "REDIS_AUTH_TOKEN", valueFrom = aws_ssm_parameter.redis_auth_token.arn }
      ]

      // SIGKILL after this long; the app's graceful shutdown finishes within 20s
      stopTimeout = 30

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.api.name
          awslogs-region        = data.aws_region.current.region
          awslogs-stream-prefix = local.container_name
        }
      }
    }
  ])
}

# ---------- Load balancer ----------

resource "aws_lb" "api" {
  name               = "${var.name}-api"
  internal           = var.internal
  load_balancer_type = "application"
  security_groups    = [var.alb_sg_id]
  subnets            = var.internal ? var.app_subnet_ids : var.public_subnet_ids

  drop_invalid_header_fields = true
}

resource "aws_lb_target_group" "api" {
  name        = "${var.name}-api"
  vpc_id      = var.vpc_id
  target_type = "ip"
  protocol    = "HTTP"
  port        = 8080

  // Lets in-flight requests finish before a stopping task is removed
  deregistration_delay = 30

  // The management port isn't on any listener, only the ALB's health checks reach it
  health_check {
    protocol            = "HTTP"
    port                = "8081"
    path                = "/actuator/health/liveness"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.api.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}

# ---------- Service ----------

resource "aws_ecs_service" "api" {
  name            = "${var.name}-api"
  cluster         = aws_ecs_cluster.cluster.id
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = var.min_tasks
  launch_type     = "FARGATE"

  // Startup takes ~50s on 0.5 vCPU, don't count failed health checks before then
  health_check_grace_period_seconds = 120

  // Rolling deploy: new tasks start next to the old ones, old ones stop once new ones are healthy
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets          = var.app_subnet_ids
    security_groups  = [var.task_sg_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = local.container_name
    container_port   = 8080
  }

  // The target group must be attached to the load balancer first
  depends_on = [aws_lb_listener.http]

  // CI owns deploys and autoscaling owns the count
  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }
}

# ---------- Autoscaling ----------

resource "aws_appautoscaling_target" "api" {
  service_namespace  = "ecs"
  resource_id        = "service/${aws_ecs_cluster.cluster.name}/${aws_ecs_service.api.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  min_capacity       = var.min_tasks
  max_capacity       = var.max_tasks
}

resource "aws_appautoscaling_policy" "cpu" {
  name               = "${var.name}-api-cpu"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.api.service_namespace
  resource_id        = aws_appautoscaling_target.api.resource_id
  scalable_dimension = aws_appautoscaling_target.api.scalable_dimension

  target_tracking_scaling_policy_configuration {
    target_value       = 60
    scale_out_cooldown = 60
    scale_in_cooldown  = 300

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}
