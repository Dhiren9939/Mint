#!/usr/bin/env bash
# Runs bench/k6/main.js (the sender/receiver load) on the load-gen instance and reports
# the results. Invoked via `aws ssm send-command` from .github/workflows/bench-run.yml,
# after that workflow has already synced bench/k6/ and this script to
# s3://$RESULTS_BUCKET/scripts/.
#
# Usage: run-scenario.sh <sender_vus> <receiver_vus> <duration_minutes> <base_url> <arm_name> <results_bucket>
set -euo pipefail

SENDER_VUS="$1"
RECEIVER_VUS="$2"
DURATION_MINUTES="$3"
BASE_URL="$4"
ARM_NAME="$5"
RESULTS_BUCKET="$6"

WORKDIR=/opt/loadgen
RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)"
RESULT_PREFIX="results/${ARM_NAME}/${RUN_ID}"

echo "Syncing k6 scripts from s3://${RESULTS_BUCKET}/scripts/ ..."
aws s3 sync "s3://${RESULTS_BUCKET}/scripts/" "$WORKDIR" --delete

cd "$WORKDIR/k6"

SUMMARY_FILE="$WORKDIR/summary-${RUN_ID}.json"

echo "Running main.js against ${BASE_URL} (sender_vus=${SENDER_VUS}, receiver_vus=${RECEIVER_VUS}, duration_minutes=${DURATION_MINUTES}) ..."

# Never fail this script purely because k6 reported a threshold breach -- breaching at a
# high VU count is the expected way to find the knee. Only a genuine script/setup error
# (no summary file produced at all) is treated as a real failure below.
set +e
k6 run \
  -e BASE_URL="$BASE_URL" \
  -e SENDER_VUS="$SENDER_VUS" \
  -e RECEIVER_VUS="$RECEIVER_VUS" \
  -e DURATION_MINUTES="$DURATION_MINUTES" \
  --summary-export="$SUMMARY_FILE" \
  main.js
K6_EXIT_CODE=$?
set -e
echo "k6 exited with code ${K6_EXIT_CODE} (non-zero here is usually a threshold breach, not a script error -- checked separately below)"

if [ ! -s "$SUMMARY_FILE" ]; then
  echo "ERROR: k6 did not produce a summary file -- treating this as a real failure (script/setup error, not a threshold breach)" >&2
  exit 1
fi

# ---------- Publish selected metrics to CloudWatch namespace K6 (post-run, not live) ----------
# This is the stock-k6-compatible fallback: k6's built-in OpenTelemetry/statsd outputs
# either aren't in core k6 or need a custom xk6 build, so metrics land here only once the
# run finishes and this script executes -- there's no live dashboard during an
# in-progress run. Continuous host metrics (CPU/network of this box) come from the
# CloudWatch agent separately and remain live throughout.
publish() {
  local metric="$1" value="$2" unit="$3"
  aws cloudwatch put-metric-data \
    --namespace "K6" \
    --metric-name "$metric" \
    --dimensions "Arm=${ARM_NAME}" \
    --value "$value" \
    --unit "$unit" \
    --timestamp "$(date -u +%Y-%m-%dT%H:%M:%SZ)" || echo "put-metric-data failed for ${metric}, continuing"
}

DOWNLOAD_P95=$(jq -r '.metrics["http_req_duration{step:download}"].values["p(95)"] // .metrics.http_req_duration.values["p(95)"] // 0' "$SUMMARY_FILE")
DOWNLOAD_P99=$(jq -r '.metrics["http_req_duration{step:download}"].values["p(99)"] // .metrics.http_req_duration.values["p(99)"] // 0' "$SUMMARY_FILE")
ITER_RATE=$(jq -r '.metrics.iterations.values.rate // 0' "$SUMMARY_FILE")
DROPPED=$(jq -r '.metrics.dropped_iterations.values.count // 0' "$SUMMARY_FILE")
RATE_LIMITED=$(jq -r '.metrics.rate_limited.values.rate // 0' "$SUMMARY_FILE")
SERVER_ERRORS=$(jq -r '.metrics.server_errors.values.rate // 0' "$SUMMARY_FILE")
CLIENT_ERRORS=$(jq -r '.metrics.client_errors.values.rate // 0' "$SUMMARY_FILE")

publish "download_p95" "$DOWNLOAD_P95" "Milliseconds"
publish "download_p99" "$DOWNLOAD_P99" "Milliseconds"
publish "iteration_rate" "$ITER_RATE" "Count/Second"
publish "dropped_iterations" "$DROPPED" "Count"
publish "rate_limited_rate" "$RATE_LIMITED" "Percent"
publish "server_errors_rate" "$SERVER_ERRORS" "Percent"
publish "client_errors_rate" "$CLIENT_ERRORS" "Percent"

# ---------- Upload the summary + a small run-metadata file ----------
cat > "$WORKDIR/run-metadata-${RUN_ID}.json" <<EOF
{
  "arm": "${ARM_NAME}",
  "baseUrl": "${BASE_URL}",
  "senderVus": ${SENDER_VUS},
  "receiverVus": ${RECEIVER_VUS},
  "durationMinutes": ${DURATION_MINUTES},
  "runId": "${RUN_ID}",
  "k6ExitCode": ${K6_EXIT_CODE}
}
EOF

aws s3 cp "$SUMMARY_FILE" "s3://${RESULTS_BUCKET}/${RESULT_PREFIX}/summary.json"
aws s3 cp "$WORKDIR/run-metadata-${RUN_ID}.json" "s3://${RESULTS_BUCKET}/${RESULT_PREFIX}/run-metadata.json"

echo "RESULT_S3_PREFIX=${RESULT_PREFIX}"
echo "Done."
