# CardDemo Auth Service - Multi-stage Docker Build

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy parent pom and shared module first (for better caching)
COPY ../pom.xml ./pom.xml
COPY ../shared ./shared

# Copy auth-service
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY auth-service/src ./auth-service/src

# Build
RUN mvn -f auth-service/pom.xml clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S carddemo && adduser -S carddemo -G carddemo

# Copy JAR from build stage
COPY --from=build /app/auth-service/target/auth-service-1.0.0.jar app.jar

# Set ownership
RUN chown -R carddemo:carddemo /app

# Switch to non-root user
USER carddemo

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
