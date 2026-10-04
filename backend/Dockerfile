# build from the repo root: docker build -f backend/Dockerfile -t mint-backend .
FROM node:22-alpine AS frontend

WORKDIR /frontend

COPY frontend/package*.json ./

RUN npm ci

COPY frontend/ .

# same origin as the api, so the paths in the code are enough
ENV VITE_API_URL=

RUN npm run build

FROM maven:3.9.14-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY backend/pom.xml .

RUN mvn dependency:go-offline

COPY backend/src ./src

COPY --from=frontend /frontend/dist ./src/main/resources/static

RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S app && adduser -S -G app app

RUN mkdir -p /opt/mint-backend/config

RUN mkdir -p /app

RUN chown -R app:app /app /opt/mint-backend

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

USER app

ENTRYPOINT ["java","-jar","app.jar"]