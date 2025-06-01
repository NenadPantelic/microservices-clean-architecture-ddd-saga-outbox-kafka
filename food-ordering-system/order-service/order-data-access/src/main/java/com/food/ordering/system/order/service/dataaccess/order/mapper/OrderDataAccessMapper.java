package com.food.ordering.system.order.service.dataaccess.order.mapper;

import com.food.ordering.system.domain.valueobject.*;
import com.food.ordering.system.order.service.dataaccess.order.entity.OrderAddressEntity;
import com.food.ordering.system.order.service.dataaccess.order.entity.OrderEntity;
import com.food.ordering.system.order.service.dataaccess.order.entity.OrderItemEntity;
import com.food.ordering.system.order.service.domain.constant.MessageConstant;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.OrderItem;
import com.food.ordering.system.order.service.domain.entity.Product;
import com.food.ordering.system.order.service.domain.valueobject.OrderItemId;
import com.food.ordering.system.order.service.domain.valueobject.StreetAddress;
import com.food.ordering.system.order.service.domain.valueobject.TrackingId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderDataAccessMapper {


    public OrderEntity orderToOrderEntity(Order order) {
        List<String> failureMessages = order.getFailureMessages();
        OrderEntity orderEntity = OrderEntity.builder()
                .id(order.getId().getValue())
                .customerId(order.getCustomerId().getValue())
                .restaurantId(order.getRestaurantId().getValue())
                .trackingId(order.getTrackingId().getValue())
                .address(deliveryAddressToAddressEntity(order.getDeliveryAddress()))
                .price(order.getPrice().amount())
                .items(orderItemsToOrderItemEntities(order.getItems()))
                .failureMessages(failureMessages != null ?
                        String.join(MessageConstant.FAILURE_MESSAGE_DELIMITER, failureMessages) : "")
                .build();

        orderEntity.getAddress().setOrder(orderEntity);
        orderEntity.getItems().forEach(item -> item.setOrder(orderEntity));

        return orderEntity;
    }

    public Order orderEntityToOrder(OrderEntity orderEntity) {
        return Order.builder()
                .id(new OrderId(orderEntity.getId()))
                .customerId(new CustomerId(orderEntity.getCustomerId()))
                .restaurantId(new RestaurantId(orderEntity.getRestaurantId()))
                .deliveryAddress(addressEntityToDeliveryAddress(orderEntity.getAddress()))
                .price(new Money(orderEntity.getPrice()))
                .items(orderItemEntitiesToOrderItems(orderEntity.getItems()))
                .trackingId(new TrackingId(orderEntity.getTrackingId()))
                .orderStatus(orderEntity.getOrderStatus())
                .failureMessages(new ArrayList<>(
                                // unmodifiable list -> convert to modifiable
                                Arrays.asList(
                                        orderEntity.getFailureMessages().split(MessageConstant.FAILURE_MESSAGE_DELIMITER))
                        )
                )
                .build();
    }

    private OrderAddressEntity deliveryAddressToAddressEntity(StreetAddress deliveryAddress) {
        return OrderAddressEntity.builder()
                .id(deliveryAddress.id())
                .street(deliveryAddress.street())
                .postalCode(deliveryAddress.postalCode())
                .city(deliveryAddress.city())
                .build();
    }

    private List<OrderItemEntity> orderItemsToOrderItemEntities(List<OrderItem> items) {
        return items.stream()
                .map(orderItem ->
                        OrderItemEntity.builder()
                                .id(orderItem.getId().getValue())
                                .productId(orderItem.getProduct().getId().getValue())
                                .price(orderItem.getPrice().amount())
                                .quantity(orderItem.getQuantity())
                                .subtotal(orderItem.getSubtotal().amount())
                                .build()
                ).collect(Collectors.toList());
    }

    private StreetAddress addressEntityToDeliveryAddress(OrderAddressEntity address) {
        return new StreetAddress(address.getId(), address.getStreet(), address.getPostalCode(), address.getCity());
    }

    private List<OrderItem> orderItemEntitiesToOrderItems(List<OrderItemEntity> items) {
        return items.stream()
                .map(item -> OrderItem.builder()
                        .id(new OrderItemId(item.getId()))
                        .orderId(new OrderId(item.getOrder().getId()))
                        .subtotal(new Money(item.getSubtotal()))
                        .quantity(item.getQuantity())
                        .price(new Money(item.getPrice()))
                        .product(new Product(new ProductId(item.getProductId())))
                        .build())
                .collect(Collectors.toList());
    }
}
