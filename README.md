# Mint

<p align="center">
  <img src="frontend/public/favicon.svg" alt="Mint Logo" width="120" />
</p>

[![Status](https://img.shields.io/badge/status-active-success)](https://github.com/Dhiren9939/Mint)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)](https://github.com/Dhiren9939/Mint)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/downloads/)
[![Spring Boot](https://img.shields.io/badge/Spring--Boot-3.4.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/react-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-caching-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![AWS S3](https://img.shields.io/badge/AWS--S3-storage-FF9900?logo=amazons3&logoColor=white)](https://aws.amazon.com/s3/)
[![License](https://img.shields.io/badge/license-unlicensed-lightgrey)](https://github.com/Dhiren9939/Mint)

Mint is a modern, anonymous file and text sharing platform built with React, Spring Boot, and AWS S3.

`react` `vite` `springboot` `postgresql` `redis` `tailwindcss` `awss3` `bucket4j` `lucide-react`

## Core Features

- **Fully Anonymous:** No registration, no user profiles, no tracking.
- **Secure S3 Storage:** All files are stored securely with signed URL access.
- **Instant Sharing:** Upload any file or text and get a unique retrieval key.
- **Auto-Expiring Links:** Links and resources are automatically purged after expiration.
- **Modern UI:** Sleek, dark glassmorphism interface with smooth animations.

## Tech Stack

- **Frontend:** React 19, Vite, Tailwind CSS, React Router, Lucide React
- **Backend:** Java 21, Spring Boot, PostgreSQL, Redis, Bucket4j
- **Infrastructure:** AWS S3 for storage

## Project Structure

```text
Mint/
  backend/    Spring Boot API, storage integration, metadata persistence
  frontend/   React app, file/text upload UI, anonymous retrieval
```

## NPM Scripts & Tools

### Backend (`backend/`)

- `./mvnw spring-boot:run` - Run the application in development mode
- `./mvnw clean package` - Build the production-ready JAR

### Frontend (`frontend/package.json`)

- `npm run dev` - Run Vite dev server
- `npm run build` - Production build
- `npm run preview` - Preview production build
- `npm run lint` - Lint frontend source


## Current State

- No automated tests are wired yet.
- Rate limiting is active on all public endpoints via Bucket4j.
- Project is optimized for local/dev workflow right now.

## Contributing

1. Fork and clone the repository.
2. Create a specific feature branch.
3. Ensure the project builds successfully on both ends.
4. Provide clear reproduction or validation steps in your PR.

## License

No license file is currently defined in this repository.
