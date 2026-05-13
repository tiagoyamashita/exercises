# syntax=docker/dockerfile:1
# Build runs tests by default (set SKIP_TESTS=true for faster image builds).
# Example: docker compose build --build-arg SKIP_TESTS=true java
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
ARG SKIP_TESTS=false
COPY pom.xml .
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN chmod +x mvnw \
  && mvn -B -q dependency:go-offline
COPY src ./src
RUN chmod +x mvnw \
  && if [ "$SKIP_TESTS" = "true" ]; then \
       mvn -B -q package -DskipTests; \
     else \
       mvn -B -q package; \
     fi

FROM maven:3.9.9-eclipse-temurin-17
WORKDIR /workspace
RUN mkdir -p /workspace/logs
COPY --from=build /build/pom.xml /build/mvnw /build/mvnw.cmd ./
COPY --from=build /build/.mvn ./.mvn
COPY --from=build /build/src ./src
COPY --from=build /build/target ./target
RUN chmod +x mvnw
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/workspace/target/demo-0.0.1-SNAPSHOT.jar", "--server.address=0.0.0.0"]
