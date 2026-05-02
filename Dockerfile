# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle* build.gradle* ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon || true
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 spring
COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN chown -R spring:spring /app
USER spring
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
