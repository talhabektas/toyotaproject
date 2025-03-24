package com.example.kafkaconsumeropensearch.service;

import com.example.kafkaconsumeropensearch.model.RateData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenSearch ile etkileşim için servis sınıfı
 */
@Service
public class OpenSearchService {

    private static final Logger logger = LogManager.getLogger(OpenSearchService.class);

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    @Value("${opensearch.index-prefix:rates}")
    private String indexPrefix;

    /**
     * Constructor
     * @param client OpenSearch client
     * @param objectMapper Jackson ObjectMapper
     */
    @Autowired
    public OpenSearchService(RestHighLevelClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    /**
     * İlklendirme - indeksleri oluştur
     */
    @PostConstruct
    public void init() {
        // Günlük indeks adını oluştur
        String indexName = getIndexName();

        try {
            // İndeks var mı kontrol et
            boolean exists = client.indices().exists(
                    new GetIndexRequest(indexName), RequestOptions.DEFAULT);

            if (!exists) {
                // İndeks yoksa oluştur
                CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);

                // Mapping - alan tanımlarını belirle
                Map<String, Object> properties = new HashMap<>();

                // rateName alanı - keyword tipi
                Map<String, Object> rateName = new HashMap<>();
                rateName.put("type", "keyword");

                // bid ve ask alanları - double tipi
                Map<String, Object> bid = new HashMap<>();
                bid.put("type", "double");

                Map<String, Object> ask = new HashMap<>();
                ask.put("type", "double");

                // spread ve midPrice alanları - double tipi (hesaplanmış)
                Map<String, Object> spread = new HashMap<>();
                spread.put("type", "double");

                Map<String, Object> midPrice = new HashMap<>();
                midPrice.put("type", "double");

                // timestamp alanı - date tipi
                Map<String, Object> timestamp = new HashMap<>();
                timestamp.put("type", "date");
                timestamp.put("format", "date_time||strict_date_time");

                // Tüm alanları properties altında birleştir
                properties.put("rateName", rateName);
                properties.put("bid", bid);
                properties.put("ask", ask);
                properties.put("spread", spread);
                properties.put("midPrice", midPrice);
                properties.put("timestamp", timestamp);

                Map<String, Object> mapping = new HashMap<>();
                mapping.put("properties", properties);

                // Create a proper mapping using XContentType.JSON
                String mappingJson = objectMapper.writeValueAsString(mapping);
                createIndexRequest.mapping(mappingJson, XContentType.JSON);

                // İndeksi oluştur
                client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                logger.info("İndeks oluşturuldu: {}", indexName);
            } else {
                logger.info("İndeks zaten var: {}", indexName);
            }
        } catch (IOException e) {
            logger.error("İndeks oluşturulurken hata: {}", indexName, e);
        }
    }

    /**
     * RateData'yı OpenSearch'e kaydet
     * @param rateData Kur verisi
     */
    public void indexRateData(RateData rateData) {
        try {
            // Günlük indeks adını oluştur
            String indexName = getIndexName();

            // ID oluştur - rateName ve timestamp birleşimi
            String id = rateData.getRateName() + "_" +
                    rateData.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Belgeyi hazırla - spread ve midPrice hesaplanmış alanlarını da ekle
            Map<String, Object> document = new HashMap<>();
            document.put("rateName", rateData.getRateName());
            document.put("bid", rateData.getBid());
            document.put("ask", rateData.getAsk());
            document.put("spread", rateData.getSpread());
            document.put("midPrice", rateData.getMidPrice());
            document.put("timestamp", rateData.getTimestamp().toString());

            // IndexRequest oluştur
            IndexRequest indexRequest = new IndexRequest(indexName)
                    .id(id)
                    .source(objectMapper.writeValueAsString(document), XContentType.JSON);

            // Belgeyi indeksle
            client.index(indexRequest, RequestOptions.DEFAULT);

            logger.debug("Kur verisi indekslendi: {}", rateData.getRateName());
        } catch (IOException e) {
            logger.error("Kur verisi indekslenirken hata: {}", rateData.getRateName(), e);
        }
    }

    /**
     * Günlük indeks adı oluştur
     * @return İndeks adı
     */
    private String getIndexName() {
        // Günlük indeks adı: rates-YYYY.MM.DD formatında
        return indexPrefix + "-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }
}