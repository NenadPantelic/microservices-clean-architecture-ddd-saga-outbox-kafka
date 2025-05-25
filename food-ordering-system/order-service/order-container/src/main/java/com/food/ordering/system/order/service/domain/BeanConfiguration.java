package com.food.ordering.system.order.service.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean // in domain-core we don't have any Spring dependency, so we have to define a Spring bean here
    public OrderDomainService orderDomainService() {
        return new OrderDomainServiceImpl(orderCreatedEventDomainEventPublisher, orderCancelledEventDomainEventPublisher, orderPaidEventDomainEventPublisher);
    }
}
