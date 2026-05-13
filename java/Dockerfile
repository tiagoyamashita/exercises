# syntax=docker/dockerfile:1
# Build runs tests so target/surefire-reports exists. Runtime image keeps a Maven workspace so the
# dashboard can discover tests, show Surefire XML, and run "mvn test" from the UI (dev compose).
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY src ./src
RUN chmod +x mvnw \
  && mvn -B -q package

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
