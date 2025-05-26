package com.food.ordering.system.restaurant.service.domain;

import com.food.ordering.system.domain.DomainConstants;
import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.domain.valueobject.OrderApprovalStatus;
import com.food.ordering.system.restaurant.service.domain.entity.Restaurant;
import com.food.ordering.system.restaurant.service.domain.event.OrderApprovalEvent;
import com.food.ordering.system.restaurant.service.domain.event.OrderApprovedEvent;
import com.food.ordering.system.restaurant.service.domain.event.OrderRejectedEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
public class RestaurantDomainServiceImpl implements RestaurantDomainService {

    private final DomainEventPublisher<OrderApprovedEvent> orderApprovedEventDomainEventPublisher;
    private final DomainEventPublisher<OrderRejectedEvent> orderRejectedEventDomainEventPublisher;

    public RestaurantDomainServiceImpl(DomainEventPublisher<OrderApprovedEvent> orderApprovedEventDomainEventPublisher,
                                       DomainEventPublisher<OrderRejectedEvent> orderRejectedEventDomainEventPublisher) {
        this.orderApprovedEventDomainEventPublisher = orderApprovedEventDomainEventPublisher;
        this.orderRejectedEventDomainEventPublisher = orderRejectedEventDomainEventPublisher;
    }

    @Override
    public OrderApprovalEvent validateOrder(Restaurant restaurant,
                                            List<String> failureMessages,
                                            DomainEventPublisher<OrderApprovedEvent> orderApprovedEventDomainEventPublisher,
                                            DomainEventPublisher<OrderRejectedEvent> orderRejectedEventDomainEventPublisher) {
        restaurant.validateOrder(failureMessages);
        UUID orderId = restaurant.getOrderDetail().getId().getValue();
        log.info("Validating order with id {}", orderId);

        if (failureMessages.isEmpty()) {
            log.info("Order[id = {}] approved", orderId);
            restaurant.constructOrderApproval(OrderApprovalStatus.APPROVED);
            return new OrderApprovedEvent(
                    restaurant.getOrderApproval(),
                    restaurant.getId(),
                    failureMessages,
                    currentUtcTime(),
                    orderApprovedEventDomainEventPublisher
            );
        }

        log.info("Order[id = {}] rejected", orderId);
        restaurant.constructOrderApproval(OrderApprovalStatus.REJECTED);

        return new OrderRejectedEvent(
                restaurant.getOrderApproval(),
                restaurant.getId(),
                failureMessages,
                currentUtcTime(),
                orderRejectedEventDomainEventPublisher
        );
    }

    private ZonedDateTime currentUtcTime() {
        return ZonedDateTime.now(ZoneId.of(DomainConstants.UTC));
    }
}
