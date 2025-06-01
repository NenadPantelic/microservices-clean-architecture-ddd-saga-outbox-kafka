package com.food.ordering.system.domain.event;


// for ending operations in Saga
public final class EmptyEvent implements DomainEvent<Void> {

    public static final EmptyEvent INSTANCE = new EmptyEvent();

    private EmptyEvent() {

    }

    @Override
    public void fire() {
        // nothing to do
    }
}
