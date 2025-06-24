package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.DomainConstants;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.domain.valueobject.PaymentStatus;
import com.food.ordering.system.order.service.domain.dto.message.PaymentResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.outbox.scheduler.approval.ApprovalOutboxHelper;
import com.food.ordering.system.order.service.domain.outbox.scheduler.payment.PaymentOutboxHelper;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import com.food.ordering.system.saga.SagaStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

// Order service will be the coordinator of Saga, all Saga code will be here
@Slf4j
@Component
// PaymentResponse will be called when getting a response from PaymentService
// OrderPaidEvent - when the payment is completed, we create this event (if everything went well)
// EmptyEvent - since this is a first step (first we should pay), in case something goes wrong we should just update
// status in a local database

// How is Saga processed?
// 1. OrderCreateCommandHandler::createOrder -> creates an outbox object with STARTED OutboxStatus
// 2. PaymentOutboxScheduler::processOutboxMessage -> fetches that outbox message and publishes it to Kafka topic
// 3. PaymentResponseKafkaListener::receive -> listens to payment response topic and process the payment
// 4. OrderPaymentSaga::process -> updates an outbox message (its Saga status and Order status); need to fire an event
// to trigger the restaurant approval flow (save it to local database)
public class OrderPaymentSaga implements SagaStep<PaymentResponse> {

    private final OrderDomainService orderDomainService;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final ApprovalOutboxHelper approvalOutboxHelper;
    private final OrderSagaHelper orderSagaHelper;
    private final OrderDataMapper orderDataMapper;

    public OrderPaymentSaga(OrderDomainService orderDomainService,
                            PaymentOutboxHelper paymentOutboxHelper,
                            ApprovalOutboxHelper approvalOutboxHelper,
                            OrderSagaHelper orderSagaHelper,
                            OrderDataMapper orderDataMapper) {
        this.orderDomainService = orderDomainService;
        this.approvalOutboxHelper = approvalOutboxHelper;
        this.orderSagaHelper = orderSagaHelper;
        this.paymentOutboxHelper = paymentOutboxHelper;
        this.orderDataMapper = orderDataMapper;
    }

    @Override
    @Transactional
    public void process(PaymentResponse paymentResponse) {
        Optional<OrderPaymentOutboxMessage> paymentOutboxMessageOptional = paymentOutboxHelper
                .getPaymentOutboxMessageBySagaIdAndSagaStatus(
                        UUID.fromString(paymentResponse.sagaId()),
                        SagaStatus.STARTED
                );

        // if the same message is produced twice; i.e. if the scheduler runs more than once before the outbox message
        // set is completed
        // or if we have multiple instances of the order service and the same  message is sent from all instances to the
        // same Kafka topic
        if (paymentOutboxMessageOptional.isEmpty()) {
            log.info("An outbox message with saga id {} is already processed!",
                    paymentResponse.sagaId());
            return;
        }

        OrderPaymentOutboxMessage orderPaymentOutboxMessage = paymentOutboxMessageOptional.get();
        OrderPaidEvent orderPaidEvent = completeOrderPayment(paymentResponse);

        SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(orderPaidEvent.getOrder().getStatus());
        OrderPaymentOutboxMessage updatedPaymentOutboxMessage = getUpdatedPaymentOutboxMessage(
                orderPaymentOutboxMessage,
                orderPaymentOutboxMessage.getOrderStatus(),
                sagaStatus
        );
        paymentOutboxHelper.save(updatedPaymentOutboxMessage);
        approvalOutboxHelper.saveApprovalOutboxMessage(
                orderDataMapper.orderPaidEventToOrderApprovalEventPayload(orderPaidEvent),
                orderPaidEvent.getOrder().getStatus(),
                sagaStatus,
                OutboxStatus.STARTED,
                UUID.fromString(paymentResponse.sagaId())
        );
        log.info("Order[id = {}] is paid", orderPaidEvent.getOrder().getId().getValue());
    }

    @Override
    @Transactional
    public void rollback(PaymentResponse paymentResponse) {
        Optional<OrderPaymentOutboxMessage> paymentOutboxMessageOptional = paymentOutboxHelper
                .getPaymentOutboxMessageBySagaIdAndSagaStatus(
                        UUID.fromString(paymentResponse.sagaId()),
                        getCurrentSagaStatus(paymentResponse.paymentStatus())
                );

        if (paymentOutboxMessageOptional.isEmpty()) {
            log.info("An outbox message with saga id {} is already rollbacked!", paymentResponse.sagaId());
            return;
        }
        OrderPaymentOutboxMessage orderPaymentOutboxMessage = paymentOutboxMessageOptional.get();
        Order order = rollbackOrderPayment(paymentResponse);
        SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(order.getStatus());
        OrderPaymentOutboxMessage updatedPaymentOutboxMessage = getUpdatedPaymentOutboxMessage(
                orderPaymentOutboxMessage,
                order.getStatus(),
                sagaStatus
        );
        paymentOutboxHelper.save(updatedPaymentOutboxMessage);
        if (paymentResponse.paymentStatus() == PaymentStatus.CANCELLED) {
            OrderApprovalOutboxMessage updatedApprovalOutboxMessage = getUpdatedApprovalOutboxMessage(
                    paymentResponse.sagaId(),
                    order.getStatus(),
                    sagaStatus
            );
            approvalOutboxHelper.save(updatedApprovalOutboxMessage);
        }
        log.info("Order[id = {}] is cancelled", order.getId().getValue());
    }

    private OrderPaymentOutboxMessage getUpdatedPaymentOutboxMessage(OrderPaymentOutboxMessage orderPaymentOutboxMessage,
                                                                     OrderStatus orderStatus,
                                                                     SagaStatus sagaStatus) {
        orderPaymentOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(DomainConstants.UTC)));
        orderPaymentOutboxMessage.setOrderStatus(orderStatus);
        orderPaymentOutboxMessage.setSagaStatus(sagaStatus);
        return orderPaymentOutboxMessage;
    }

    private OrderApprovalOutboxMessage getUpdatedApprovalOutboxMessage(String sagaId,
                                                                       OrderStatus orderStatus,
                                                                       SagaStatus sagaStatus) {
        Optional<OrderApprovalOutboxMessage> orderApprovalOutboxMessageOptional = approvalOutboxHelper
                .getApprovalOutboxMessageBySagaIdAndSagaStatus(
                        UUID.fromString(sagaId),
                        SagaStatus.COMPENSATING
                );
        if (orderApprovalOutboxMessageOptional.isEmpty()) {
            throw new OrderDomainException(String.format("Approval outbox message[sagaId = %s] could not be found" +
                    "in %s status", sagaId, SagaStatus.COMPENSATING.name()));
        }

        OrderApprovalOutboxMessage orderApprovalOutboxMessage = orderApprovalOutboxMessageOptional.get();
        orderApprovalOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(DomainConstants.UTC)));
        orderApprovalOutboxMessage.setOrderStatus(orderStatus);
        orderApprovalOutboxMessage.setSagaStatus(sagaStatus);
        return orderApprovalOutboxMessage;
    }

    private OrderPaidEvent completeOrderPayment(PaymentResponse paymentResponse) {
        log.info("Completing payment for order[id = {}]", paymentResponse.orderId());
        Order order = orderSagaHelper.findOrder(paymentResponse.orderId());
        OrderPaidEvent orderPaidEvent = orderDomainService.payOrder(order);
        orderSagaHelper.saveOrder(order);
        return orderPaidEvent;
    }

    private SagaStatus[] getCurrentSagaStatus(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case COMPLETED -> new SagaStatus[]{SagaStatus.STARTED};
            case CANCELLED -> new SagaStatus[]{SagaStatus.PROCESSING};
            case FAILED -> new SagaStatus[]{SagaStatus.STARTED, SagaStatus.PROCESSING};
        };
    }

    private Order rollbackOrderPayment(PaymentResponse paymentResponse) {
        log.info("Cancelling order with id = {}", paymentResponse.orderId());
        Order order = orderSagaHelper.findOrder(paymentResponse.orderId());
        // void as we have no the previous step
        orderDomainService.cancelOrder(order, paymentResponse.failureMessages());
        orderSagaHelper.saveOrder(order);
        return order;
    }
}
