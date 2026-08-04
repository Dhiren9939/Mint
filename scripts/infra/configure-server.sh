#!/bin/bash

set -euo pipefail

# Add Docker's official GPG key:
sudo apt update
sudo apt install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add the repository to Apt sources:
sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/debian
Suites: $(. /etc/os-release && echo "$VERSION_CODENAME")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update

sudo apt install -y postgresql-client

sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin -y

sudo systemctl enable --now docker

sudo systemctl status docker

sudo adduser springuser

sudo groupadd apprunners

sudo usermod -aG apprunners springuser

sudo chown -R :apprunners /opt/mint-backend

sudo chmod -R 750 /opt/mint-backend

sudo curl -o /opt/mint-backend/config/global-bundle.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
