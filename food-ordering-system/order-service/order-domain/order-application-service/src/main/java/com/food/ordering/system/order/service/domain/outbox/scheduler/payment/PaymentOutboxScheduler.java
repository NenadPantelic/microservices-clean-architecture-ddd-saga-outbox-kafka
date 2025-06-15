package com.food.ordering.system.order.service.domain.outbox.scheduler.payment;

import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.port.output.message.publisher.payment.PaymentRequestMessagePublisher;
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
public class PaymentOutboxScheduler implements OutboxScheduler {

    private final PaymentOutboxHelper paymentOutboxHelper;
    private final PaymentRequestMessagePublisher paymentRequestMessagePublisher;

    public PaymentOutboxScheduler(PaymentOutboxHelper paymentOutboxHelper,
                                  PaymentRequestMessagePublisher paymentRequestMessagePublisher) {
        this.paymentOutboxHelper = paymentOutboxHelper;
        this.paymentRequestMessagePublisher = paymentRequestMessagePublisher;
    }

    @Override
    @Transactional
    @Scheduled(
            fixedDelayString = "${order-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${order-service.outbox-scheduler-initial-delay}"
    )
    public void processOutboxMessage() {
        // in the payment outbox table, we will have the domain events for 2 types of events:
        // 1. order created
        // 2. order cancelling
        // order service triggers the payment service for these 2 types of events

        // we are updating the outbox status of messages sent to Kafka, so we do not poll them multiple times
        // only those message that are in `STARTED` state are polled. Still, if Kafka producer-consumer mechanism is
        // slower than the rate of a polling scheduler, the same outbox message could be processed multiple times (i.e.
        // sent multiple times). This cannot be avoided with the strict lock-and-wait approach (that would just slow
        // down the whole delivery). On the consumer side we have to pay attention not to process duplicate messages
        // multiple times (idempotent messages).
        List<OrderPaymentOutboxMessage> orderPaymentOutboxMessages =
                paymentOutboxHelper.getPaymentOutboxMessageByOutboxStatusAndSagaStatus(
                        // started and compensating means pending and cancelling events
                        OutboxStatus.STARTED, SagaStatus.STARTED, SagaStatus.COMPENSATING
                );
        if (orderPaymentOutboxMessages == null || orderPaymentOutboxMessages.isEmpty()) {
            log.error("No OrderPaymentOutboxMessage in the outbox table with the given statuses.");
            return;
        }

        log.info("Received {} OrderPaymentOutboxMessage items with ids: {}",
                orderPaymentOutboxMessages.size(),
                orderPaymentOutboxMessages.stream()
                        .map(outboxMessage -> outboxMessage.getId().toString())
                        .collect(Collectors.joining(","))
        );

        orderPaymentOutboxMessages.forEach(orderPaymentOutboxMessage ->
                paymentRequestMessagePublisher.publish(orderPaymentOutboxMessage, this::updateOutboxStatus)
        );
        log.info("{} OrderPaymentOutboxMessage items sent to the message bus!", orderPaymentOutboxMessages.size());
    }

    private void updateOutboxStatus(OrderPaymentOutboxMessage orderPaymentOutboxMessage, OutboxStatus outboxStatus) {
        orderPaymentOutboxMessage.setOutboxStatus(outboxStatus);
        paymentOutboxHelper.save(orderPaymentOutboxMessage);
        log.info("OrderPaymentOutboxMessage[id = {}] is updated with status {}",
                orderPaymentOutboxMessage.getId(), outboxStatus.name()
        );
    }
}
