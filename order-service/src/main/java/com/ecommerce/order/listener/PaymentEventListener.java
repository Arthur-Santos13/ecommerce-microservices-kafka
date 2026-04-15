package com.ecommerce.order.listener;

import com.ecommerce.order.event.PaymentConfirmedEvent;
import com.ecommerce.order.event.PaymentFailedEvent;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final OrderService orderService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(
            topics = "${kafka.topics.payment-confirmed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("Received PaymentConfirmedEvent: eventId={}, orderId={}, customerId={}",
                event.eventId(), event.orderId(), event.customerId());
        orderService.onPaymentConfirmed(event.orderId(), event.eventId());
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(
            topics = "${kafka.topics.payment-failed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: eventId={}, orderId={}, reason={}",
                event.eventId(), event.orderId(), event.failureReason());
        orderService.onPaymentFailed(event.orderId(), event.eventId(), event.failureReason());
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, Object> record, Exception ex) {
        log.error("DLT: payment event could not be processed after retries. topic={}, key={}, error={}",
                record.topic(), record.key(), ex.getMessage());
    }
}
