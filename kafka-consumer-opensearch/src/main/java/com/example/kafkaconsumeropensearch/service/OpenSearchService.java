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

@Service
public class OpenSearchService {

    private static final Logger logger = LogManager.getLogger(OpenSearchService.class);

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    @Value("${opensearch.index-prefix:rates}")
    private String indexPrefix;

    @Autowired
    public OpenSearchService(RestHighLevelClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
        logger.info("OpenSearchService initialized with objectMapper: {}", objectMapper);
    }

    @PostConstruct
    public void init() {
        // Create daily index name
        String indexName = getIndexName();

        try {
            // Check if index exists
            boolean exists = client.indices().exists(
                    new GetIndexRequest(indexName), RequestOptions.DEFAULT);

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

                // Create the index
                client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                logger.info("Index created: {}", indexName);
            } else {
                logger.info("Index already exists: {}", indexName);
            }
        } catch (IOException e) {
            logger.error("Error creating index: {}", indexName, e);
        }
    }

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
            document.put("timestamp", rateData.getTimestamp().toString());

            // Create IndexRequest
            IndexRequest indexRequest = new IndexRequest(indexName)
                    .id(id)
                    .source(objectMapper.writeValueAsString(document), XContentType.JSON);

            // Index the document
            client.index(indexRequest, RequestOptions.DEFAULT);

            logger.debug("Rate data indexed: {}", rateData.getRateName());
        } catch (IOException e) {
            logger.error("Error indexing rate data: {}", rateData.getRateName(), e);
        }
    }

    private String getIndexName() {
        // Daily index name: rates-YYYY.MM.DD format
        return indexPrefix + "-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }
}