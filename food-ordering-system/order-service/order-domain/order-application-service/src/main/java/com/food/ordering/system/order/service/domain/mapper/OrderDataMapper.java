package com.food.ordering.system.order.service.domain.mapper;

import com.food.ordering.system.domain.valueobject.*;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderCommand;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderResponse;
import com.food.ordering.system.order.service.domain.dto.create.OrderAddress;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.OrderItem;
import com.food.ordering.system.order.service.domain.entity.Product;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.valueobject.StreetAddress;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderDataMapper {

    public Restaurant createOrderCommandToRestaurant(CreateOrderCommand createOrderCommand) {
        return Restaurant.builder()
                .id(new RestaurantId(createOrderCommand.restaurantId()))
                .active(true)
                .products(
                        createOrderCommand.items()
                                .stream()
                                .map(orderItem -> {
                                    ProductId productId = new ProductId(orderItem.productId());
                                    return new Product(productId);
                                })
                                .collect(Collectors.toList()))
                .build();
    }

    public Order createOrderCommandToOrder(CreateOrderCommand createOrderCommand) {
        return Order.builder()
                .customerId(new CustomerId(createOrderCommand.customerId()))
                .restaurantId(new RestaurantId(createOrderCommand.restaurantId()))
                .deliveryAddress(orderAddressToStreetAddress(createOrderCommand.address()))
                .price(new Money(createOrderCommand.price()))
                .items(orderItemsToOrderItemEntities(createOrderCommand.items()))
                .build();
    }

    public CreateOrderResponse orderToCreateOrderResponse(Order order) {
        return CreateOrderResponse.builder()
                .orderTrackingId(order.getTrackingId().getValue())
                .orderStatus(order.getOrderStatus())
                .build();
    }

    private StreetAddress orderAddressToStreetAddress(OrderAddress address) {
        return new StreetAddress(UUID.randomUUID(), address.street(), address.postalCode(), address.city());
    }

    private List<OrderItem> orderItemsToOrderItemEntities(
            List<com.food.ordering.system.order.service.domain.dto.create.OrderItem> items) {
        return items.stream()
                .map(item -> OrderItem.builder()
                        .product(new Product(new ProductId(item.productId())))
                        .price(new Money(item.price()))
                        .quantity(item.quantity())
                        .subtotal(new Money(item.subtotal()))
                        .build()
                )
                .collect(Collectors.toList());
    }
}
