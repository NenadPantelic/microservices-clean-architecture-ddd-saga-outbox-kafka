package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.exception.OrderNotFoundException;
import com.food.ordering.system.order.service.domain.port.output.repository.OrderRepository;
import com.food.ordering.system.saga.SagaStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class OrderSagaHelper {

    private final OrderRepository orderRepository;

    public OrderSagaHelper(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    Order findOrder(String orderId) {
        return orderRepository
                .findById(
                        new OrderId(UUID.fromString(orderId))
                ).orElseThrow(
                        () -> {
                            String errMessage = String.format("Order[id = %s] could not be found!", orderId);
                            log.error(errMessage);
                            return new OrderNotFoundException(errMessage);
                        }
                );
    }

    void saveOrder(Order order) {
        orderRepository.save(order);
    }

    SagaStatus orderStatusToSagaStatus(OrderStatus orderStatus) {
        switch (orderStatus) {
            case PAID -> {
                return SagaStatus.PROCESSING;
            }
            case APPROVED -> {
                return SagaStatus.SUCCEEDED;
            }
            case CANCELLING -> {
                return SagaStatus.COMPENSATING;
            }
            case CANCELLED -> {
                return SagaStatus.COMPENSATED;
            }
            default -> {
                return SagaStatus.STARTED;
            }
        }
    }
}
