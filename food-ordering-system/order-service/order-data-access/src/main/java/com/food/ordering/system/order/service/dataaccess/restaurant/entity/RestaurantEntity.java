package com.food.ordering.system.order.service.dataaccess.restaurant.entity;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(RestaurantEntityId.class)
@EqualsAndHashCode(of = {"id", "productId"})
@Table(name = "order_restaurant_m_view", schema = "restaurant") // materialized view
@Entity
public class RestaurantEntity {

    @Id
    private UUID id;
    @Id
    private UUID productId;
    private String name;
    private Boolean restaurantActive;
    private String productName;
    private BigDecimal productPrice;

}
