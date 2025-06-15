package com.food.ordering.system.order.service.domain.port.output.message.publisher.restaurantapproval;

import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.outbox.OutboxStatus;

import java.util.function.BiConsumer;

public interface RestaurantApprovalRequestMessagePublisher {

    void publish(OrderApprovalOutboxMessage orderApprovalOutboxMessage,
                 // will tell if Kafka publisher has sent the event successfully
                 BiConsumer<OrderApprovalOutboxMessage, OutboxStatus> outboxCallback);
}
