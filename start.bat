@echo off
setlocal enabledelayedexpansion

echo ==========================================================
echo   Finansal Veri Toplama ve Hesaplama Sistemi
echo ==========================================================
echo.

echo Gerekli dizinler oluşturuluyor...
mkdir logs 2>nul
mkdir filebeat 2>nul

echo Filebeat yapılandırması oluşturuluyor...
(
echo filebeat.inputs:
echo   - type: log
echo     enabled: true
echo     paths:
echo       - /logs/main-app/*.log
echo       - /logs/tcp-simulator/*.log
echo       - /logs/rest-simulator/*.log
echo       - /logs/kafka-consumer/*.log
echo       - /logs/kafka-consumer-opensearch/*.log
echo     fields:
echo       application: finansal_rates
echo     fields_under_root: true
echo     multiline:
echo       pattern: '^\d{4}-\d{2}-\d{2}'
echo       negate: true
echo       match: after
echo.
echo processors:
echo   - add_host_metadata: ~
echo   - add_docker_metadata: ~
echo.
echo output.opensearch:
echo   hosts: ["opensearch:9200"]
echo   protocol: "http"
echo   username: ""
echo   password: ""
echo   index: "finansal-rates-logs-%%{+yyyy.MM.dd}"
echo.
echo logging.level: info
) > filebeat\filebeat.yml

echo Sistem durumu kontrol ediliyor...

docker-compose ps >nul 2>&1
if %errorlevel% neq 0 (
  echo Docker Compose çalışmıyor veya Docker başlatılmamış olabilir!
  echo Lütfen Docker'ın çalıştığından emin olun.
  exit /b
)

echo Eski konteynerler kapatılıyor...
docker-compose down

echo Sistem başlatılıyor...

echo Altyapı bileşenleri başlatılıyor...
docker-compose up -d zookeeper kafka postgres redis opensearch
timeout /t 10 /nobreak

echo Sistemin çalıştığından emin olunuyor...
set MAX_RETRIES=5
set RETRY_COUNT=0

:retry_kafka
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:29092 >nul 2>&1
if %errorlevel% neq 0 (
  set /a RETRY_COUNT+=1
  if !RETRY_COUNT! equ %MAX_RETRIES% (
    echo Kafka hazır değil. Lütfen manuel olarak kontrol edin.
    goto continue_setup
  )
  echo Kafka hazır değil. !RETRY_COUNT!/%MAX_RETRIES% deneme...
  timeout /t 10 /nobreak
  goto retry_kafka
)

:continue_setup
echo Kafka topic oluşturuluyor: finansal.rates
docker exec -it kafka kafka-topics.sh --create --topic finansal.rates --partitions 1 --replication-factor 1 --bootstrap-server localhost:29092 >nul 2>&1

echo Platform simülatörleri başlatılıyor...
docker-compose up -d tcp-simulator rest-simulator
echo Platform simülatörleri başlatıldı.
timeout /t 10 /nobreak

echo Ana uygulama başlatılıyor...
docker-compose up -d main-app
echo Ana uygulama başlatıldı.
timeout /t 15 /nobreak

echo Kafka tüketicileri başlatılıyor...
docker-compose up -d kafka-consumer kafka-consumer-opensearch
echo Kafka tüketicileri başlatıldı.
timeout /t 5 /nobreak

echo Filebeat başlatılıyor...
docker-compose up -d filebeat
echo Filebeat başlatıldı.

echo Sistem durumu kontrol ediliyor...
docker-compose ps

echo ==========================================================
echo   Sistem başarıyla başlatıldı!
echo ==========================================================
echo.
echo REST Platform Simulator: http://localhost:8080/api/rates
echo Ana Uygulama: http://localhost:8090
echo OpenSearch: http://localhost:9200
echo PostgreSQL: localhost:5432
echo Kafka: localhost:9092
echo.
echo Log klasörleri konteynerlar içinde oluşturuldu.
echo Veritabanı kontrolü: docker exec -it postgres psql -U postgres -d finansal_rates -c "SELECT * FROM tbl_rates LIMIT 10;"
echo Redis kontrolü: docker exec -it redis redis-cli KEYS "rate:*"
echo.

endlocal