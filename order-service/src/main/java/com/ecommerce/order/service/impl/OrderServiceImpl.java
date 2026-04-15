package com.ecommerce.order.service.impl;

import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.client.dto.ProductResponse;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.domain.ProcessedEvent;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.order.exception.BusinessRuleViolationException;
import com.ecommerce.order.exception.KafkaPublishException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.exception.ProductServiceException;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.ProcessedEventRepository;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ProductClient productClient;
    private final OrderEventPublisher eventPublisher;

    @Override
    @Transactional
    public OrderResponse create(OrderRequest request) {
        Order order = Order.builder()
                .customerId(request.customerId())
                .status(OrderStatus.AWAITING_PAYMENT)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        List<OrderItemRequest> reservedItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        try {
            for (OrderItemRequest itemReq : request.items()) {
                ProductResponse product = productClient.findById(itemReq.productId());

                if (product.availableQuantity() < itemReq.quantity()) {
                    throw new BusinessRuleViolationException(
                            "Insufficient stock for product '" + product.name()
                                    + "': requested " + itemReq.quantity()
                                    + ", available " + product.availableQuantity());
                }

                productClient.reserveStock(itemReq.productId(), itemReq.quantity());
                reservedItems.add(itemReq);

                BigDecimal subtotal = product.price()
                        .multiply(BigDecimal.valueOf(itemReq.quantity()));

                OrderItem item = OrderItem.builder()
                        .order(order)
                        .productId(itemReq.productId())
                        .productName(product.name())
                        .unitPrice(product.price())
                        .quantity(itemReq.quantity())
                        .subtotal(subtotal)
                        .build();

                order.getItems().add(item);
                total = total.add(subtotal);
            }
        } catch (BusinessRuleViolationException | ProductServiceException ex) {
            rollbackReservations(reservedItems);
            throw ex;
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        try {
            eventPublisher.publishOrderCreated(OrderCreatedEvent.from(saved));
        } catch (KafkaPublishException ex) {
            rollbackReservations(reservedItems);
            throw ex;
        }

        return OrderResponse.from(saved);
    }

    @Override
    @Transactional
    public void onPaymentConfirmed(UUID orderId, String eventId) {
        if (processedEventRepository.existsByEventId(eventId)) {
            log.warn("Duplicate PaymentConfirmedEvent detected: eventId={}, orderId={} -- skipping",
                    eventId, orderId);
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            log.warn("PaymentConfirmedEvent ignored: order {} is in status {}, expected AWAITING_PAYMENT",
                    orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType("payment.confirmed")
                .build());

        log.info("Order {} confirmed after payment confirmation", orderId);
    }

    @Override
    @Transactional
    public void onPaymentFailed(UUID orderId, String eventId, String failureReason) {
        if (processedEventRepository.existsByEventId(eventId)) {
            log.warn("Duplicate PaymentFailedEvent detected: eventId={}, orderId={} -- skipping",
                    eventId, orderId);
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            log.warn("PaymentFailedEvent ignored: order {} is in status {}, expected AWAITING_PAYMENT",
                    orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        order.setFailureReason(failureReason);
        orderRepository.save(order);

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType("payment.failed")
                .build());

        log.info("Order {} marked as PAYMENT_FAILED: reason={}", orderId, failureReason);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        return OrderResponse.from(
                orderRepository.findById(id)
                        .orElseThrow(() -> new OrderNotFoundException(id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findByCustomer(UUID customerId) {
        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse cancel(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new BusinessRuleViolationException(
                    "Cannot cancel an order that has already been paid.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                    "Order is already cancelled.");
        }

        order.getItems().forEach(item -> {
            try {
                productClient.releaseStock(item.getProductId(), item.getQuantity());
            } catch (Exception ex) {
                log.error("Failed to release stock for product {} on cancel: {}",
                        item.getProductId(), ex.getMessage());
            }
        });

        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(orderRepository.save(order));
    }

    private void rollbackReservations(List<OrderItemRequest> reservedItems) {
        reservedItems.forEach(item -> {
            try {
                productClient.releaseStock(item.productId(), item.quantity());
            } catch (Exception ex) {
                log.error("Failed to rollback reservation for product {}: {}",
                        item.productId(), ex.getMessage());
            }
        });
    }
}