package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.event.EmptyEvent;
import com.food.ordering.system.order.service.domain.dto.message.RestaurantApprovalResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.event.OrderCancelledEvent;
import com.food.ordering.system.order.service.domain.port.output.message.publisher.payment.OrderCancelledPaymentRequestMessagePublisher;
import com.food.ordering.system.saga.SagaStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class OrderApprovalSaga implements SagaStep<RestaurantApprovalResponse, EmptyEvent, OrderCancelledEvent> {


    private final OrderDomainService orderDomainService;
    private final OrderSagaHelper orderSagaHelper;
    private final OrderCancelledPaymentRequestMessagePublisher orderCancelledPaymentRequestMessagePublisher;

    public OrderApprovalSaga(OrderDomainService orderDomainService,
                             OrderSagaHelper orderSagaHelper,
                             OrderCancelledPaymentRequestMessagePublisher orderCancelledPaymentRequestMessagePublisher) {
        this.orderDomainService = orderDomainService;
        this.orderSagaHelper = orderSagaHelper;
        this.orderCancelledPaymentRequestMessagePublisher = orderCancelledPaymentRequestMessagePublisher;
    }

    @Override
    @Transactional // local transaction
    public EmptyEvent process(RestaurantApprovalResponse restaurantApprovalResponse) {
        log.info("Approving an order with id {}", restaurantApprovalResponse.orderId());
        Order order = orderSagaHelper.findOrder(restaurantApprovalResponse.orderId());
        orderDomainService.approveOrder(order); // final step, there is no next event
        orderSagaHelper.saveOrder(order);
        log.info("Order[id = {}] is approved", order.getId().getValue());
        return EmptyEvent.INSTANCE;
    }

    @Override
    @Transactional // local transaction
    public OrderCancelledEvent rollback(RestaurantApprovalResponse restaurantApprovalResponse) {
        log.info("Cancelling an order with id {}", restaurantApprovalResponse.orderId());
        Order order = orderSagaHelper.findOrder(restaurantApprovalResponse.orderId());
        OrderCancelledEvent orderCancelledEvent = orderDomainService.cancelOrderPayment(
                order, restaurantApprovalResponse.failureMessages(), orderCancelledPaymentRequestMessagePublisher
        );
        orderSagaHelper.saveOrder(order);
        log.info("Order[id = {}] is cancelled", order.getId().getValue());
        return orderCancelledEvent;
    }
}
