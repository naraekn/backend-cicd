FROM openjdk:16.0.2 AS builder

COPY . .

RUN ["./gradlew", "assemble"]

FROM openjdk:16.0.2

COPY --from=builder /app/build/libs/app.jar .

ENTRYPOINT ["java", "-jar", "app.jar"]