# Mint

<p align="center">
  <img src="frontend/public/favicon.svg" alt="Mint Logo" width="120" />
</p>

A modern anonymous file and text sharing platform built with React, Spring Boot, PostgreSQL, Redis and AWS S3.

## Highlights

- **Anonymous sharing** without registration or user accounts.
- **Secure storage** using AWS S3 signed URLs.
- **Auto-expiring files** and link cleanup.
- **Rate limiting** on public endpoints via Bucket4j.
- **Modern frontend** built with React + Vite.

## Tech Stack

- **Frontend:** React 19, Vite, Tailwind CSS, TypeScript
- **Backend:** Java 21, Spring Boot 3.4.2
- **Persistence:** PostgreSQL
- **Cache / session:** Redis
- **Storage:** AWS S3

## Project Structure

```text
Mint/
  backend/    Spring Boot API, PostgreSQL metadata, AWS S3 storage
  frontend/   React app, upload/share UI, analytics
```

## Prerequisites

- Java 21 SDK
- Maven (or use `./backend/mvnw`)
- Node.js 20+ and npm
- PostgreSQL
- Redis
- AWS credentials configured for S3 access

## Running Locally

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Open the frontend at the Vite dev server URL shown in the terminal.

## Build

### Backend

```bash
cd backend
./mvnw clean package
```

### Frontend

```bash
cd frontend
npm run build
```

## Available Scripts

### Backend

- `./mvnw spring-boot:run` — start the backend in development mode
- `./mvnw clean package` — build the backend JAR

### Frontend

- `npm run dev` — start Vite dev server
- `npm run build` — build production assets
- `npm run preview` — preview production build
- `npm run lint` — run frontend lint checks

## Notes

- The repository currently does not include a formal open source license.
- The root project includes a production Docker Compose file at `backend/docker-compose.prod.yml`.

## Contributing

1. Fork the repo.
2. Create a branch for your feature or fix.
3. Make sure the backend and frontend build correctly.
4. Submit a PR with a clear summary and validation steps.
