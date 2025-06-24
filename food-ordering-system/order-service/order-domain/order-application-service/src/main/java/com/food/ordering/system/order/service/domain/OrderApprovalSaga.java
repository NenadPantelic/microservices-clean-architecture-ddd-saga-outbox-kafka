package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.DomainConstants;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.order.service.domain.dto.message.RestaurantApprovalResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.event.OrderCancelledEvent;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentEventPayload;
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

@Slf4j
@Component
public class OrderApprovalSaga implements SagaStep<RestaurantApprovalResponse> {


    private final OrderDomainService orderDomainService;
    private final OrderSagaHelper orderSagaHelper;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final ApprovalOutboxHelper approvalOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    public OrderApprovalSaga(OrderDomainService orderDomainService,
                             OrderSagaHelper orderSagaHelper,
                             PaymentOutboxHelper paymentOutboxHelper,
                             ApprovalOutboxHelper approvalOutboxHelper,
                             OrderDataMapper orderDataMapper) {
        this.orderDomainService = orderDomainService;
        this.orderSagaHelper = orderSagaHelper;
        this.paymentOutboxHelper = paymentOutboxHelper;
        this.approvalOutboxHelper = approvalOutboxHelper;
        this.orderDataMapper = orderDataMapper;
    }

    @Override
    @Transactional // local transaction
    public void process(RestaurantApprovalResponse restaurantApprovalResponse) {
        String sagaId = restaurantApprovalResponse.sagaId();
        Optional<OrderApprovalOutboxMessage> orderApprovalSagaOptional = approvalOutboxHelper
                .getApprovalOutboxMessageBySagaIdAndSagaStatus(
                        UUID.fromString(sagaId),
                        // query by this status, since the OrderPaymentSaga::process will transform the OrderPaidEvent
                        // to PROCESSING state; check OrderSagaHelper::orderStatusToSagaStatus
                        SagaStatus.PROCESSING
                );
        if (orderApprovalSagaOptional.isEmpty()) {
            log.info("An outbox message with sagaId {} is already processed", sagaId);
            return;
        }

        OrderApprovalOutboxMessage orderApprovalOutboxMessage = orderApprovalSagaOptional.get();
        Order order = approveOrder(restaurantApprovalResponse);
        SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(order.getStatus());

        approvalOutboxHelper.save(getUpdatedApprovalOutboxMessage(
                orderApprovalOutboxMessage, order.getStatus(), sagaStatus
        ));
        paymentOutboxHelper.save(getUpdatedPaymentOutboxMessage(
                sagaId, order.getStatus(), sagaStatus
        ));
        log.info("Order[id = {}] is approved", order.getId().getValue());
    }

    @Override
    @Transactional // local transaction
    public void rollback(RestaurantApprovalResponse restaurantApprovalResponse) {
        String sagaId = restaurantApprovalResponse.sagaId();
        Optional<OrderApprovalOutboxMessage> orderApprovalOutboxMessageOptional = approvalOutboxHelper
                .getApprovalOutboxMessageBySagaIdAndSagaStatus(
                        UUID.fromString(sagaId),
                        SagaStatus.PROCESSING
                );
        if (orderApprovalOutboxMessageOptional.isEmpty()) {
            log.info("An outbox message with sagaId {} is already rollbacked", sagaId);
            return;
        }
        OrderApprovalOutboxMessage orderApprovalOutboxMessage = orderApprovalOutboxMessageOptional.get();
        OrderCancelledEvent orderCancelledEvent = cancelOrder(restaurantApprovalResponse);
        SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(orderCancelledEvent.getOrder().getStatus());
        approvalOutboxHelper.save(getUpdatedApprovalOutboxMessage(
                orderApprovalOutboxMessage, orderCancelledEvent.getOrder().getStatus(), sagaStatus
        ));
        OrderPaymentEventPayload orderPaymentEventPayload = orderDataMapper.orderCancelledEventToOrderPaymentEventPayload(
                orderCancelledEvent
        );
        paymentOutboxHelper.savePaymentOutboxMessage(
                orderPaymentEventPayload,
                orderCancelledEvent.getOrder().getStatus(),
                sagaStatus,
                OutboxStatus.STARTED, // new object
                UUID.fromString(restaurantApprovalResponse.sagaId())
        );

        // duplicates are not possible
        // 1. we have an optimistic locking when the outbox message is updated
        // 2. unique index - payment outbox table and approval outbox table
        log.info("Order[id = {}] is cancelled", orderCancelledEvent.getOrder().getId().getValue());
    }

    private Order approveOrder(RestaurantApprovalResponse restaurantApprovalResponse) {
        log.info("Approving an order with id {}", restaurantApprovalResponse.orderId());
        Order order = orderSagaHelper.findOrder(restaurantApprovalResponse.orderId());
        orderDomainService.approveOrder(order); // final step, there is no next event
        orderSagaHelper.saveOrder(order);
        return order;
    }

    private OrderCancelledEvent cancelOrder(RestaurantApprovalResponse restaurantApprovalResponse) {
        log.info("Cancelling an order with id {}", restaurantApprovalResponse.orderId());
        Order order = orderSagaHelper.findOrder(restaurantApprovalResponse.orderId());
        return orderDomainService.cancelOrderPayment(
                order, restaurantApprovalResponse.failureMessages()
        );
    }

    private OrderApprovalOutboxMessage getUpdatedApprovalOutboxMessage(OrderApprovalOutboxMessage orderApprovalOutboxMessage,
                                                                       OrderStatus orderStatus,
                                                                       SagaStatus sagaStatus) {
        orderApprovalOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(DomainConstants.UTC)));
        orderApprovalOutboxMessage.setOrderStatus(orderStatus);
        orderApprovalOutboxMessage.setSagaStatus(sagaStatus);
        return orderApprovalOutboxMessage;
    }

    private OrderPaymentOutboxMessage getUpdatedPaymentOutboxMessage(String sagaId,
                                                                     OrderStatus status,
                                                                     SagaStatus sagaStatus) {
        Optional<OrderPaymentOutboxMessage> orderPaymentOutboxMessageOptional = paymentOutboxHelper
                .getPaymentOutboxMessageBySagaIdAndSagaStatus(
                        UUID.fromString(sagaId),
                        SagaStatus.COMPENSATING
                );
        if (orderPaymentOutboxMessageOptional.isEmpty()) {
            throw new OrderDomainException(String.format("Payment outbox message[sagaId = %s] could not be found" +
                    "in %s status", sagaId, SagaStatus.COMPENSATING.name()));
        }

        OrderPaymentOutboxMessage orderPaymentOutboxMessage = orderPaymentOutboxMessageOptional.get();
        orderPaymentOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(DomainConstants.UTC)));
        orderPaymentOutboxMessage.setOrderStatus(status);
        orderPaymentOutboxMessage.setSagaStatus(sagaStatus);
        return orderPaymentOutboxMessage;
    }
}
