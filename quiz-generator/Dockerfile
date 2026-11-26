# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B -DskipTests package

# Run (Cloud Run injects PORT; application.yml reads it)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/quiz-generator-*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:TieredStopAtLevel=1"
ENTRYPOINT ["java","-jar","app.jar"]
