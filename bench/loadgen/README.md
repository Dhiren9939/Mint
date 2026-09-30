# bench/loadgen

Standalone Terraform root for the load-generator EC2 instance, created once and reused
across every bench arm. Own state (`projects/mint/loadgen.tfstate`, via the `key`
injected at `terraform init` time -- see `backend.tf`), own provider block.

## What it creates

- One `c7i.large` EC2 instance in `ap-south-1` (compute-optimized -- k6 generating a real
  ramping-vus load is CPU-bound, and a burstable family like t3/t3a would throttle
  mid-run once its credit balance ran out), in the account's **default VPC/subnets** (no
  dedicated VPC -- this is intentionally the simplest possible setup for a standalone,
  throwaway root), with an EIP.
- An IAM role/instance profile with `AmazonSSMManagedInstanceCore` (Session Manager, no
  SSH/key pair) and `CloudWatchAgentServerPolicy`, plus an inline policy for the results
  bucket (S3 get/put/list) and `cloudwatch:PutMetricData` scoped to namespace `K6`.
- A security group with **egress only** (the instance calls out to the target API and to
  AWS APIs) and **no ingress rules at all** -- SSM Session Manager is outbound-only, so
  nothing needs to reach the instance.
- An S3 bucket (`results_bucket_name`, default `dhiren9939-mint-loadgen-results`) used to
  stage k6 scripts before a run and to collect each run's `summary.json` after.
- `user_data.sh` installs, at boot: k6 (official apt repo), a **local Redis** bound to
  `127.0.0.1` only with persistence disabled (`save ""`) -- purely load-gen coordination
  state for `bench/k6/lib/pool.js`'s sender/receiver pool, never the application's own
  Valkey, disposable/wiped on restart -- and the CloudWatch agent (continuous host-level
  CPU/memory/network metrics, namespace `CWAgent`).

## k6 metrics -> CloudWatch: the approach and its limitation

Stock k6 (the official binary, no custom `xk6` build) has no first-class CloudWatch
output. The realistic stock-compatible options are (a) k6's experimental OpenTelemetry
output + the CloudWatch agent's OTLP receiver, or (b) write a local JSON summary and push
selected numbers to CloudWatch afterwards. **This implements (b)**:

- `k6 run --summary-export=summary.json main.js` writes the end-of-run summary locally.
- `run-scenario.sh` parses it with `jq` and calls `aws cloudwatch put-metric-data`
  (namespace `K6`, dimension `Arm=<arm_name>`) for download p95/p99, iteration rate,
  dropped iterations, and the rate_limited/server_errors/client_errors rates.

**Limitation, stated plainly**: this is a post-run, batch publish. The `K6` namespace
only gets data once a run finishes and `run-scenario.sh` executes its `put-metric-data`
calls -- there is no live dashboard widget updating during an in-progress run. Continuous
host metrics (this box's own CPU/network) come from the CloudWatch agent separately and
*are* live throughout a run; only the k6-specific test metrics have this gap.

## Results/scripts transfer mechanism

`bench-run.yml` (in `.github/workflows/`) syncs `bench/k6/` and `run-scenario.sh` to
`s3://<results_bucket>/scripts/` before every run, then uses `aws ssm send-command` to
have the instance `aws s3 sync` that prefix down to `/opt/loadgen` and execute
`run-scenario.sh`. After the run, `run-scenario.sh` uploads `summary.json` and a small
`run-metadata.json` to `s3://<results_bucket>/results/<arm_name>/<run_id>/`, which the
workflow then `aws s3 cp`s down and attaches via `actions/upload-artifact`.

## Wiring this into a bench arm

The EIP (`terraform output public_ip`) needs to go into that arm's
`infra/envs/<arm>.tfvars` `api_ingress_cidrs` -- that edit belongs to the per-arm stages
(Stage 4-7 in the master plan), not to this root, so it is intentionally not done here.

## Usage

```sh
cd bench/loadgen
terraform init -backend-config="key=projects/mint/loadgen.tfstate"
terraform apply
```

In CI, `.github/workflows/loadgen.yml` does this via the same `setup-aws-terraform`
composite action the main `infra/` root uses, passing `environment: loadgen` so the
action's own key-building logic (`projects/mint/$ENVIRONMENT.tfstate`) lands on exactly
`projects/mint/loadgen.tfstate` without duplicating it.
