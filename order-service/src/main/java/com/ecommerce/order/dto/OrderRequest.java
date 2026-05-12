package com.ecommerce.order.dto;

import com.ecommerce.order.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OrderRequest(
        @NotNull UUID customerId,
        @NotEmpty @Valid List<OrderItemRequest> items,
        PaymentMethod paymentMethod
) {
}
