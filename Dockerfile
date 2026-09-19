# ==============================================================================
# Multi-Stage Dockerfile for AdServe Spring Boot Application
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Application with Maven and Temurin JDK 17
# ------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy pom.xml and download dependencies first to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source tree
COPY src ./src

# Package application JAR (skipping test suite execution during image build)
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal Production JRE Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install curl for Actuator health checks and clean up apt caches
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Security: Create non-root system user and group
RUN groupadd -r appgroup && useradd -r -g appgroup -u 1001 appuser

# Copy executable fat JAR from builder stage
COPY --from=builder /build/target/adserve-0.0.1-SNAPSHOT.jar app.jar

# Set ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose Spring Boot HTTP port
EXPOSE 8080

# Environment defaults (overrideable via Docker Compose / Railway / .env)
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Healthcheck using Spring Boot Actuator endpoint with dynamic port support
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Launch application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
