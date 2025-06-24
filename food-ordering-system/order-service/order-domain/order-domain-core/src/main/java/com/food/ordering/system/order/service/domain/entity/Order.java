package com.food.ordering.system.order.service.domain.entity;

import com.food.ordering.system.domain.entity.AggregateRoot;
import com.food.ordering.system.domain.valueobject.*;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.valueobject.OrderItemId;
import com.food.ordering.system.order.service.domain.valueobject.StreetAddress;
import com.food.ordering.system.order.service.domain.valueobject.TrackingId;

import java.util.List;
import java.util.UUID;

// OrderId will be inherited as id in Order class
public class Order extends AggregateRoot<OrderId> {

    private final CustomerId customerId;
    private final RestaurantId restaurantId;
    private final StreetAddress deliveryAddress;
    private final Money price;
    private final List<OrderItem> items;

    // updatable fields
    private TrackingId trackingId;
    private OrderStatus status;
    private List<String> failureMessages;

    public void initializeOrder() {
        setId(new OrderId(UUID.randomUUID()));
        trackingId = new TrackingId(UUID.randomUUID());
        status = OrderStatus.PENDING;
        initializeOrderItems();
    }

    public void validateOrder() {
        validateInitialOrder();
        validateTotalPrice();
        validateItemsPrice();
    }

    public void pay() {
        if (status != OrderStatus.PENDING) {
            throw new OrderDomainException("Order is not in correct state for pay operation");
        }

        status = OrderStatus.PAID;
    }

    public void approve() {
        if (status != OrderStatus.PAID) {
            throw new OrderDomainException("Order is not in correct state for approve operation");
        }

        status = OrderStatus.APPROVED;
    }

    public void initCancel(List<String> failureMessages) {
        if (status != OrderStatus.PAID) {
            throw new OrderDomainException("Order is not in correct state for initCancel operation");
        }

        status = OrderStatus.CANCELLING;
        updateFailureMessages(failureMessages);
    }


    public void cancel(List<String> failureMessages) {
        if (!(status == OrderStatus.CANCELLING || status == OrderStatus.PENDING)) {
            throw new OrderDomainException("Order is not in correct state for cancel operation");
        }

        status = OrderStatus.CANCELLED;
        updateFailureMessages(failureMessages);
    }

    private void validateInitialOrder() {
        // since it is an initial order, these fields must not be set prior
        // to that
        if (status != null && getId() != null) {
            throw new OrderDomainException("Order is not in correct state for initialization!");
        }
    }

    private void validateTotalPrice() {
        if (price == null || !price.isGreaterThanZero()) {
            throw new OrderDomainException("Total price must be greater than zero!");
        }
    }

    private void validateItemsPrice() {
        Money orderItemsTotalPrice = items.stream()
                .map(orderItem -> {
                    validateItemPrice(orderItem);
                    return orderItem.getSubtotal();
                }).reduce(Money.ZERO, Money::add);

        if (!price.equals(orderItemsTotalPrice)) {
            throw new OrderDomainException(String.format(
                    "Total price %.2f is not equal to order items total price %.2f",
                    price.amount(), orderItemsTotalPrice.amount())
            );
        }
    }

    private void validateItemPrice(OrderItem orderItem) {
        if (!orderItem.isPriceValid()) {
            throw new OrderDomainException(String.format(
                    "Order item price %.2f is not valid for product %s",
                    orderItem.getPrice().amount(), orderItem.getProduct().getId().getValue())
            );
        }
    }

    private void initializeOrderItems() {
        long itemId = 1;
        for (OrderItem orderItem : items) {
            orderItem.initializeOrderItem(super.getId(), new OrderItemId(itemId++));
        }
    }

    private void updateFailureMessages(List<String> failureMessages) {
        if (this.failureMessages == null) {
            this.failureMessages = failureMessages;
            return;
        }

        if (failureMessages != null) {
            this.failureMessages.addAll(failureMessages.stream()
                    .filter(failureMessage -> !failureMessage.isEmpty())
                    .toList());
        }
    }

    private Order(Builder builder) {
        super.setId(builder.id);
        customerId = builder.customerId;
        restaurantId = builder.restaurantId;
        deliveryAddress = builder.deliveryAddress;
        price = builder.price;
        items = builder.items;
        trackingId = builder.trackingId;
        status = builder.orderStatus;
        failureMessages = builder.failureMessages;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public RestaurantId getRestaurantId() {
        return restaurantId;
    }

    public StreetAddress getDeliveryAddress() {
        return deliveryAddress;
    }

    public Money getPrice() {
        return price;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public TrackingId getTrackingId() {
        return trackingId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<String> getFailureMessages() {
        return failureMessages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private OrderId id;
        private CustomerId customerId;
        private RestaurantId restaurantId;
        private StreetAddress deliveryAddress;
        private Money price;
        private List<OrderItem> items;
        private TrackingId trackingId;
        private OrderStatus orderStatus;
        private List<String> failureMessages;

        private Builder() {
        }

        public Builder id(OrderId val) {
            id = val;
            return this;
        }

        public Builder customerId(CustomerId val) {
            customerId = val;
            return this;
        }

        public Builder restaurantId(RestaurantId val) {
            restaurantId = val;
            return this;
        }

        public Builder deliveryAddress(StreetAddress val) {
            deliveryAddress = val;
            return this;
        }

        public Builder price(Money val) {
            price = val;
            return this;
        }

        public Builder items(List<OrderItem> val) {
            items = val;
            return this;
        }

        public Builder trackingId(TrackingId val) {
            trackingId = val;
            return this;
        }

        public Builder orderStatus(OrderStatus val) {
            orderStatus = val;
            return this;
        }

        public Builder failureMessages(List<String> val) {
            failureMessages = val;
            return this;
        }

        public Order build() {
            return new Order(this);
        }
    }
}
