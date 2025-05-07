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



    @PostConstruct

    @Retryable(value = {IOException.class}, maxAttempts = 5, backoff = @Backoff(delay = 5000))

    public void init() {

        int retryCount = 0;

        int maxRetries = 5;

        long retryDelayMs = 10000; // 10 saniye



        while (retryCount < maxRetries) {

            try {

// Create daily index name

                String indexName = getIndexName();

                logger.info("İndeks oluşturma başlatılıyor: {}", indexName);



// Check if index exists

                boolean exists = client.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);



                if (!exists) {

// Create index if it doesn't exist

                    CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);



// Define mapping for fields

                    Map<String, Object> properties = new HashMap<>();



// rateName field - keyword type

                    Map<String, Object> rateName = new HashMap<>();

                    rateName.put("type", "keyword");



// bid and ask fields - double type

                    Map<String, Object> bid = new HashMap<>();

                    bid.put("type", "double");



                    Map<String, Object> ask = new HashMap<>();

                    ask.put("type", "double");



// spread and midPrice fields - double type (calculated)

                    Map<String, Object> spread = new HashMap<>();

                    spread.put("type", "double");



                    Map<String, Object> midPrice = new HashMap<>();

                    midPrice.put("type", "double");



// timestamp field - date type

                    Map<String, Object> timestamp = new HashMap<>();

                    timestamp.put("type", "date");

                    timestamp.put("format", "date_time||strict_date_time");



// Add all fields to properties

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



// Create the index with RequestOptions

                    client.indices().create(createIndexRequest, RequestOptions.DEFAULT);

                    logger.info("İndeks başarıyla oluşturuldu: {}", indexName);

                    break; // Başarıyla oluşturuldu, döngüden çık

                } else {

                    logger.info("İndeks zaten mevcut: {}", indexName);

                    break; // İndeks zaten var, döngüden çık

                }

            } catch (IOException e) {

                retryCount++;

                logger.error("İndeks oluşturulurken hata (deneme {}/{}): {}",

                        retryCount, maxRetries, e.getMessage());



                if (retryCount >= maxRetries) {

                    logger.error("Maksimum yeniden deneme sayısına ulaşıldı. İndeks oluşturma başarısız.");

                    break;

                }



                try {

                    logger.info("{} ms sonra tekrar denenecek...", retryDelayMs);

                    Thread.sleep(retryDelayMs);

                } catch (InterruptedException ie) {

                    Thread.currentThread().interrupt();

                    logger.error("Bekleme sırasında kesinti", ie);

                    break;

                }

            }

        }

    }



    @Retryable(value = {IOException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))

    public void indexRateData(RateData rateData) {

        try {

// Create daily index name

            String indexName = getIndexName();



// Create ID - combination of rateName and timestamp

            String id = rateData.getRateName() + "_" +

                    rateData.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);



// Prepare document - add calculated fields spread and midPrice

            Map<String, Object> document = new HashMap<>();

            document.put("rateName", rateData.getRateName());

            document.put("bid", rateData.getBid());

            document.put("ask", rateData.getAsk());

            document.put("spread", rateData.getAsk() - rateData.getBid());

            document.put("midPrice", (rateData.getBid() + rateData.getAsk()) / 2);

            document.put("timestamp", rateData.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME));



// Create IndexRequest

            IndexRequest indexRequest = new IndexRequest(indexName)

                    .id(id)

                    .source(document); // Map direkt olarak kullanılabilir



// Index the document with RequestOptions

            client.index(indexRequest, RequestOptions.DEFAULT);



            logger.debug("Rate data indexed: {}", rateData.getRateName());

        } catch (IOException e) {

            logger.error("Error indexing rate data: {}", rateData.getRateName(), e);

            throw new RuntimeException("Failed to index rate data", e);

        }

    }



    private String getIndexName() {

// Daily index name: rates-YYYY.MM.DD format

        return indexPrefix + "-" +

                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

    }

}