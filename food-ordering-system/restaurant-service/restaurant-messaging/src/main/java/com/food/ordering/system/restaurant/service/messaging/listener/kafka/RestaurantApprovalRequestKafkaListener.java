package com.food.ordering.system.restaurant.service.messaging.listener.kafka;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.RestaurantApprovalRequestAvroModel;
import com.food.ordering.system.restaurant.service.domain.exception.RestaurantDomainException;
import com.food.ordering.system.restaurant.service.domain.exception.RestaurantNotFoundException;
import com.food.ordering.system.restaurant.service.domain.port.input.message.listener.RestaurantApprovalRequestMessageListener;
import com.food.ordering.system.restaurant.service.messaging.mapper.RestaurantMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLState;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@Component
public class RestaurantApprovalRequestKafkaListener implements KafkaConsumer<RestaurantApprovalRequestAvroModel> {

    private final RestaurantApprovalRequestMessageListener restaurantApprovalRequestMessageListener;
    private final RestaurantMessagingDataMapper restaurantMessagingDataMapper;

    public RestaurantApprovalRequestKafkaListener(RestaurantApprovalRequestMessageListener
                                                          restaurantApprovalRequestMessageListener,
                                                  RestaurantMessagingDataMapper
                                                          restaurantMessagingDataMapper) {
        this.restaurantApprovalRequestMessageListener = restaurantApprovalRequestMessageListener;
        this.restaurantMessagingDataMapper = restaurantMessagingDataMapper;
    }

    @Override
    @KafkaListener(id = "${kafka-consumer-config.restaurant-approval-consumer-group-id}",
            topics = "${restaurant-service.restaurant-approval-request-topic-name}")
    public void receive(@Payload List<RestaurantApprovalRequestAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("{} restaurant approval requests received with keys = {}, partitions = {}, and offsets = {}",
                messages.size(), keys, partitions, offsets
        );

        messages.forEach(restaurantApprovalRequestAvroModel -> {
            try {
                String orderId = restaurantApprovalRequestAvroModel.getOrderId();
                log.info("Processing an order approval for order id: {}", orderId);
                restaurantApprovalRequestMessageListener.approveOrder(restaurantMessagingDataMapper.
                        restaurantApprovalRequestAvroModelToRestaurantApproval(restaurantApprovalRequestAvroModel));
            } catch (DataAccessException e) {
                SQLException sqlException = (SQLException) e.getRootCause();
                if (sqlException != null && sqlException.getSQLState() != null &&
                        PSQLState.UNIQUE_VIOLATION.getState().equals(sqlException.getSQLState())) {
                    //NO-OP for unique constraint exception
                    log.error("Caught a unique constraint exception with sql state: {} in " +
                                    "RestaurantApprovalRequestKafkaListener for order id: {}",
                            sqlException.getSQLState(), restaurantApprovalRequestAvroModel.getOrderId());
                } else {
                    throw new RestaurantDomainException(
                            String.format(
                                    "Throwing DataAccessException in RestaurantApprovalRequestKafkaListener: %s",
                                    e.getMessage()
                            ), e
                    );
                }
            } catch (RestaurantNotFoundException e) {
                //NO-OP for RestaurantNotFoundException
                log.error("No restaurant found for restaurant id {} and order id {}",
                        restaurantApprovalRequestAvroModel.getRestaurantId(),
                        restaurantApprovalRequestAvroModel.getOrderId()
                );
            }
        });
    }
}