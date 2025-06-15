package com.food.ordering.system.order.service.domain.outbox.scheduler.approval;

import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.port.output.message.publisher.payment.PaymentRequestMessagePublisher;
import com.food.ordering.system.order.service.domain.port.output.message.publisher.restaurantapproval.RestaurantApprovalRequestMessagePublisher;
import com.food.ordering.system.outbox.OutboxScheduler;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RestaurantApprovalOutboxScheduler implements OutboxScheduler {

    private final ApprovalOutboxHelper approvalOutboxHelper;
    private final RestaurantApprovalRequestMessagePublisher restaurantApprovalRequestMessagePublisher;

    public RestaurantApprovalOutboxScheduler(ApprovalOutboxHelper approvalOutboxHelper,
                                             RestaurantApprovalRequestMessagePublisher restaurantApprovalRequestMessagePublisher) {
        this.approvalOutboxHelper = approvalOutboxHelper;
        this.restaurantApprovalRequestMessagePublisher = restaurantApprovalRequestMessagePublisher;
    }

    @Override
    @Transactional
    @Scheduled(
            fixedDelayString = "${order-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${order-service.outbox-scheduler-initial-delay}"
    )
    public void processOutboxMessage() {
        // in the payment outbox table, we will have the domain events for OrderPaid event:
        List<OrderApprovalOutboxMessage> orderApprovalOutboxMessages =
                approvalOutboxHelper.getApprovalOutboxMessageByOutboxStatusAndSagaStatus(
                        // paid means pending and cancelling events
                        OutboxStatus.STARTED, SagaStatus.PROCESSING
                );
        if (orderApprovalOutboxMessages == null || orderApprovalOutboxMessages.isEmpty()) {
            log.error("No OrderApprovalOutboxMessage in the outbox table with the given statuses.");
            return;
        }

        log.info("Received {} OrderApprovalOutboxMessage items with ids: {}",
                orderApprovalOutboxMessages.size(),
                orderApprovalOutboxMessages.stream()
                        .map(outboxMessage -> outboxMessage.getId().toString())
                        .collect(Collectors.joining(","))
        );

        orderApprovalOutboxMessages.forEach(orderPaymentOutboxMessage ->
                restaurantApprovalRequestMessagePublisher.publish(orderPaymentOutboxMessage, this::updateOutboxStatus)
        );
        log.info("{} OrderPaymentOutboxMessage items sent to the message bus!", orderApprovalOutboxMessages.size());
    }

    private void updateOutboxStatus(OrderApprovalOutboxMessage orderApprovalOutboxMessage, OutboxStatus outboxStatus) {
        orderApprovalOutboxMessage.setOutboxStatus(outboxStatus);
        approvalOutboxHelper.save(orderApprovalOutboxMessage);
        log.info("OrderApprovalOutboxMessage[id = {}] is updated with status {}",
                orderApprovalOutboxMessage.getId(), outboxStatus.name()
        );
    }
}
