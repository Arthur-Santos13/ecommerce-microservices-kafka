package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.order.client.dto.ProductResponse;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests the event-driven state transitions in the order lifecycle:
 *
 *  AWAITING_PAYMENT
 *      ├─ PaymentConfirmedEvent → CONFIRMED  (OrderConfirmedEvent published)
 *      └─ PaymentFailedEvent   → CANCELLED  (stock released, OrderCancelledEvent published)
 *
 * Idempotency: duplicate events with the same eventId must be silently ignored.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Order event-driven flow tests")
class OrderEventDrivenFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("order_event_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("services.product.url", () -> "http://localhost:8081");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @MockBean private ProductClient productClient;
    @MockBean private OrderEventPublisher eventPublisher;

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;

    private OrderResponse createOrder() {
        UUID productId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        given(productClient.findById(productId))
                .willReturn(new ProductResponse(productId, "Widget", new BigDecimal("100.00"), 10));
        doNothing().when(productClient).reserveStock(eq(productId), eq(1));
        doNothing().when(eventPublisher).publishOrderCreated(any());

        return orderService.create(new OrderRequest(customerId,
                List.of(new OrderItemRequest(productId, 1))));
    }

    // ── PaymentConfirmedEvent ──────────────────────────────────────────────────

    @Test
    @DisplayName("onPaymentConfirmed: transitions order to CONFIRMED and publishes OrderConfirmedEvent")
    void onPaymentConfirmed_transitionsToConfirmed() {
        OrderResponse order = createOrder();
        String eventId = UUID.randomUUID().toString();

        doNothing().when(eventPublisher).publishOrderConfirmed(any());

        orderService.onPaymentConfirmed(order.id(), eventId);

        OrderResponse updated = orderService.findById(order.id());
        assertThat(updated.status()).isEqualTo(OrderStatus.CONFIRMED);

        verify(eventPublisher).publishOrderConfirmed(any());
        assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();
    }

    @Test
    @DisplayName("onPaymentConfirmed: duplicate event is silently ignored (idempotency)")
    void onPaymentConfirmed_duplicateEvent_ignored() {
        OrderResponse order = createOrder();
        String eventId = UUID.randomUUID().toString();

        doNothing().when(eventPublisher).publishOrderConfirmed(any());

        orderService.onPaymentConfirmed(order.id(), eventId);
        orderService.onPaymentConfirmed(order.id(), eventId); // duplicate

        // OrderConfirmedEvent must be published exactly once
        verify(eventPublisher).publishOrderConfirmed(any());
    }

    // ── PaymentFailedEvent ────────────────────────────────────────────────────

    @Test
    @DisplayName("onPaymentFailed: transitions order to CANCELLED, releases stock, publishes OrderCancelledEvent")
    void onPaymentFailed_transitionsToCancelled() {
        OrderResponse order = createOrder();
        String eventId = UUID.randomUUID().toString();
        String reason = "Insufficient funds";

        // stock release for each order item
        doNothing().when(productClient).releaseStock(any(UUID.class), any(Integer.class));
        doNothing().when(eventPublisher).publishOrderCancelled(any());

        orderService.onPaymentFailed(order.id(), eventId, reason);

        OrderResponse updated = orderService.findById(order.id());
        assertThat(updated.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(updated.failureReason()).isEqualTo(reason);

        verify(productClient).releaseStock(any(UUID.class), eq(1));
        verify(eventPublisher).publishOrderCancelled(any());
        assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();
    }

    @Test
    @DisplayName("onPaymentFailed: duplicate event is silently ignored (idempotency)")
    void onPaymentFailed_duplicateEvent_ignored() {
        OrderResponse order = createOrder();
        String eventId = UUID.randomUUID().toString();

        doNothing().when(productClient).releaseStock(any(UUID.class), any(Integer.class));
        doNothing().when(eventPublisher).publishOrderCancelled(any());

        orderService.onPaymentFailed(order.id(), eventId, "Declined");
        orderService.onPaymentFailed(order.id(), eventId, "Declined"); // duplicate

        // OrderCancelledEvent must be published exactly once
        verify(eventPublisher).publishOrderCancelled(any());
    }

    @Test
    @DisplayName("onPaymentConfirmed: event for non-AWAITING_PAYMENT order is silently ignored")
    void onPaymentConfirmed_wrongStatus_ignored() {
        OrderResponse order = createOrder();
        String firstEventId = UUID.randomUUID().toString();
        doNothing().when(eventPublisher).publishOrderConfirmed(any());
        orderService.onPaymentConfirmed(order.id(), firstEventId); // → CONFIRMED

        // Second, different eventId — should be ignored because order is no longer AWAITING_PAYMENT
        orderService.onPaymentConfirmed(order.id(), UUID.randomUUID().toString());

        verify(eventPublisher).publishOrderConfirmed(any()); // still only once
    }
}
