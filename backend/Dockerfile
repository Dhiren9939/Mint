# build from the repo root: docker build -f backend/Dockerfile -t mint-backend .
FROM node:22-alpine AS frontend

WORKDIR /frontend

COPY frontend/package*.json ./

RUN npm ci --no-audit --no-fund

COPY frontend/ .

# same origin as the api, so the paths in the code are enough
ENV VITE_API_URL=

RUN npm run build

FROM maven:3.9.14-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY backend/pom.xml .

RUN mvn -B -ntp dependency:go-offline

COPY backend/src ./src

COPY --from=frontend /frontend/dist ./src/main/resources/static

# split the jar into layers, a code change then only ships the small application layer
RUN mvn -B -ntp package -DskipTests && cp target/*.jar app.jar     && java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S app && adduser -S -G app app     && mkdir -p /opt/mint-backend/config /app     && chown -R app:app /app /opt/mint-backend

WORKDIR /app

COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./

EXPOSE 8080

USER app

ENTRYPOINT ["java","-jar","app.jar"]
