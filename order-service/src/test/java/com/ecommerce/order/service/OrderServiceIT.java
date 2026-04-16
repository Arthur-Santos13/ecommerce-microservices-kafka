package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.client.dto.ProductResponse;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.order.exception.BusinessRuleViolationException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("OrderService integration tests")
class OrderServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("order_test")
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

    private ProductResponse mockProduct(UUID productId, int availableQty) {
        return new ProductResponse(productId, "Test Widget", new BigDecimal("49.90"), availableQty);
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates order, reserves stock, persists, and returns response")
        void create_success() {
            UUID productId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            given(productClient.findById(productId)).willReturn(mockProduct(productId, 5));
            doNothing().when(productClient).reserveStock(eq(productId), eq(2));
            doNothing().when(eventPublisher).publishOrderCreated(any());

            OrderRequest request = new OrderRequest(customerId,
                    List.of(new OrderItemRequest(productId, 2)));

            OrderResponse response = orderService.create(request);

            assertThat(response.id()).isNotNull();
            assertThat(response.customerId()).isEqualTo(customerId);
            assertThat(response.status()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
            assertThat(response.totalAmount()).isEqualByComparingTo("99.80");
            assertThat(response.items()).hasSize(1);

            verify(productClient).reserveStock(productId, 2);
            verify(eventPublisher).publishOrderCreated(any());

            assertThat(orderRepository.findById(response.id())).isPresent();
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when product has insufficient stock")
        void create_insufficientStock_throwsException() {
            UUID productId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            given(productClient.findById(productId)).willReturn(mockProduct(productId, 1));

            OrderRequest request = new OrderRequest(customerId,
                    List.of(new OrderItemRequest(productId, 5)));

            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Insufficient stock");
        }
    }

    // ── findById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("throws OrderNotFoundException for unknown ID")
        void findById_notFound() {
            assertThatThrownBy(() -> orderService.findById(UUID.randomUUID()))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    // ── findByCustomer ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByCustomer()")
    class FindByCustomer {

        @Test
        @DisplayName("returns empty list when customer has no orders")
        void findByCustomer_noOrders_returnsEmpty() {
            List<OrderResponse> orders = orderService.findByCustomer(UUID.randomUUID());
            assertThat(orders).isEmpty();
        }
    }
}
