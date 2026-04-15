package com.ecommerce.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient productRestClient(
            @Value("${services.product.url}") String productServiceUrl,
            RestClient.Builder builder) {
        return builder
                .baseUrl(productServiceUrl)
                .build();
    }
}
