package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.event.OrderCancelledEvent;
import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;

import java.util.List;

// the event firing will be in the application service
// after the underlying business operation has been persisted
// if we first fire an event and then do the persistence we could end up having
// an incorrect event if the has gone wrong.
// Also, the domain layer should not know about how to fire the event,
// so the Application service should do that
// Where to create the event? Domain service or Entities.
// Domain entities are responsible for creating related events as they can be directly called from the
// application service because in DDD, using a domain service is not mandatory.
// Domain service is required if we have access to multiple aggregates in business logic, or we
// have some logic that doesn't fit into an entity class.
// An approach to follow (by experience): do not let the application service directly talk to entities,
// but rather put the domain service between them (in front of the domain).
public interface OrderDomainService {

    OrderCreatedEvent validateAndInitiateOrder(Order order, Restaurant restaurant);

    OrderPaidEvent payOrder(Order order);

    void approveOrder(Order order);

    OrderCancelledEvent cancelOrderPayment(Order order, List<String> failureMessages);

    void cancelOrder(Order order, List<String> failureMessages);
}
