# Backend TripNova (Spring Boot, Java 21). Build: docker build -t tripnova-backend .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package && cp target/*.jar /app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 tripnova && mkdir -p /app/uploads && chown -R tripnova /app
WORKDIR /app
COPY --from=build /app.jar app.jar
USER tripnova
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
