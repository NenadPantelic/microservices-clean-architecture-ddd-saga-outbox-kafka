package com.food.ordering.system.order.service.domain;


import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.domain.ports.output.message.publisher.payment.OrderCreatedPaymentRequestMessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class OrderCreatedEventApplicationListener {

    private final OrderCreatedPaymentRequestMessagePublisher orderCreatedPaymentRequestPublisher;

    public OrderCreatedEventApplicationListener(OrderCreatedPaymentRequestMessagePublisher
                                                        orderCreatedPaymentRequestPublisher) {
        this.orderCreatedPaymentRequestPublisher = orderCreatedPaymentRequestPublisher;
    }

    // will only process when the OrderCreateCommandHandler::createOrder
    // is completed and the transaction is committed
    @TransactionalEventListener
    void process(OrderCreatedEvent orderCreatedEvent) {
        orderCreatedPaymentRequestPublisher.publish(orderCreatedEvent);
    }
}
