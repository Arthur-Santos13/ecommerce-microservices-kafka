package com.ecommerce.gateway.dto;

import java.util.List;

/**
 * Response body returned by the {@code POST /auth/login} endpoint.
 */
public record LoginResponse(String token, String username, List<String> roles) {
}
