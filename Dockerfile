FROM gradle:8.11.1-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle buildFatJar --no-daemon

FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app
COPY --from=builder /app/build/libs/shurl.jar ./shurl.jar
CMD ["./shurl.jar"]
