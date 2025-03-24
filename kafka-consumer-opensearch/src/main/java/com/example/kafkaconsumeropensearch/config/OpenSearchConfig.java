package com.example.kafkaconsumeropensearch.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenSearch konfigürasyon sınıfı
 */

@Configuration
public class OpenSearchConfig {

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
        // Url'den host ve port bilgilerini çıkar
        String protocol = "http";
        String hostname;
        int port = 9200;

        // URL'yi parçala
        if (opensearchUrl.startsWith("http://")) {
            opensearchUrl = opensearchUrl.substring(7);
        } else if (opensearchUrl.startsWith("https://")) {
            opensearchUrl = opensearchUrl.substring(8);
            protocol = "https";
        }

        // Host ve port
        if (opensearchUrl.contains(":")) {
            String[] parts = opensearchUrl.split(":");
            hostname = parts[0];
            port = Integer.parseInt(parts[1]);
        } else {
            hostname = opensearchUrl;
        }

        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostname, port, protocol));

        // Kimlik doğrulama bilgileri varsa ekle
        if (username != null && !username.isEmpty()) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));

            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        return new RestHighLevelClient(builder);
    }
}