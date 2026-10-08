FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . /workspace

RUN if [ -f /workspace/services/user-service/pom.xml ]; then \
      cd /workspace/services/user-service && chmod +x mvnw && ./mvnw -q -DskipTests package && cp target/*.jar /workspace/app.jar; \
    else \
      chmod +x mvnw && ./mvnw -q -DskipTests package && cp target/*.jar /workspace/app.jar; \
    fi

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
