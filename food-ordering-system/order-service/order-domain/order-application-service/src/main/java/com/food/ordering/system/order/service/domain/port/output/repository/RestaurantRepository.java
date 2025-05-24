package com.food.ordering.system.order.service.domain.port.output.repository;

import com.food.ordering.system.order.service.domain.entity.Restaurant;

import java.util.Optional;

// output port in the domain layer
public interface RestaurantRepository {

    Optional<Restaurant> findRestaurantInformation(Restaurant restaurant);
}
