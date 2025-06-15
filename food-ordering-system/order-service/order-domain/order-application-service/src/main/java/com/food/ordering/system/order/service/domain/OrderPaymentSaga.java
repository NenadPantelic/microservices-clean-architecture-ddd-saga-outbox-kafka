package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.event.EmptyEvent;
import com.food.ordering.system.order.service.domain.dto.message.PaymentResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;
import com.food.ordering.system.saga.SagaStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Order service will be the coordinator of Saga, all Saga code will be here
@Slf4j
@Component
// PaymentResponse will be called when getting a response from PaymentService
// OrderPaidEvent - when the payment is completed, we create this event (if everything went well)
// EmptyEvent - since this is a first step (first we should pay), in case something goes wrong we should just update
// status in a local database
public class OrderPaymentSaga implements SagaStep<PaymentResponse, OrderPaidEvent, EmptyEvent> {


    private final OrderDomainService orderDomainService;
    private final OrderSagaHelper orderSagaHelper;
    private final OrderPaidRestaurantRequestMessagePublisher orderPaidRestaurantRequestMessagePublisher;

    public OrderPaymentSaga(OrderDomainService orderDomainService,
                            OrderSagaHelper orderSagaHelper,
                            OrderPaidRestaurantRequestMessagePublisher orderPaidRestaurantRequestMessagePublisher) {
        this.orderDomainService = orderDomainService;
        this.orderSagaHelper = orderSagaHelper;
        this.orderPaidRestaurantRequestMessagePublisher = orderPaidRestaurantRequestMessagePublisher;
    }

    @Override
    @Transactional
    public OrderPaidEvent process(PaymentResponse paymentResponse) {
        log.info("Completing payment for order[id = {}]", paymentResponse.orderId());
        Order order = orderSagaHelper.findOrder(paymentResponse.orderId());
        OrderPaidEvent orderPaidEvent = orderDomainService.payOrder(
                order, orderPaidRestaurantRequestMessagePublisher
        );
        orderSagaHelper.saveOrder(order);
        log.info("Order[id = {}] is paid", order.getId().getValue());
        return orderPaidEvent;
    }

    @Override
    @Transactional
    public EmptyEvent rollback(PaymentResponse paymentResponse) {
        log.info("Cancelling order with id = {}", paymentResponse.orderId());
        Order order = orderSagaHelper.findOrder(paymentResponse.orderId());
        orderDomainService.cancelOrder(order, paymentResponse.failureMessages()); // void as we have no the previous
        // step
        orderSagaHelper.saveOrder(order);
        log.info("Order[id = {}] is cancelled", order.getId().getValue());
        return EmptyEvent.INSTANCE;
    }
}
