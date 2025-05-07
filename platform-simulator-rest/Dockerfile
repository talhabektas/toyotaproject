FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# POM dosyasını kopyala
COPY pom.xml .
# Bağımlılıkları indir
RUN mvn dependency:go-offline -B

# Proje kaynak kodunu kopyala
COPY src ./src

# Uygulamayı paketle
RUN mvn package -DskipTests

# Çalışma aşaması
FROM eclipse-temurin:17-jre
WORKDIR /app

# curl ve netcat için gerekli araçları yükle
RUN apt-get update && apt-get install -y curl netcat-openbsd && apt-get clean

# Build aşamasından JAR dosyasını kopyala
COPY --from=build /app/target/*.jar app.jar

# Konfigürasyon dosyalarını kopyala
COPY src/main/resources/application.properties /app/
COPY src/main/resources/rates-config.json /app/
COPY src/main/resources/log4j2.xml /app/config/

# Log dizini oluştur
RUN mkdir -p /app/logs

# Uygulamayı başlat
ENTRYPOINT ["java", "-Dlogging.config=/app/config/log4j2.xml", "-jar", "app.jar"]