FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the POM file
COPY pom.xml .
# Download all dependencies
RUN mvn dependency:go-offline -B

# Copy the project source
COPY src ./src
COPY config ./config

# Package the application
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
# Diğer komutlar...
RUN apt-get update && apt-get install -y netcat-openbsd && apt-get clean
# Copy the built artifact from the build stage
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/config ./config

# Create directory for logs
RUN mkdir -p /app/logs

# Set entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]