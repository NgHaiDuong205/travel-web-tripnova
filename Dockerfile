# syntax=docker/dockerfile:1
# Backend TripNova (Spring Boot, Java 21). Build: docker build -t tripnova-backend .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Cache ~/.m2 giữa các lần build; mạng chập chờn (tải dở 1 file) → thử lại, phần đã tải được giữ trong cache.
RUN --mount=type=cache,target=/root/.m2 \
    sed -i 's/\r$//' mvnw && chmod +x mvnw && \
    for i in 1 2 3 4 5; do ./mvnw -q -B dependency:go-offline && break; [ $i = 5 ] && exit 1; sleep 5; done
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    for i in 1 2 3; do ./mvnw -q -B -DskipTests package && break; [ $i = 3 ] && exit 1; sleep 5; done && \
    cp target/*.jar /app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 tripnova && mkdir -p /app/uploads && chown -R tripnova /app
WORKDIR /app
COPY --from=build /app.jar app.jar
USER tripnova
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
