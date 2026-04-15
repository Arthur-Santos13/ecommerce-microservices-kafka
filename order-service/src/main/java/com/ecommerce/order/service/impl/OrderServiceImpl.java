package com.ecommerce.order.service.impl;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.exception.BusinessRuleViolationException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponse create(OrderRequest request) {
        Order order = Order.builder()
                .customerId(request.customerId())
                .status(OrderStatus.CONFIRMED)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : request.items()) {
            BigDecimal subtotal = itemReq.unitPrice()
                    .multiply(BigDecimal.valueOf(itemReq.quantity()));

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(itemReq.productId())
                    .productName(itemReq.productName())
                    .unitPrice(itemReq.unitPrice())
                    .quantity(itemReq.quantity())
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(item);
            total = total.add(subtotal);
        }

        order.setTotalAmount(total);
        return OrderResponse.from(orderRepository.save(order));
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

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessRuleViolationException(
                    "Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(orderRepository.save(order));
    }
}
