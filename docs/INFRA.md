# Infrastructure

Generated from `infra/` (Terraform, AWS provider 6.39.0, region `ap-south-1`). Each environment gets its own state and toggles; resource names are `mint` in prod and `mint-<env>` elsewhere.

The frontend and user-files bucket are optional (`frontend_enabled`, `user_files_enabled`). With `frontend_enabled`, CloudFront fronts both S3 and the API, and the API's load balancer is internal: CloudFront reaches it through a VPC origin. Without it, the load balancer is internet-facing and the domain points straight at it, open to `api_ingress_cidrs` only. The cache, DynamoDB table, ECS service and IAM roles are always created.

![Mint infrastructure](infra.svg)

The diagram is a hand-laid-out SVG (`docs/infra.svg`) using the AWS architecture icons. Security groups are the red dashed boxes, with their rules listed inside. Edit the SVG directly when the Terraform changes.

## Request path

1. The browser resolves `mint.<domain>` to CloudFront and connects over HTTPS.
2. `/api/*` goes to the VPC origin: CloudFront connects over a network interface in the app subnets to the internal ALB, on HTTP port 80. The ALB has no public address, and only this account's CloudFront distributions are admitted.
3. The ALB forwards to a healthy task on port 8080. Health checks go to `/actuator/health/liveness` on port 8081, which no listener exposes.
4. Everything else is served from the frontend bucket through Origin Access Control.
5. Uploads and downloads go straight from the browser to the user-files bucket with presigned URLs.

The client IP for rate limiting comes from `X-Forwarded-For`. The app uses Tomcat's native handling (`server.forward-headers-strategy=native`), which reads the header from the right and skips private proxy addresses, so a client can't fake it by sending its own header.

## ECS service

| Setting | Value |
| --- | --- |
| Launch type | Fargate, x86, 0.5 vCPU / 1 GB per task |
| Tasks | 2 to 4, target tracking on 60% average CPU (scale out after 60s, in after 300s) |
| Placement | App subnets in both AZs, no public IP |
| Deploys | Rolling, 100% min healthy / 200% max, circuit breaker with rollback |
| Health check grace | 120s (startup takes about 50s on 0.5 vCPU) |
| Deregistration delay | 30s, then SIGTERM; the app shuts down gracefully within 20s, SIGKILL after 30s |
| Logs | CloudWatch `/ecs/mint-api`, 7 days |
| Image | `ghcr.io/dhiren9939/mint-backend`; Terraform seeds `:latest`, `backend-cd` deploys the commit tag |

The task definition carries the configuration the old Compose file did: `SPRING_PROFILES_ACTIVE=prod`, `REDIS_HOST`, `DYNAMO_TABLE`, `USER_FILES_BUCKET` and the JVM flags as environment variables. The Valkey AUTH token is stored as the SSM SecureString `/<name>/redis-auth-token`, and ECS injects it as `REDIS_AUTH_TOKEN` when a task starts. Terraform ignores changes to the service's task definition and desired count, so CI deploys and autoscaling own them.

## Deploys

`backend-cd` builds the image, pushes the commit and `latest` tags to GHCR, then:

1. Downloads the latest revision of the task definition family (Terraform owns everything in it except the image).
2. Swaps the `api` container's image for the commit tag and registers a new revision.
3. Updates the service and waits for the rolling deploy to finish.
4. Fails unless the service's primary deployment is the new revision and completed, since a rolled-back deploy also ends stable.

`create-app` runs `infra-cd` and then deploys the frontend and backend; `destroy-app` tears the environment down. Neither needs SSH.

## Security groups

| Group | Direction | Port | Peer |
| --- | --- | --- | --- |
| `alb_sg` | ingress | 80 | `CloudFront-VPCOrigins-Service-SG` (frontend enabled, rule lives in the `cloudfront` module) |
| `alb_sg` | ingress | 80 | `api_ingress_cidrs` (frontend disabled) |
| `alb_sg` | egress | 8080, 8081 | `task_sg` |
| `task_sg` | ingress | 8080, 8081 | `alb_sg` |
| `task_sg` | egress | 443 | `0.0.0.0/0` |
| `task_sg` | egress | 6379 | `cache_sg` |
| `cache_sg` | ingress | 6379 | `task_sg` |

## Network

| Subnet | ap-south-1a | ap-south-1b | Holds |
| --- | --- | --- | --- |
| Public | 10.0.0.0/24 | 10.0.1.0/24 | NAT gateways, the ALB when there is no frontend |
| App | 10.0.20.0/24 | 10.0.21.0/24 | ECS tasks, the internal ALB, CloudFront's VPC-origin interfaces |
| Cache | 10.0.10.0/24 | 10.0.11.0/24 | Valkey primary and replica |

Each app subnet routes `0.0.0.0/0` to the NAT gateway in its own AZ, so one AZ failing doesn't cut the other off. DynamoDB and S3 gateway endpoints sit on the app route tables, so that traffic skips the NAT and its per-GB charge. Image pulls from GHCR, the SSM secret and CloudWatch logs go through the NAT.

## Modules

| Module | Resources |
| --- | --- |
| `vpc` | VPC, IGW, public, app and cache subnets in two AZs, a NAT gateway and Elastic IP per AZ, route tables, DynamoDB and S3 gateway endpoints, ElastiCache subnet group, `alb_sg`, `task_sg`, `cache_sg` and their rules |
| `ecs` | Cluster, log group, SSM parameter for the AUTH token, task definition, ALB, target group and listener, service, autoscaling target and CPU policy |
| `iam` | Task role (S3 Get/Put/DeleteObject on the user-files bucket, DynamoDB GetItem/PutItem on the metadata table) and execution role (`AmazonECSTaskExecutionRolePolicy`, read the SSM parameter); both trust `ecs-tasks.amazonaws.com` from this account only |
| `s3` | Frontend bucket, user-files bucket with 1-day lifecycle expiry and CORS for the site origin |
| `cloudfront` | Distribution (S3 default origin, VPC origin for `/api/*`), OAC, bucket policy for OAC, origin request policy forwarding the `MINT_ID` cookie, managed security headers on `/api/*`, the `alb_sg` rule for the VPC origin |
| `route53` | Alias A/AAAA to CloudFront, or an alias A record to the internet-facing ALB |
| `dynamodb` | `file-metadata` table, TTL on `cleanAt` |
| `elasticache` | Valkey replication group, 2 nodes, failover on, encrypted in transit and at rest |

## Notes

- Nothing accepts SSH; there is no instance to log into. Logs are in CloudWatch.
- While an environment is up it pays for two NAT gateways and their Elastic IPs, the ALB and 2 to 4 tasks. `destroy-app` removes all of it.
- CloudFront creates `CloudFront-VPCOrigins-Service-SG` and network interfaces in the app subnets for the VPC origin. They are not in Terraform state and AWS removes them some time after the VPC origin is deleted, so a teardown can fail on the subnets or VPC and need a rerun.
