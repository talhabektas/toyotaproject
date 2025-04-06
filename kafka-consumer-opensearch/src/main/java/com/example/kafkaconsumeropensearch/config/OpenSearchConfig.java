package com.example.kafkaconsumeropensearch.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenSearch konfigürasyon sınıfı
 */
@Configuration
public class OpenSearchConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchConfig.class);

    @Value("${opensearch.url}")
    private String opensearchUrl;

    @Value("${opensearch.username:}")
    private String username;

    @Value("${opensearch.password:}")
    private String password;

    /**
     * OpenSearch için RestHighLevelClient
     * @return RestHighLevelClient
     */
    @Bean(destroyMethod = "close")
    public RestHighLevelClient opensearchClient() {
        try {
            // URL'yi parçala
            String protocol = "http";
            String hostname = opensearchUrl;
            int port = 9200;

            // URL'deki protokolü temizle
            if (hostname.startsWith("http://")) {
                hostname = hostname.substring(7);
            } else if (hostname.startsWith("https://")) {
                hostname = hostname.substring(8);
                protocol = "https";
            }

            // Host ve port ayrıştırma
            if (hostname.contains(":")) {
                String[] parts = hostname.split(":");
                hostname = parts[0];
                port = Integer.parseInt(parts[1]);
            }

            logger.info("OpenSearch bağlantı parametreleri: {}://{}:{}", protocol, hostname, port);

            // Bağlantıyı oluştur
            RestClientBuilder builder = RestClient.builder(
                    new HttpHost(hostname, port, protocol));

            // Bağlantıya timeout ekle
            builder.setRequestConfigCallback(requestConfigBuilder ->
                    requestConfigBuilder
                            .setConnectTimeout(30000)
                            .setSocketTimeout(60000));

            // Kimlik doğrulama bilgileri varsa ekle
            if (username != null && !username.isEmpty()) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));

                builder.setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            }

            RestHighLevelClient client = new RestHighLevelClient(builder);

            // OpenSearch ping işlemi için RequestOptions yok
            logger.info("OpenSearch bağlantısı başarıyla oluşturuldu");

            return client;
        } catch (Exception e) {
            logger.error("OpenSearch client oluşturulurken hata: {}", e.getMessage(), e);
            throw new RuntimeException("OpenSearch client oluşturulamadı", e);
        }
    }
}