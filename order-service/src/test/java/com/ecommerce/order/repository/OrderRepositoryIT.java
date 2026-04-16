package com.ecommerce.order.repository;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("OrderRepository integration tests")
class OrderRepositoryIT {

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
    }

    @Autowired
    private OrderRepository orderRepository;

    private Order buildOrder(UUID customerId, OrderStatus status) {
        Order order = Order.builder()
                .customerId(customerId)
                .status(status)
                .totalAmount(new BigDecimal("199.90"))
                .items(new ArrayList<>())
                .build();
        order.onCreate();

        OrderItem item = OrderItem.builder()
                .order(order)
                .productId(UUID.randomUUID())
                .productName("Widget")
                .unitPrice(new BigDecimal("199.90"))
                .quantity(1)
                .subtotal(new BigDecimal("199.90"))
                .build();
        order.getItems().add(item);
        return order;
    }

    @Test
    @DisplayName("saves order with items and retrieves by ID")
    void save_andFindById() {
        UUID customerId = UUID.randomUUID();
        Order saved = orderRepository.save(buildOrder(customerId, OrderStatus.AWAITING_PAYMENT));

        assertThat(saved.getId()).isNotNull();
        Order found = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getCustomerId()).isEqualTo(customerId);
        assertThat(found.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(found.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("findByCustomerId returns only orders belonging to the given customer")
    void findByCustomerId_returnsCorrectOrders() {
        UUID customerA = UUID.randomUUID();
        UUID customerB = UUID.randomUUID();

        orderRepository.save(buildOrder(customerA, OrderStatus.AWAITING_PAYMENT));
        orderRepository.save(buildOrder(customerA, OrderStatus.CONFIRMED));
        orderRepository.save(buildOrder(customerB, OrderStatus.AWAITING_PAYMENT));

        List<Order> ordersA = orderRepository.findByCustomerId(customerA);

        assertThat(ordersA).hasSize(2);
        assertThat(ordersA).allMatch(o -> o.getCustomerId().equals(customerA));
    }

    @Test
    @DisplayName("status update is persisted correctly")
    void updateStatus_persisted() {
        Order order = orderRepository.save(
                buildOrder(UUID.randomUUID(), OrderStatus.AWAITING_PAYMENT));

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
