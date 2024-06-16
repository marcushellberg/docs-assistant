# First stage: JDK with GraalVM
FROM ghcr.io/graalvm/native-image-community:21 AS build


WORKDIR /usr/src/app

COPY . .

RUN ./mvnw -Pnative -Pproduction native:compile

# Second stage: Lightweight debian-slim image
FROM debian:bookworm-slim

WORKDIR /app

# Copy the native binary from the build stage
COPY --from=build /usr/src/app/target/vaadin-docs-assistant /app/vaadin-docs-assistant

# Run the application
CMD ["/app/vaadin-docs-assistant"]