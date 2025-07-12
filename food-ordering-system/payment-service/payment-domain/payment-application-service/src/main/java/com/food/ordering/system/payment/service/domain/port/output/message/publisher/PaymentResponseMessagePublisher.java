package com.food.ordering.system.payment.service.domain.port.output.message.publisher;

import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderOutboxMessage;

import java.util.function.BiConsumer;

public interface PaymentResponseMessagePublisher {

    void publish(OrderOutboxMessage orderOutboxMessage,
                 BiConsumer<OrderOutboxMessage, OutboxStatus> outboxCallback);
}
