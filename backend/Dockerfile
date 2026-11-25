# Stage 1: Build JAR with Maven Wrapper
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw package -DskipTests -B

# Stage 2: Minimal non-root runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S riftradar && adduser -S riftradar -G riftradar
COPY --from=build /app/target/*.jar app.jar
RUN chown -R riftradar:riftradar /app

USER riftradar:riftradar
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=25s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
