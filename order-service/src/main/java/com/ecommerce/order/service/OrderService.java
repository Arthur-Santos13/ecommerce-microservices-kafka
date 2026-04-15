package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse create(OrderRequest request);

    OrderResponse findById(UUID id);

    List<OrderResponse> findByCustomer(UUID customerId);

    OrderResponse cancel(UUID id);

    void onPaymentConfirmed(UUID orderId, String eventId);

    void onPaymentFailed(UUID orderId, String eventId, String failureReason);
}
