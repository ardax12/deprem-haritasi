# Multi-stage build for Türkiye Deprem İzleme Haritası

# Stage 1: Build React Frontend
FROM node:22-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# Stage 2: Build Java application
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy gradle wrapper and definitions
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

# Pre-fetch dependencies for Docker cache optimization
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Copy compiled frontend from Stage 1 into Spring Boot static resources
COPY --from=frontend-builder /app/src/main/resources/static/ src/main/resources/static/

# Build executable Spring Boot jar
RUN ./gradlew bootJar --no-daemon -x test

# Stage 3: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Create a non-root system user and archive directory with proper permissions
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/archive && chown -R appuser:appgroup /app/archive
USER appuser

# Copy jar from builder
COPY --from=builder /app/build/libs/deprem-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/api/v1/status || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
