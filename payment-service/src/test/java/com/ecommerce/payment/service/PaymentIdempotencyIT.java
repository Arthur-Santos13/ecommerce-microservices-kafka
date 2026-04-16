package com.ecommerce.payment.service;

import com.ecommerce.payment.domain.PaymentMethod;
import com.ecommerce.payment.domain.PaymentStatus;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.event.OrderCreatedEvent;
import com.ecommerce.payment.event.PaymentEventPublisher;
import com.ecommerce.payment.exception.BusinessRuleViolationException;
import com.ecommerce.payment.gateway.GatewayResult;
import com.ecommerce.payment.gateway.PaymentGatewaySimulator;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.repository.PaymentTransactionRepository;
import com.ecommerce.payment.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for PaymentService focusing on:
 * - Idempotent processing of OrderCreatedEvents (duplicate eventId must be ignored)
 * - Correct status transitions: PENDING → PROCESSING → PAID / FAILED
 * - Correct event publishing based on gateway outcome
 * - Duplicate payment guard for the same orderId
 */
@SpringBootTest
@Testcontainers
@DisplayName("Payment idempotent processing tests")
class PaymentIdempotencyIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("payment_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @MockBean private PaymentGatewaySimulator gatewaySimulator;
    @MockBean private PaymentEventPublisher eventPublisher;

    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentTransactionRepository transactionRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;

    private OrderCreatedEvent buildOrderEvent(String eventId, UUID orderId) {
        return new OrderCreatedEvent(
                eventId,
                "order.created",
                "order-service",
                Instant.now(),
                orderId,
                UUID.randomUUID(),
                new BigDecimal("250.00"),
                List.of()
        );
    }

    // ── processFromEvent: approved ─────────────────────────────────────────────

    @Nested
    @DisplayName("processFromEvent() — gateway approves")
    class ProcessApproved {

        @Test
        @DisplayName("creates payment with PAID status and publishes PaymentConfirmedEvent")
        void process_approved_persistsAndPublishes() {
            given(gatewaySimulator.process(any())).willReturn(GatewayResult.approved("Approved"));
            doNothing().when(eventPublisher).publishPaymentConfirmed(any());

            String eventId = UUID.randomUUID().toString();
            UUID orderId = UUID.randomUUID();

            PaymentResponse response = paymentService.processFromEvent(buildOrderEvent(eventId, orderId));

            assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(paymentRepository.findByOrderId(orderId)).isPresent();
            assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();

            // Transaction records: PENDING + PROCESSING + PAID = 3
            UUID paymentId = response.id();
            assertThat(transactionRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId)).hasSize(3);

            verify(eventPublisher).publishPaymentConfirmed(any());
            verify(eventPublisher, never()).publishPaymentFailed(any());
        }
    }

    // ── processFromEvent: declined ─────────────────────────────────────────────

    @Nested
    @DisplayName("processFromEvent() — gateway declines")
    class ProcessDeclined {

        @Test
        @DisplayName("creates payment with FAILED status and publishes PaymentFailedEvent")
        void process_declined_persistsAndPublishes() {
            given(gatewaySimulator.process(any())).willReturn(GatewayResult.declined("Card declined"));
            doNothing().when(eventPublisher).publishPaymentFailed(any());

            String eventId = UUID.randomUUID().toString();
            UUID orderId = UUID.randomUUID();

            PaymentResponse response = paymentService.processFromEvent(buildOrderEvent(eventId, orderId));

            assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(response.failureReason()).isEqualTo("Card declined");
            assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();

            verify(eventPublisher).publishPaymentFailed(any());
            verify(eventPublisher, never()).publishPaymentConfirmed(any());
        }
    }

    // ── Idempotency ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Idempotency — duplicate eventId")
    class Idempotency {

        @Test
        @DisplayName("second call with same eventId returns existing payment without reprocessing")
        void processFromEvent_duplicateEventId_ignoredAndReturnsExisting() {
            given(gatewaySimulator.process(any())).willReturn(GatewayResult.approved("Approved"));
            doNothing().when(eventPublisher).publishPaymentConfirmed(any());

            String eventId = UUID.randomUUID().toString();
            UUID orderId = UUID.randomUUID();
            OrderCreatedEvent event = buildOrderEvent(eventId, orderId);

            PaymentResponse first  = paymentService.processFromEvent(event);
            PaymentResponse second = paymentService.processFromEvent(event); // duplicate

            assertThat(second.id()).isEqualTo(first.id());
            assertThat(second.status()).isEqualTo(PaymentStatus.PAID);

            // Gateway and publisher invoked exactly once, not twice
            verify(gatewaySimulator, times(1)).process(any());
            verify(eventPublisher, times(1)).publishPaymentConfirmed(any());
        }
    }

    // ── create (REST endpoint): duplicate orderId guard ────────────────────────

    @Nested
    @DisplayName("create() — duplicate orderId guard")
    class CreateDuplicateGuard {

        @Test
        @DisplayName("throws BusinessRuleViolationException when payment already exists for orderId")
        void create_duplicateOrderId_throwsException() {
            given(gatewaySimulator.process(any())).willReturn(GatewayResult.approved("Approved"));
            doNothing().when(eventPublisher).publishPaymentConfirmed(any());

            UUID orderId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            // First payment via event
            paymentService.processFromEvent(
                    buildOrderEvent(UUID.randomUUID().toString(), orderId));

            // Direct REST create for the same orderId must be rejected
            PaymentRequest duplicate = new PaymentRequest(
                    orderId, customerId, new BigDecimal("250.00"), PaymentMethod.CREDIT_CARD);

            assertThatThrownBy(() -> paymentService.create(duplicate))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining(orderId.toString());
        }
    }
}
