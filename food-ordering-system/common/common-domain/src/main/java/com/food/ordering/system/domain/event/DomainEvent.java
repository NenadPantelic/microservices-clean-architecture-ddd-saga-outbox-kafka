package com.food.ordering.system.domain.event;

// generic will mark an event object with the type
// of the entity that will fire that event
// e.g. if the event type is OrderCreatedEvent, this generic will be Order
public interface DomainEvent<T> {

    void fire();
}
