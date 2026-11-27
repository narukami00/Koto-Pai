# ==============================================================================
# Multi-Stage Production Dockerfile for Koto-Pai
# Runtime: Eclipse Temurin 21 JRE Alpine (Hardened, Layered, Non-Root)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Application Artifact with Maven
# ------------------------------------------------------------------------------
# Maven ships inside this image. The Maven wrapper (./mvnw) downloads Maven from Maven Central at build time,
# and shared build hosts such as Render get "429 Too Many Requests" there, which failed a deployment.
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy the POM first so dependency downloads are cached. Retry because Maven Central can rate-limit shared hosts.
COPY pom.xml ./
RUN for i in 1 2 3 4 5; do \
      mvn -B -q dependency:go-offline && exit 0; \
      echo "Dependency download failed (attempt $i), retrying..."; sleep $((i * 15)); \
    done; exit 1

# Copy application source code and compile package
COPY src src
RUN for i in 1 2 3; do \
      mvn -B -q clean package -DskipTests=true && exit 0; \
      echo "Build failed (attempt $i), retrying..."; sleep $((i * 15)); \
    done; exit 1

# ------------------------------------------------------------------------------
# Stage 2: Extract Spring Boot Layered JAR
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /workspace
COPY --from=builder /workspace/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher

# ------------------------------------------------------------------------------
# Stage 3: Minimal, Hardened Runtime Container
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Install curl for container healthcheck
RUN apk add --no-cache curl

# Create unprivileged application user and group
RUN addgroup -S kotopai && adduser -S kotopai -G kotopai

# Copy Spring Boot layers in order of change frequency for optimal Docker layer caching
COPY --from=extractor --chown=kotopai:kotopai /workspace/app/dependencies/ ./
COPY --from=extractor --chown=kotopai:kotopai /workspace/app/spring-boot-loader/ ./
COPY --from=extractor --chown=kotopai:kotopai /workspace/app/snapshot-dependencies/ ./
COPY --from=extractor --chown=kotopai:kotopai /workspace/app/application/ ./

USER kotopai:kotopai

EXPOSE 8080

# Configure JVM ergonomics for container environments (75% RAM limit, G1 GC)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD sh -c 'curl -fsS "http://localhost:${PORT:-8080}/actuator/health" | grep -q "\"status\":\"UP\"" || exit 1'

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

