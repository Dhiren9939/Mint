#!/usr/bin/env bash
# Boots the load-gen instance: k6, a local coordination Redis, the CloudWatch agent, and
# awscli/jq for the run-scenario.sh wrapper. Rendered via Terraform templatefile() --
# ${results_bucket} below is a Terraform template variable, substituted at apply time.
set -euxo pipefail

RESULTS_BUCKET="${results_bucket}"

export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y gnupg2 curl unzip jq redis-server

# ---------- k6 (official apt repo) ----------
curl -fsSL https://dl.k6.io/key.gpg | gpg --dearmor -o /usr/share/keyrings/k6-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  > /etc/apt/sources.list.d/k6.list
apt-get update -y
apt-get install -y k6

# ---------- Local Redis: load-gen coordination state only, NOT the app's Valkey ----------
# Disposable: bound to loopback only, no persistence, wiped on every restart/run.
sed -i 's/^bind .*/bind 127.0.0.1 ::1/' /etc/redis/redis.conf
sed -i 's/^save .*/save ""/' /etc/redis/redis.conf
if ! grep -q '^save ""' /etc/redis/redis.conf; then
  echo 'save ""' >> /etc/redis/redis.conf
fi
systemctl enable redis-server
systemctl restart redis-server

# ---------- AWS CLI v2 (the distro's apt package can lag; install the official bundle) ----------
if ! command -v aws >/dev/null 2>&1; then
  curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip
  unzip -q /tmp/awscliv2.zip -d /tmp
  /tmp/aws/install
fi

# ---------- CloudWatch agent (host-level metrics: CPU/mem/net of this box, continuous) ----------
curl -fsSL https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb \
  -o /tmp/amazon-cloudwatch-agent.deb
dpkg -i -E /tmp/amazon-cloudwatch-agent.deb

mkdir -p /opt/aws/amazon-cloudwatch-agent/etc
cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json <<'CWCONFIG'
{
  "metrics": {
    "namespace": "CWAgent",
    "metrics_collected": {
      "cpu": {
        "measurement": ["cpu_usage_idle", "cpu_usage_user", "cpu_usage_system"],
        "totalcpu": true
      },
      "mem": {
        "measurement": ["mem_used_percent"]
      },
      "net": {
        "measurement": ["bytes_sent", "bytes_recv"],
        "resources": ["*"]
      }
    }
  }
}
CWCONFIG

/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config -m ec2 -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

# ---------- Load-gen working directory ----------
# bench-run.yml syncs bench/k6/ and run-scenario.sh into here at run time via
# `aws s3 sync s3://<results_bucket>/scripts/ /opt/loadgen` before invoking it.
mkdir -p /opt/loadgen
echo "$RESULTS_BUCKET" > /opt/loadgen/results-bucket.txt

echo "loadgen instance ready" > /opt/loadgen/READY
