# Mint

<p align="center">
  <img src="frontend/public/favicon.svg" alt="Mint Logo" width="120" />
</p>

Anonymous file sharing. Upload a file, get a short code, it expires on its own.

This branch runs the whole thing on one t3.micro: the api serves the frontend, with postgres, redis and caddy next to it in docker compose. Files go to S3 with presigned urls.

Live at https://fs.dhiren.xyz

## Stack

- Frontend: React 19, Vite, Tailwind, TypeScript
- Backend: Java 21, Spring Boot 4, Postgres, Redis (lua rate limiter)
- Box: caddy for tls, certs backed up to S3 so a rebuilt box keeps them
- Infra: Terraform, VPC + EC2 + S3 + Route53

## Running locally

```bash
cd backend
docker compose -f docker-compose.dev.yml up -d   # postgres with the schema, redis
./mvnw spring-boot:run
```

```bash
cd frontend
cp .env-example .env
npm install
npm run dev
```

The dev profile uploads to the `fs.mint.bucket.test` bucket, so you need AWS credentials that can reach it.

## Deploying

Everything goes through GitHub Actions, in the `fast-deploy-single-ec2` environment.

- `create-app` builds the image and applies terraform at the same time, then deploys. A fresh box pulls the image on boot, so the app is up when the user data finishes.
- `destroy-app` tears it all down. Type the environment name to confirm.
- A push to this branch deploys on its own: `backend-cd` for backend or frontend changes, `infra-cd` for infra.

The image is built on the runner and pushed to ghcr. The box only pulls the layers that changed, then `docker compose up --wait` holds until the api healthcheck passes.

The environment needs:

| | |
|---|---|
| `INFRA_ROLE_ARN` | variable, the role terraform runs as |
| `APP_HOST` | variable, infra-cd sets it |
| `DB_USERNAME`, `DB_PASSWORD` | secrets for postgres on the box |
| `EC2_SSH_KEY` | secret, private key of the box's key pair |
| `GH_ACCESS_TOKEN` | secret, lets infra-cd set `APP_HOST` |

Postgres lives on the box's disk. A user data change doesn't replace the instance because of that, run `destroy-app` and `create-app` when you need a fresh box.

## Layout

```text
backend/    Spring Boot api, Dockerfile, compose files, config/ (Caddyfile, schema)
frontend/   React app, built into the api image
infra/      Terraform, modules/ec2/user_data.sh.tftpl sets up the box
.github/    workflows and the redeploy script that runs on the box
```
