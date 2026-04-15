package com.ecommerce.order.client;

import com.ecommerce.order.client.dto.ProductResponse;
import com.ecommerce.order.client.dto.StockAdjustmentRequest;
import com.ecommerce.order.exception.ProductServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductClient {

    private final RestClient productRestClient;

    public ProductResponse findById(UUID productId) {
        try {
            return productRestClient.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ProductServiceException("Product not found: " + productId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ProductServiceException("Product service is unavailable");
                    })
                    .body(ProductResponse.class);
        } catch (RestClientException ex) {
            throw new ProductServiceException("Failed to reach product service", ex);
        }
    }

    public void reserveStock(UUID productId, int quantity) {
        try {
            productRestClient.post()
                    .uri("/api/v1/products/{id}/inventory/reserve", productId)
                    .body(new StockAdjustmentRequest(quantity, "Order reservation"))
                    .retrieve()
                    .onStatus(status -> status.value() == 422, (req, res) -> {
                        throw new ProductServiceException(
                                "Insufficient stock for product: " + productId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ProductServiceException("Product service is unavailable");
                    })
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ProductServiceException("Failed to reserve stock for product: " + productId, ex);
        }
    }

    public void releaseStock(UUID productId, int quantity) {
        try {
            productRestClient.post()
                    .uri("/api/v1/products/{id}/inventory/release", productId)
                    .body(new StockAdjustmentRequest(quantity, "Order cancellation"))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new ProductServiceException(
                                "Failed to release stock for product: " + productId);
                    })
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ProductServiceException("Failed to release stock for product: " + productId, ex);
        }
    }
}
