FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY services/payment-service/.mvn .mvn
COPY services/payment-service/mvnw services/payment-service/pom.xml ./
COPY services/payment-service/src ./src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S -G app app
COPY --from=build --chown=app:app /workspace/target/*.jar app.jar
USER app
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
