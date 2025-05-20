package com.food.ordering.system.order.service.messaging.listener.kafka;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.PaymentResponseAvroModel;
import com.food.ordering.system.kafka.order.avro.model.PaymentStatus;
import com.food.ordering.system.order.service.domain.dto.message.PaymentResponse;
import com.food.ordering.system.order.service.domain.ports.input.message.listener.payment.PaymentResponseMessageListener;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PaymentResponseKafkaListener implements KafkaConsumer<PaymentResponseAvroModel> {

    // one of the input ports in the domain layer
    private final PaymentResponseMessageListener paymentResponseMessageListener;
    private final OrderMessagingDataMapper orderMessagingDataMapper;

    public PaymentResponseKafkaListener(PaymentResponseMessageListener paymentResponseMessageListener,
                                        OrderMessagingDataMapper orderMessagingDataMapper) {
        this.paymentResponseMessageListener = paymentResponseMessageListener;
        this.orderMessagingDataMapper = orderMessagingDataMapper;
    }


    @Override
    @KafkaListener(
            id = "${kafka-consumer-config.payment-consumer-group-id}",
            topics = "${order-service.payment-response-topic-name}"
    )
    public void receive(@Payload List<PaymentResponseAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("{} payment responses received with keys = {}, partitions = {} and offsets = {}",
                messages.size(), keys, partitions, offsets);
        messages.forEach(paymentResponseAvroModel -> {
            PaymentStatus paymentStatus = paymentResponseAvroModel.getPaymentStatus();
            PaymentResponse paymentResponse = orderMessagingDataMapper.paymentResponseAvroModelToPaymentResponse(
                    paymentResponseAvroModel
            );
            if (PaymentStatus.COMPLETED == paymentStatus) {
                log.info("Processing successful payment for order[id = {}]", paymentResponseAvroModel.getOrderId());
                paymentResponseMessageListener.paymentCompleted(paymentResponse);
            } else if (PaymentStatus.CANCELLED == paymentStatus || PaymentStatus.FAILED == paymentStatus) {
                log.info("Processing unsuccessful payment order[id = {}]", paymentResponseAvroModel.getOrderId());
                paymentResponseMessageListener.paymentCancelled(paymentResponse);
            }
        });
    }
}
