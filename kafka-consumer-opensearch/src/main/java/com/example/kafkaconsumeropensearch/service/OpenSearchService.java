package com.example.kafkaconsumeropensearch.service;



import com.example.kafkaconsumeropensearch.model.RateData;

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

import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.retry.annotation.Backoff;

import org.springframework.retry.annotation.EnableRetry;

import org.springframework.retry.annotation.Retryable;

import org.springframework.stereotype.Service;



import jakarta.annotation.PostConstruct;

import java.io.IOException;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;

import java.util.HashMap;

import java.util.Map;



@Service

@EnableRetry

public class OpenSearchService {



    private static final Logger logger = LogManager.getLogger(OpenSearchService.class);



    private final RestHighLevelClient client;

    private final ObjectMapper objectMapper;



    @Value("${opensearch.index-prefix:rates}")

    private String indexPrefix;



    @Autowired

    public OpenSearchService(RestHighLevelClient client, @Qualifier("opensearchObjectMapper") ObjectMapper objectMapper) {

        this.client = client;

        this.objectMapper = objectMapper;

        logger.info("OpenSearchService initialized with objectMapper: {}", objectMapper);

    }



    // OpenSearchService.java dosyasında
    @PostConstruct
    @Retryable(value = {IOException.class}, maxAttempts = 5, backoff = @Backoff(delay = 5000))
    public void init() {
        try {
            // Günlük indeks adı oluştur
            String indexName = getIndexName();
            logger.info("İndeks oluşturma başlatılıyor: {}", indexName);

            // İndeks var mı kontrol et
            boolean exists = client.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);

            if (!exists) {
                // İndeks yoksa oluştur
                CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);

                // Alanlar için mapping tanımla
                Map<String, Object> properties = new HashMap<>();

                // rateName alanı - keyword tipi
                Map<String, Object> rateName = new HashMap<>();
                rateName.put("type", "keyword");

                // bid ve ask alanları - double tipi
                Map<String, Object> bid = new HashMap<>();
                bid.put("type", "double");

                Map<String, Object> ask = new HashMap<>();
                ask.put("type", "double");

                // timestamp alanı - date tipi - FORMAT DÜZELTİLDİ!
                Map<String, Object> timestamp = new HashMap<>();
                timestamp.put("type", "date");
                timestamp.put("format", "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ||yyyy-MM-dd'T'HH:mm:ss.SSSSSS||yyyy-MM-dd'T'HH:mm:ss||strict_date_optional_time");

                // Tüm alanları properties'e ekle
                properties.put("rateName", rateName);
                properties.put("bid", bid);
                properties.put("ask", ask);
                properties.put("timestamp", timestamp);

                Map<String, Object> mapping = new HashMap<>();
                mapping.put("properties", properties);

                // Mapping oluştur
                String mappingJson = objectMapper.writeValueAsString(mapping);
                createIndexRequest.mapping(mappingJson, XContentType.JSON);

                // İndeksi oluştur
                client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                logger.info("İndeks başarıyla oluşturuldu: {}", indexName);
            } else {
                logger.info("İndeks zaten mevcut: {}", indexName);
            }
        } catch (IOException e) {
            logger.error("İndeks oluşturulurken hata: {}", e.getMessage());
            throw new RuntimeException("İndeks oluşturulamadı", e);
        }
    }


    @Retryable(value = {IOException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void indexRateData(RateData rateData) {
        try {
            // Günlük indeks adı oluştur
            String indexName = getIndexName();

            // ID oluştur
            String id = rateData.getRateName() + "_" +
                    rateData.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME);

            // Doküman hazırla
            Map<String, Object> document = new HashMap<>();
            document.put("rateName", rateData.getRateName());
            document.put("bid", rateData.getBid());
            document.put("ask", rateData.getAsk());
            document.put("spread", rateData.getAsk() - rateData.getBid());
            document.put("midPrice", (rateData.getBid() + rateData.getAsk()) / 2);

            // Tarih formatını basitleştiriyoruz
            document.put("timestamp", rateData.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));

            // IndexRequest oluştur
            IndexRequest indexRequest = new IndexRequest(indexName)
                    .id(id)
                    .source(document);

            // Dokümanı ekle
            client.index(indexRequest, RequestOptions.DEFAULT);

            logger.debug("Rate verisi indekslendi: {}", rateData.getRateName());
        } catch (IOException e) {
            logger.error("Rate verisi indekslenirken hata: {}", rateData.getRateName(), e);
            throw new RuntimeException("Rate verisi indekslenemedi", e);
        }
    }

    private String getIndexName() {

// Daily index name: rates-YYYY.MM.DD format

        return indexPrefix + "-" +

                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

    }

}