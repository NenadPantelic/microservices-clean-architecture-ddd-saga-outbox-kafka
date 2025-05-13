package com.food.ordering.system.order.service.dataaccess.order.entity;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id", "order"})
@IdClass(OrderItemEntityId.class) // so we can use this type as id with multi-column ID
@Table(name = "order_item")
@Entity
public class OrderItemEntity {

    @Id
    private Long id;

    @Id // to provide uniqueness
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "ORDER_ID")
    private OrderEntity order;

    private UUID productId;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}
