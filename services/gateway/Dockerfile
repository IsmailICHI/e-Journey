FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY services/gateway/pom.xml ./pom.xml
COPY services/gateway/src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S -G app app
COPY --from=build --chown=app:app /workspace/target/gateway-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8089 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
