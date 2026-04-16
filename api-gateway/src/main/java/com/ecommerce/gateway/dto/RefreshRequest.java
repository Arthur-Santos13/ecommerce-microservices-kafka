package com.ecommerce.gateway.dto;

/**
 * Request body for the {@code POST /auth/refresh} endpoint.
 */
public record RefreshRequest(String refreshToken) {
}
