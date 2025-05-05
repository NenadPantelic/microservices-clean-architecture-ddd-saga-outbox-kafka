package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.order.service.domain.dto.create.CreateOrderCommand;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.ports.output.repository.CustomerRepository;
import com.food.ordering.system.order.service.domain.ports.output.repository.OrderRepository;
import com.food.ordering.system.order.service.domain.ports.output.repository.RestaurantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
public class OrderCreateCommandHandler {

    private final OrderDomainService orderDomainService;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderDataMapper orderDataMapper;
    private final ApplicationDomainEventPublisher applicationDomainEventPublisher;

    public OrderCreateCommandHandler(OrderDomainService orderDomainService,
                                     OrderRepository orderRepository,
                                     CustomerRepository customerRepository,
                                     RestaurantRepository restaurantRepository,
                                     OrderDataMapper orderDataMapper,
                                     ApplicationDomainEventPublisher applicationDomainEventPublisher) {
        this.orderDomainService = orderDomainService;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderDataMapper = orderDataMapper;
        this.applicationDomainEventPublisher = applicationDomainEventPublisher;
    }


    @Transactional
    public CreateOrderResponse createOrder(CreateOrderCommand createOrderCommand) {
        checkCustomer(createOrderCommand.customerId());
        Restaurant restaurant = checkRestaurant(createOrderCommand);
        Order order = orderDataMapper.createOrderCommandToOrder(createOrderCommand);
        OrderCreatedEvent orderCreatedEvent = orderDomainService.validateAndInitiateOrder(order, restaurant);
        Order savedOrder = saveOrder(order);
        log.info("Order created: {}", savedOrder);
        applicationDomainEventPublisher.publish(orderCreatedEvent);
        return orderDataMapper.orderToCreateOrderResponse(savedOrder);
    }

    private void checkCustomer(UUID customerId) {
        // if we had to more business check with the customer
        // we would pass it to the domain service and do these checks there
        customerRepository.findById(customerId).orElseThrow(() -> {
            String errMessage = String.format("Customer[id = %s] not found.", customerId);
            log.warn(errMessage);
            return new OrderDomainException(errMessage);
        });
    }

    private Restaurant checkRestaurant(CreateOrderCommand createOrderCommand) {
        Restaurant restaurant = orderDataMapper.createOrderCommandToRestaurant(createOrderCommand);
        return restaurantRepository.findRestaurantInformation(restaurant).orElseThrow(() -> {
            String errMessage = String.format("Restaurant[id = %s] not found.", restaurant.getId());
            log.warn(errMessage);
            return new OrderDomainException(errMessage);
        });
    }

    private Order saveOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        if (savedOrder == null) {
            String errMessage = String.format("Could not save order: %s", order);
            log.error(errMessage);
            throw new OrderDomainException(errMessage);
        }

        log.info("Order is saved with id {}", savedOrder.getId().getValue());
        return savedOrder;
    }

}
