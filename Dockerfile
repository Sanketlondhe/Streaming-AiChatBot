# ── Stage 1: Build ──────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom first — Docker caches this layer.
# Dependencies only re-download if pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Run ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Render injects PORT automatically (usually 10000).
# JVM tuned for Render free tier (512 MB RAM).
ENV PORT=8080
ENV JAVA_OPTS="-Xms128m -Xmx400m -XX:+UseContainerSupport"

EXPOSE ${PORT}

USER app

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
