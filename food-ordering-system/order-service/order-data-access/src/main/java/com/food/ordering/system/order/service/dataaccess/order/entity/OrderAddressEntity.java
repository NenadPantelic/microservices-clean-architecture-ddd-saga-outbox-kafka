package com.food.ordering.system.order.service.dataaccess.order.entity;

import com.food.ordering.system.domain.valueobject.OrderStatus;
import lombok.*;

import javax.persistence.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id"})
@Table(name = "order_address")
@Entity
public class OrderAddressEntity {

    @Id
    private UUID id;

    @OneToOne(cascade = CascadeType.ALL) // when an order is deleted, its address will be too
    @JoinColumn(name = "ORDER_ID")
    // the same name as the column name in OrderEntity
    private OrderEntity order;

    private String street;
    private String postalCode;
    private String city;
}
