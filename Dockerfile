# ---- Build stage: compile and package with Maven (no local Maven needed) ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Runtime stage: small JRE image, non-root user ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring
COPY --from=build /app/target/billing-api-*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
