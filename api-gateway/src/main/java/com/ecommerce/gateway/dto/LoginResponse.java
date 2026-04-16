package com.ecommerce.gateway.dto;

import java.util.List;

/**
 * Response body returned by the {@code POST /auth/login} and {@code POST /auth/refresh} endpoints.
 */
public record LoginResponse(String token, String refreshToken, String username, List<String> roles) {
}
