# =============================================================================
# Build Stage: Compile and Package Spring Boot Application
# =============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependency layer for faster iterative builds
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# =============================================================================
# Runtime Stage: Minimal Hardened Alpine Container (Non-root user)
# =============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: Create non-privileged service user and ensure directory ownership
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app /app/upload-dir /tmp \
    && chown -R appuser:appgroup /app /tmp

# Copy built JAR from builder stage with strict ownership
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser

# Default port (overridden by cloud providers like Render, Railway, Cloud Run via $PORT)
ENV PORT=8080
EXPOSE 8080

# Dynamic port binding: uses $PORT injected by cloud provider or falls back to 8080
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
