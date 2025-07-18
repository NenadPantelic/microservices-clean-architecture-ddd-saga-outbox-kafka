package com.food.ordering.system.order.service.messaging.publisher.kafka;

import com.food.ordering.system.kafka.order.avro.model.RestaurantApprovalRequestAvroModel;
import com.food.ordering.system.kafka.producer.KafkaMessageHelper;
import com.food.ordering.system.kafka.producer.service.KafkaProducer;
import com.food.ordering.system.order.service.domain.config.OrderServiceConfigData;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalEventPayload;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.order.service.domain.port.output.message.publisher.restaurantapproval.RestaurantApprovalRequestMessagePublisher;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.food.ordering.system.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@Component
public class OrderApprovalEventKafkaPublisher implements RestaurantApprovalRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final KafkaProducer<String, RestaurantApprovalRequestAvroModel> kafkaProducer;
    private final OrderServiceConfigData orderServiceConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;

    public OrderApprovalEventKafkaPublisher(OrderMessagingDataMapper orderMessagingDataMapper,
                                            KafkaProducer<String, RestaurantApprovalRequestAvroModel> kafkaProducer,
                                            OrderServiceConfigData orderServiceConfigData,
                                            KafkaMessageHelper kafkaMessageHelper) {
        this.orderMessagingDataMapper = orderMessagingDataMapper;
        this.kafkaProducer = kafkaProducer;
        this.orderServiceConfigData = orderServiceConfigData;
        this.kafkaMessageHelper = kafkaMessageHelper;
    }

    @Override
    public void publish(OrderApprovalOutboxMessage orderApprovalOutboxMessage,
                        BiConsumer<OrderApprovalOutboxMessage, OutboxStatus> outboxCallback) {

        OrderApprovalEventPayload orderApprovalEventPayload = kafkaMessageHelper.getOrderEventPayload(
                orderApprovalOutboxMessage.getPayload(), OrderApprovalEventPayload.class
        );
        String sagaId = orderApprovalOutboxMessage.getSagaId().toString();
        log.info("Received an OrderApprovalOutboxMessage for orderId = {} and sagaId = {}",
                orderApprovalEventPayload.getOrderId(),
                sagaId
        );

        try {
            RestaurantApprovalRequestAvroModel restaurantApprovalRequestAvroModel = orderMessagingDataMapper
                    .orderApprovalEventToRestaurantApprovalRequestAvroModel(
                            sagaId, orderApprovalEventPayload
                    );
            kafkaProducer.send(
                    orderServiceConfigData.getPaymentRequestTopicName(),
                    sagaId,
                    restaurantApprovalRequestAvroModel,
                    kafkaMessageHelper.getKafkaCallback(
                            orderServiceConfigData.getPaymentRequestTopicName(),
                            restaurantApprovalRequestAvroModel,
                            orderApprovalOutboxMessage,
                            outboxCallback,
                            orderApprovalEventPayload.getOrderId(),
                            "RestaurantApprovalRequestAvroModel"
                    )
            );

            log.info("OrderApprovalEventPayload sent to Kafka for orderId = {} and sagaId = {}",
                    orderApprovalEventPayload.getOrderId(), sagaId
            );
        } catch (Exception e) {
            log.error("Error while sending OrderApprovalEventPayload to Kafka with orderId = {} and sagaId = {}. Error: {}",
                    orderApprovalEventPayload.getOrderId(), sagaId, e.getMessage(), e
            );
        }
    }
}
