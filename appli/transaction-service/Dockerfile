# Transaction Service Dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy all project files (parent pom needs all modules referenced)
COPY pom.xml ./pom.xml
COPY shared ./shared
COPY auth-service ./auth-service
COPY account-service ./account-service
COPY card-service ./card-service
COPY transaction-service ./transaction-service

# Install Maven
RUN apk add --no-cache maven

# Build only transaction-service (and its dependencies)
RUN mvn -pl transaction-service -am clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy JAR from builder
COPY --from=builder /app/transaction-service/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

EXPOSE 8084

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
