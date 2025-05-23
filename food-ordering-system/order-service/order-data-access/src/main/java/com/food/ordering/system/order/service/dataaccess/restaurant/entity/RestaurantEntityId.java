package com.food.ordering.system.order.service.dataaccess.restaurant.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RestaurantEntityId implements Serializable {

    private UUID restaurantId;
    private UUID productId;
}
