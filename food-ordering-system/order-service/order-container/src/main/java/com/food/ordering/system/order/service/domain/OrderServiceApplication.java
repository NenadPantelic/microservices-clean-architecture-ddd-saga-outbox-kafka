package com.food.ordering.system.order.service.domain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = {"com.food.ordering.system.order.service.dataaccess", "com.food.ordering.system.dataaccess"}) // scan JPA repositorties
@EntityScan(basePackages = {"com.food.ordering.system.order.service.dataaccess", "com.food.ordering.system.dataaccess"}) // scan JPA entities
@SpringBootApplication(scanBasePackages = "com.food.ordering.system") // to scan all packages in all modules
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
