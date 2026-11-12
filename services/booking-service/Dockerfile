FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY services/booking-service/pom.xml ./
COPY services/booking-service/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S -G app app
COPY --from=build --chown=app:app /workspace/target/*.jar app.jar
USER app
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
