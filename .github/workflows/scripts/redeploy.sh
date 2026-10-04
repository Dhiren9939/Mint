#!/bin/bash
# runs on the box, backend-cd pipes it over ssh: redeploy.sh <image>
# the config files are already in /tmp/mint-cfg
set -euo pipefail

IMAGE=$1
CONFIG=/opt/mint-backend/config
COMPOSE="docker compose --env-file /opt/mint-backend/.env -f $CONFIG/docker-compose.prod.yml"

# a fresh box may still be running its user data, wait for it whatever the outcome.
# the deploy only needs docker and the env file, a failed first app start is what it fixes
echo "waiting for the box setup to finish"
sudo timeout 900 cloud-init status --wait >/dev/null || true
if ! command -v docker >/dev/null || ! sudo test -f /opt/mint-backend/.env; then
  echo "box setup didn't get far enough, tail of /var/log/user-data.log:"
  sudo tail -30 /var/log/user-data.log
  exit 1
fi

sudo cp /tmp/mint-cfg/* $CONFIG
rm -rf /tmp/mint-cfg

sudo sed -i "s|^IMAGE_TAG=.*|IMAGE_TAG=$IMAGE|" /opt/mint-backend/.env

# --wait returns once the api healthcheck passes
if ! sudo $COMPOSE up -d --wait --wait-timeout 180; then
  echo "api didn't get healthy"
  sudo $COMPOSE logs --tail 50 api
  exit 1
fi

# keep the disk free, only the running image stays
sudo docker images --filter 'reference=*mint-backend' --filter 'reference=*/*/mint-backend' --format '{{.Repository}}:{{.Tag}}' \
  | grep -vx "$IMAGE" | xargs -r sudo docker rmi >/dev/null 2>&1 || true
sudo docker image prune -f >/dev/null
echo "running $IMAGE"
