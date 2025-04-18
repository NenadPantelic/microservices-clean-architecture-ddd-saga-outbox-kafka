package com.food.ordering.system.order.service.domain.entity;

import com.food.ordering.system.domain.entity.BaseEntity;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.order.service.domain.valueobject.OrderItemId;

// do not want to add dependencies like Lombok
// we want to keep the core logic dependency-free (both entities and value objects)
public class OrderItem extends BaseEntity<OrderItemId> {

    private OrderId orderId; // updatable field
    private final Product product;
    private final Integer quantity;
    private final Money price;
    private final Money subTotal; // quantity * price

    private OrderItem(Builder builder) {
        super.setId(builder.id);
        product = builder.product;
        quantity = builder.quantity;
        price = builder.price;
        subTotal = builder.subTotal;
    }


    public OrderId getOrderId() {
        return orderId;
    }

    public Product getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Money getPrice() {
        return price;
    }

    public Money getSubTotal() {
        return subTotal;
    }

    void initializeOrderItem(OrderId orderId, OrderItemId orderItemId) {
        this.orderId = orderId;
        super.setId(orderItemId);
    }

    boolean isPriceValid() {
        return price.isGreaterThanZero() &&
                price.equals(product.getPrice()) &&
                price.multiply(quantity).equals(subTotal);
    }

    public static final class Builder {
        private OrderItemId id;
        private final Product product;
        private final Integer quantity;
        private final Money price;
        private final Money subTotal;

        private Builder(Product product, Integer quantity, Money price, Money subTotal) {
            this.product = product;
            this.quantity = quantity;
            this.price = price;
            this.subTotal = subTotal;
        }

        public static Builder builder(Product product, Integer quantity, Money price, Money subTotal) {
            return new Builder(product, quantity, price, subTotal);
        }

        public Builder id(OrderItemId val) {
            id = val;
            return this;
        }

        public OrderItem build() {
            return new OrderItem(this);
        }
    }
}
