#!/bin/bash

set -euo pipefail

sudo cp /tmp/docker-compose.prod.yml /tmp/application.properties /tmp/application-prod.properties /opt/mint-backend/config

sudo -E docker compose -f /opt/mint-backend/config/docker-compose.prod.yml up -d --pull

sudo docker image prune -f