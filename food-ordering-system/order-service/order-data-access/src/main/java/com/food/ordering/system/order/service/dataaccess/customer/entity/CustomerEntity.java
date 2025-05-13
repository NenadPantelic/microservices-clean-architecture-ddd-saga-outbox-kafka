package com.food.ordering.system.order.service.dataaccess.customer.entity;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "order_customer_m_view", schema = "customer") // materialized view
@Entity
public class CustomerEntity {

    @Id
    private UUID id; // just to check if the customer exists
}
