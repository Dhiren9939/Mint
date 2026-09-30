#!/usr/bin/env bash
# Local replacement for .github/workflows/bench-run.yml -- runs bench/k6/main.js against a
# live arm via the load-gen instance, no GitHub Actions involved. The instance itself is
# already fully set up by user_data.sh at boot (k6, local coordination Redis, CloudWatch
# agent) -- this script only stages the k6 scripts and triggers a run over SSM, then
# downloads the results, exactly like bench-run.yml did.
#
# Prerequisites: `terraform apply` in bench/loadgen has already run (this script reads its
# outputs), and you have AWS credentials in your local environment/profile with SSM +
# the results bucket's S3 permissions.
#
# Usage:
#   ./bench/loadgen/run-local.sh <target_url> <sender_vus> <receiver_vus> <duration_minutes> <arm_name>
# Example:
#   ./bench/loadgen/run-local.sh http://1.2.3.4 5 20 5 bench-sql
set -euo pipefail

TARGET_URL="${1:?usage: run-local.sh <target_url> <sender_vus> <receiver_vus> <duration_minutes> <arm_name>}"
SENDER_VUS="${2:?}"
RECEIVER_VUS="${3:?}"
DURATION_MINUTES="${4:?}"
ARM_NAME="${5:?}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TF_DIR="$REPO_ROOT/bench/loadgen"

INSTANCE_ID="$(terraform -chdir="$TF_DIR" output -raw instance_id)"
RESULTS_BUCKET="$(terraform -chdir="$TF_DIR" output -raw results_bucket_name)"
if [ -z "$INSTANCE_ID" ] || [ -z "$RESULTS_BUCKET" ]; then
  echo "Could not read instance_id/results_bucket_name from terraform output -- is bench/loadgen applied?" >&2
  exit 1
fi

echo "==> Staging k6 scripts to s3://$RESULTS_BUCKET/scripts/"
aws s3 sync "$REPO_ROOT/bench/k6" "s3://$RESULTS_BUCKET/scripts/k6" --delete
aws s3 cp "$REPO_ROOT/bench/loadgen/run-scenario.sh" "s3://$RESULTS_BUCKET/scripts/run-scenario.sh"

RUN_SCRIPT="chmod +x /opt/loadgen/run-scenario.sh && /opt/loadgen/run-scenario.sh \
  '$SENDER_VUS' '$RECEIVER_VUS' '$DURATION_MINUTES' '$TARGET_URL' '$ARM_NAME' '$RESULTS_BUCKET'"

echo "==> Sending run-scenario.sh via SSM (sender_vus=$SENDER_VUS receiver_vus=$RECEIVER_VUS duration=${DURATION_MINUTES}m target=$TARGET_URL)"
COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --parameters "{\"commands\":[\"$RUN_SCRIPT\"]}" \
  --timeout-seconds 3600 \
  --query "Command.CommandId" --output text)
echo "SSM command id: $COMMAND_ID"

MAX_WAIT_SECONDS=$(( (DURATION_MINUTES * 60) + 45 + 300 ))
ELAPSED=0
STATUS="InProgress"
while [ "$STATUS" = "InProgress" ] || [ "$STATUS" = "Pending" ]; do
  if [ "$ELAPSED" -ge "$MAX_WAIT_SECONDS" ]; then
    echo "Timed out waiting for the SSM command after ${ELAPSED}s" >&2
    exit 1
  fi
  sleep 15
  ELAPSED=$((ELAPSED + 15))
  STATUS=$(aws ssm get-command-invocation \
    --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
    --query "Status" --output text 2>/dev/null || echo "Pending")
  echo "SSM command status: $STATUS (${ELAPSED}s elapsed)"
done

if [ "$STATUS" != "Success" ]; then
  echo "run-scenario.sh failed on the load-gen instance (status=$STATUS) -- a real script/setup error, not a k6 threshold breach (those are absorbed inside run-scenario.sh)" >&2
  aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
    --query "StandardErrorContent" --output text || true
  exit 1
fi

OUTPUT=$(aws ssm get-command-invocation \
  --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
  --query "StandardOutputContent" --output text)
echo "$OUTPUT"

RESULT_PREFIX=$(echo "$OUTPUT" | grep -oP '(?<=RESULT_S3_PREFIX=).*' | tail -1)
if [ -z "$RESULT_PREFIX" ]; then
  echo "Could not find RESULT_S3_PREFIX in the command output" >&2
  exit 1
fi

OUT_DIR="$REPO_ROOT/bench/results/$ARM_NAME/$(basename "$RESULT_PREFIX")"
mkdir -p "$OUT_DIR"
aws s3 cp "s3://$RESULTS_BUCKET/$RESULT_PREFIX/summary.json" "$OUT_DIR/summary.json"
aws s3 cp "s3://$RESULTS_BUCKET/$RESULT_PREFIX/run-metadata.json" "$OUT_DIR/run-metadata.json"

echo "==> Done. Results in $OUT_DIR"
