package com.ecommerce.gateway.dto;

/**
 * Request body for the {@code POST /auth/login} endpoint.
 */
public record LoginRequest(String username, String password) {
}
