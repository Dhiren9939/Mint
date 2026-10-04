#!/bin/bash
# runs on the box, the deploy workflow pipes it over ssh: redeploy.sh <image> <commit sha>
set -euo pipefail

IMAGE=$1
SHA=$2
SRC=/opt/src/Mint
CONFIG=/opt/mint-backend/config
# the very first deploy can land while user data is still setting the box up
for i in $(seq 1 120); do
  [ -f /var/log/mint-ready ] && break
  sleep 10
done
[ -f /var/log/mint-ready ] || { echo "box setup didn't finish"; exit 1; }

COMPOSE="docker compose --env-file /opt/mint-backend/.env -f $CONFIG/docker-compose.prod.yml"

# config, compose file and schema come from the same commit as the image
sudo git -C $SRC fetch --depth 1 origin "$SHA"
sudo git -C $SRC checkout -q --force "$SHA"
sudo cp $SRC/backend/config/*.properties $SRC/backend/config/Caddyfile $SRC/backend/docker-compose.prod.yml $CONFIG
sudo cp $SRC/.github/workflows/scripts/schema.sql $CONFIG

sudo sed -i "s|^IMAGE_TAG=.*|IMAGE_TAG=$IMAGE|" /opt/mint-backend/.env
sudo $COMPOSE up -d

# a file code that doesn't exist should come back as a 404
code=""
for i in $(seq 1 40); do
  code=$(sudo $COMPOSE exec -T api wget -S -O /dev/null http://localhost:8080/api/v1/file/aaaaaa 2>&1 \
    | grep -o 'HTTP/1.1 [0-9]*' | cut -d' ' -f2 || true)
  [ "$code" = 404 ] && break
  sleep 3
done
if [ "$code" != 404 ]; then
  echo "api didn't come up, last status '$code'"
  sudo $COMPOSE logs --tail 50 api
  exit 1
fi

# keep the disk free, only the running image stays
sudo docker images --filter 'reference=*mint-backend' --format '{{.Repository}}:{{.Tag}}' | grep -vx "$IMAGE" | xargs -r sudo docker rmi || true
sudo docker image prune -f
