FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q test package
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends git && rm -rf /var/lib/apt/lists/* && useradd --system --no-create-home forgeflow
COPY --from=build /app/target/forgeflow-api-0.1.0.jar /app.jar
ENTRYPOINT ["sh","-c","mkdir -p /runs /targets && chown forgeflow:forgeflow /runs /targets && exec runuser -u forgeflow -- java -jar /app.jar"]
