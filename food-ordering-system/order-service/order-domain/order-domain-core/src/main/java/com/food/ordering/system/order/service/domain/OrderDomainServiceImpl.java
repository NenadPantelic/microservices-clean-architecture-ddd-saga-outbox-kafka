package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.DomainConstants;
import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.Product;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.event.OrderCancelledEvent;
import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

// this (Application service) will be the first contact point of a client request
// it will drive the business logic; this could be treated like Use cases from Uncle Bob's
// Clean architecture

// the differences:
// Application service is exposed through an interface and has no business logic
// Use cases are not exposed
@Slf4j
public class OrderDomainServiceImpl implements OrderDomainService {

    private final DomainEventPublisher<OrderCreatedEvent> orderCreatedEventDomainEventPublisher;
    private final DomainEventPublisher<OrderCancelledEvent> orderCancelledEventDomainEventPublisher;
    private final DomainEventPublisher<OrderPaidEvent> orderPaidEventDomainEventPublisher;

    public OrderDomainServiceImpl(DomainEventPublisher<OrderCreatedEvent> orderCreatedEventDomainEventPublisher,
                                  DomainEventPublisher<OrderCancelledEvent> orderCancelledEventDomainEventPublisher,
                                  DomainEventPublisher<OrderPaidEvent> orderPaidEventDomainEventPublisher) {
        this.orderCreatedEventDomainEventPublisher = orderCreatedEventDomainEventPublisher;
        this.orderCancelledEventDomainEventPublisher = orderCancelledEventDomainEventPublisher;
        this.orderPaidEventDomainEventPublisher = orderPaidEventDomainEventPublisher;
    }

    @Override
    public OrderCreatedEvent validateAndInitiateOrder(Order order, Restaurant restaurant) {
        validateRestaurant(restaurant);
        setOrderProductInformation(order, restaurant);
        order.validateOrder();
        order.initializeOrder();
        log.info("Order with  id {} is initialized", order.getId());
        return new OrderCreatedEvent(order, utcTimeNow(), orderCreatedEventDomainEventPublisher);
    }

    @Override
    public OrderPaidEvent payOrder(Order order) {
        order.pay();
        log.info("Order[id = {}] is paid.", order.getId().getValue());
        return new OrderPaidEvent(order, utcTimeNow(), orderPaidEventDomainEventPublisher);
    }

    @Override
    public void approveOrder(Order order) {
        order.approve();
        log.info("Order[id = {}] is approved.", order.getId().getValue());
        // the client will fetch the order by tracking ID
    }

    @Override
    public OrderCancelledEvent cancelOrderPayment(Order order, List<String> failureMessages) {
        order.initCancel(failureMessages);
        log.info("Order[id = {}]'s payment is cancelled.", order.getId().getValue());
        return new OrderCancelledEvent(order, utcTimeNow(), orderCancelledEventDomainEventPublisher);
    }

    @Override
    public void cancelOrder(Order order, List<String> failureMessages) {
        order.cancel(failureMessages);
        log.info("Order[id = {}] is cancelled.", order.getId().getValue());
    }

    private void validateRestaurant(Restaurant restaurant) {
        if (!restaurant.isActive()) {
            throw new OrderDomainException(
                    String.format("Restaurant[id = %s] is not active", restaurant.getId().getValue())
            );
        }
    }

    private void setOrderProductInformation(Order order, Restaurant restaurant) {
        // O(n^2)
        // with hashmap, we can optimize it, not in the focus now
        order.getItems().forEach(orderItem -> {
            restaurant.getProducts().forEach(restaurantProduct -> {
                Product currentProduct = orderItem.getProduct();
                if (currentProduct.equals(restaurantProduct)) {
                    currentProduct.updateWithConfirmedNameAndPrice(
                            restaurantProduct.getName(),
                            restaurantProduct.getPrice()
                    );
                }
            });
        });
    }

    private ZonedDateTime utcTimeNow() {
        return ZonedDateTime.now(ZoneId.of(DomainConstants.UTC));
    }
}
