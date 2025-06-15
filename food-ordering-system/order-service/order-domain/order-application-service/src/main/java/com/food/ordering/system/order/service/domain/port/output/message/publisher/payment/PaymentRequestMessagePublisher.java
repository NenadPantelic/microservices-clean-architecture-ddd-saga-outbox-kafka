package com.food.ordering.system.order.service.domain.port.output.message.publisher.payment;

import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.outbox.OutboxStatus;

import java.util.function.BiConsumer;

public interface PaymentRequestMessagePublisher {

    void publish(OrderPaymentOutboxMessage orderPaymentOutboxMessage,
                 // will tell if Kafka publisher has sent the event successfully
                 BiConsumer<OrderPaymentOutboxMessage, OutboxStatus> outboxCallback);
}
