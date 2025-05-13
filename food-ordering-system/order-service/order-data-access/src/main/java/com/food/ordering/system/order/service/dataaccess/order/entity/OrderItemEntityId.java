package com.food.ordering.system.order.service.dataaccess.order.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class OrderItemEntityId implements Serializable { // identifier class must be serialized when persisting the entity

    private Long id;
    private OrderEntity order;
}
