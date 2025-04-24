# Finansal Veri Sağlayıcılardan Anlık Veri Toplama ve Hesaplama Projesi

Bu proje, çeşitli finansal veri sağlayıcılarından anlık döviz kuru verilerini toplayarak, bunları entegre eden ve hesaplamalar yapan bir sistemdir. Sistem, farklı protokoller üzerinden veri sağlayan platformlardan bilgi alabilmekte ve bu veriler üzerinden türev kurlar hesaplayabilmektedir.

## Proje Mimarisi

Proje aşağıdaki bileşenlerden oluşmaktadır:

1. **Veri Platformu Simülatörleri:**
   - TCP üzerinden veri sağlayan simülatör
   - REST API üzerinden veri sağlayan simülatör

2. **Ana Uygulama:**
   - Farklı platformlardan veri toplayan ve koordine eden merkezi uygulama
   - Kur hesaplamaları yapan modül
   - Kafka'ya veri gönderen producer

3. **Kafka Tüketicileri:**
   - Veriyi PostgreSQL veritabanına kaydeden consumer
   - Veriyi OpenSearch'e aktaran consumer

4. **Destek Servisleri:**
   - Zookeeper
   - Kafka
   - Redis (Önbellekleme)
   - PostgreSQL (Veri depolama)
   - OpenSearch (Log ve arama işlevleri)

## Sistem Gereksinimleri

- Docker ve Docker Compose
- Java 17 (Geliştirme için)
- Maven (Geliştirme için)
- 4GB RAM (minimum)
- 10GB disk alanı

## Kurulum ve Çalıştırma

### Otomatik Kurulum ve Başlatma

Sağlanan startup.sh betiği ile sistemi kolayca başlatabilirsiniz:

```bash
# Betik dosyasını çalıştırılabilir yapın
chmod +x startup.sh

# Sistemi başlatın
./startup.sh
```

### Manuel Kurulum

Eğer manuel olarak kurmak isterseniz, aşağıdaki adımları takip edin:

1. Projeyi klonlayın veya indirin.

2. Temel servisleri başlatın:
   ```bash
   docker-compose up -d zookeeper kafka redis postgres opensearch
   ```

3. Kafka topic'ini oluşturun (Kafka hazır olduktan sonra):
   ```bash
   docker exec kafka kafka-topics.sh --create --topic finansal.rates --partitions 1 --replication-factor 1 --bootstrap-server localhost:29092
   ```

4. Simülatörleri başlatın:
   ```bash
   docker-compose up -d tcp-simulator rest-simulator
   ```

5. Ana uygulamayı başlatın:
   ```bash
   docker-compose up -d main-app
   ```

6. Kafka tüketicilerini başlatın:
   ```bash
   docker-compose up -d kafka-consumer kafka-consumer-opensearch
   ```

## Servis Bileşenleri ve Portlar

- **TCP Simulator**: `http://localhost:8081`
- **REST Simulator**: `http://localhost:8080/api/rates`
- **Main App**: `http://localhost:8090`
- **Kafka**: `localhost:9092` (harici), `kafka:29092` (iç ağ)
- **OpenSearch**: `http://localhost:9200`
- **PostgreSQL**: `localhost:5432`
- **Redis**: `localhost:6379`

## Mimari Akış

1. Simülatörler gerçekçi döviz kuru verilerini üretir ve yayınlar.
2. Ana uygulama, her iki simülatöre bağlanır ve kur verilerini toplar.
3. Ana uygulama, topladığı ham verileri önbelleğe (Redis veya yerel bellek) alır.
4. Ana uygulama, ham verileri kullanarak türev kurlar hesaplar (örn. EUR/TRY, GBP/TRY).
5. Ham ve hesaplanmış kur verileri Kafka'ya gönderilir.
6. Kafka tüketiciler, gelen verileri PostgreSQL'e ve OpenSearch'e kaydeder.

## Veri Hesaplama Metodolojisi

Proje kapsamında aşağıdaki hesaplamalar yapılmaktadır:

### USD/TRY Hesabı
- İki platformdan gelen USD/TRY kurlarının ortalaması alınır.
- `USDTRY.bid = (PF1_USDTRY.bid + PF2_USDTRY.bid) / 2`
- `USDTRY.ask = (PF1_USDTRY.ask + PF2_USDTRY.ask) / 2`

### EUR/TRY Hesabı
- EUR/USD kurları ve USD/TRY mid-price değeri kullanılarak hesaplanır.
- `usdmid = ((PF1_USDTRY.bid + PF2_USDTRY.bid) / 2 + (PF1_USDTRY.ask + PF2_USDTRY.ask) / 2) / 2`
- `EURTRY.bid = usdmid × ((PF1_EURUSD.bid + PF2_EURUSD.bid) / 2)`
- `EURTRY.ask = usdmid × ((PF1_EURUSD.ask + PF2_EURUSD.ask) / 2)`

### GBP/TRY Hesabı
- GBP/USD kurları ve USD/TRY mid-price değeri kullanılarak hesaplanır.
- `usdmid = ((PF1_USDTRY.bid + PF2_USDTRY.bid) / 2 + (PF1_USDTRY.ask + PF2_USDTRY.ask) / 2) / 2`
- `GBPTRY.bid = usdmid × ((PF1_GBPUSD.bid + PF2_GBPUSD.bid) / 2)`
- `GBPTRY.ask = usdmid × ((PF1_GBPUSD.ask + PF2_GBPUSD.ask) / 2)`

## Sistemi Kontrol Etme

Sistemin doğru çalıştığını kontrol etmek için aşağıdaki adımları izleyebilirsiniz:

1. Servislerin çalıştığını kontrol edin:
   ```bash
   docker-compose ps
   ```

2. Simülatör loglarını kontrol edin:
   ```bash
   docker-compose logs tcp-simulator
   docker-compose logs rest-simulator
   ```

3. Ana uygulama loglarını kontrol edin:
   ```bash
   docker-compose logs main-app
   ```

4. PostgreSQL'deki verileri kontrol edin:
   ```bash
   docker exec -it postgres psql -U postgres -d finansal_rates -c "SELECT * FROM tbl_rates LIMIT 10;"
   ```

5. OpenSearch'e kaydedilen verileri kontrol edin:
   ```bash
   curl -X GET "localhost:9200/rates-*/_search?pretty" -H 'Content-Type: application/json' -d'
   {
     "size": 10,
     "sort": [
       { "timestamp": { "order": "desc" } }
     ]
   }
   '
   ```

## Sorun Giderme

Herhangi bir sorunla karşılaşırsanız, `troubleshooting.md` dosyasına başvurun veya servis loglarını kontrol edin:

```bash
docker-compose logs -f <servis-adı>
```

Sistemi tamamen sıfırlamak için:

```bash
docker-compose down
docker volume rm $(docker volume ls -q | grep -E 'finansal|zookeeper|kafka|redis|postgres|opensearch')
./startup.sh
```

## Geliştirme

Projeyi geliştirmek için:

1. Maven ile projeyi derleyin:
   ```bash
   mvn clean package
   ```

2. Değişikliklerinizden sonra ilgili servisleri yeniden oluşturun:
   ```bash
   docker-compose build <servis-adı>
   docker-compose up -d <servis-adı>
   ```

## Notlar

- TCP Simulator, telnet ile test edilebilir: `telnet localhost 8081`
- REST Simulator, HTTP istekleri ile test edilebilir: `curl http://localhost:8080/api/rates`
- Kafka konsoldan izlenebilir: `docker exec -it kafka kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic finansal.rates --from-beginning`
