package com.food.ordering.system.order.service.messaging.listener.kafka;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.OrderApprovalStatus;
import com.food.ordering.system.kafka.order.avro.model.RestaurantApprovalResponseAvroModel;
import com.food.ordering.system.order.service.domain.dto.message.RestaurantApprovalResponse;
import com.food.ordering.system.order.service.domain.exception.OrderNotFoundException;
import com.food.ordering.system.order.service.domain.port.input.message.listener.restaurantapproval.RestaurantApprovalResponseMessageListener;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RestaurantApprovalKafkaListener implements KafkaConsumer<RestaurantApprovalResponseAvroModel> {

    // one of the input ports in the domain layer
    private final RestaurantApprovalResponseMessageListener restaurantApprovalResponseMessageListener;
    private final OrderMessagingDataMapper orderMessagingDataMapper;

    public RestaurantApprovalKafkaListener(RestaurantApprovalResponseMessageListener restaurantApprovalResponseMessageListener,
                                           OrderMessagingDataMapper orderMessagingDataMapper) {
        this.restaurantApprovalResponseMessageListener = restaurantApprovalResponseMessageListener;
        this.orderMessagingDataMapper = orderMessagingDataMapper;
    }

    @Override
    @KafkaListener(
            id = "${kafka-consumer-config.restaurant-approval-consumer-group-id}",
            topics = "${order-service.restaurant-approval-response-topic-name}"
    )
    public void receive(@Payload List<RestaurantApprovalResponseAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("{} restaurant approval responses received with keys = {}, partitions = {} and offsets = {}",
                messages.size(), keys, partitions, offsets);
        messages.forEach(restaurantApprovalResponseAvroModel -> {
            try {
                OrderApprovalStatus orderApprovalStatus = restaurantApprovalResponseAvroModel.getOrderApprovalStatus();
                RestaurantApprovalResponse restaurantApprovalResponse = orderMessagingDataMapper.approvalResponseAvroModelToApproveResponse(
                        restaurantApprovalResponseAvroModel
                );

                if (OrderApprovalStatus.APPROVED == orderApprovalStatus) {
                    log.info("Processing approved order[id = {}]", restaurantApprovalResponseAvroModel.getOrderId());
                    restaurantApprovalResponseMessageListener.orderApproved(restaurantApprovalResponse);
                } else if (OrderApprovalStatus.REJECTED == orderApprovalStatus) {
                    log.info("Processing rejected order[id = {}]", restaurantApprovalResponseAvroModel.getOrderId());
                    restaurantApprovalResponseMessageListener.orderRejected(restaurantApprovalResponse);
                }
            } catch (OptimisticLockingFailureException e) {
                // NO-OP for optimistic locking. This means another thread finished the work, do not throw an error to
                // prevent reading the data from Kafka again
                log.error("Caught an optimistic locking exception in RestaurantApprovalKafkaListener for orderId: {}",
                        restaurantApprovalResponseAvroModel.getOrderId());
            } catch (OrderNotFoundException e) {
                // NO-OP for OrderNotFoundException - order is not found, retrying won't help
                log.error("No order found for orderId: {}", restaurantApprovalResponseAvroModel.getOrderId());
            }
            // all other messages will be propagated, i.e. reading will fail and the listener will read again the message
            // from Kafka
        });
    }
}
