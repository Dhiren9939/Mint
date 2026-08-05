#!/bin/bash

set -euo pipefail

sudo cp /tmp/docker-compose.prod.yml /tmp/application.properties /tmp/application-prod.properties /opt/mint-backend/config

sudo docker compose -f /opt/mint-backend/config/docker-compose.prod.yml down

sudo docker compose -f /opt/mint-backend/config/docker-compose.prod.yml up -d --pull always --remove-orphans

yes | sudo docker image prune