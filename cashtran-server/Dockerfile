# Stage 1: build
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /workspace

# Install git for SSH key access if needed for private repos
RUN apt-get update && apt-get install -y --no-install-recommends git && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src

# Build with optimizations
RUN mvn -e -B -DskipTests \
    -Dorg.slf4j.simpleLogger.defaultLogLevel=warn \
    package


# Stage 2: run
FROM eclipse-temurin:21-jre-alpine

# Install curl for healthchecks and wget for OpenTelemetry agent
RUN apk add --no-cache curl wget

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1000 appgroup && \
    adduser -D -u 1000 -G appgroup appuser && \
    chown -R appuser:appgroup /app

# Download OpenTelemetry Java Agent
RUN wget -O /app/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

USER appuser

COPY --from=build --chown=appuser:appgroup /workspace/target/*.jar app.jar

EXPOSE 8080

# Fixed healthcheck for Spring Boot
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]