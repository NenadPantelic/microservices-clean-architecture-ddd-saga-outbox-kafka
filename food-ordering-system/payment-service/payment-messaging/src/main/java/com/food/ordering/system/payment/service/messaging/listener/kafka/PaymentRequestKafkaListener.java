package com.food.ordering.system.payment.service.messaging.listener.kafka;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.PaymentOrderStatus;
import com.food.ordering.system.kafka.order.avro.model.PaymentRequestAvroModel;
import com.food.ordering.system.payment.domain.dto.PaymentRequest;
import com.food.ordering.system.payment.domain.port.input.message.listener.PaymentRequestMessageListener;
import com.food.ordering.system.payment.service.messaging.mapper.PaymentMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PaymentRequestKafkaListener implements KafkaConsumer<PaymentRequestAvroModel> {

    private final PaymentRequestMessageListener paymentRequestMessageListener;
    private final PaymentMessagingDataMapper paymentMessagingDataMapper;

    public PaymentRequestKafkaListener(PaymentRequestMessageListener paymentRequestMessageListener,
                                       PaymentMessagingDataMapper paymentMessagingDataMapper) {
        this.paymentRequestMessageListener = paymentRequestMessageListener;
        this.paymentMessagingDataMapper = paymentMessagingDataMapper;
    }

    @Override
    @KafkaListener(
            id = "${kafka-consumer-config.payment-consumer-group-id}",
            topics = "${payment-service.payment-request-topic-name}"
    )
    public void receive(@Payload List<PaymentRequestAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("{} number of payment requests received with keys = {}, partitions = {}, and offsets = {}",
                messages.size(), keys, partitions, offsets
        );

        messages.forEach(paymentRequestAvroModel -> {
            String orderId = paymentRequestAvroModel.getOrderId();
            PaymentOrderStatus paymentOrderStatus = paymentRequestAvroModel.getPaymentOrderStatus();
            PaymentRequest paymentRequest = paymentMessagingDataMapper.paymentRequestAvroModelToPaymentRequest(
                    paymentRequestAvroModel
            );
            if (paymentOrderStatus == PaymentOrderStatus.PENDING) {
                log.info("Processing payment for orderId: {}", orderId);
                paymentRequestMessageListener.completePayment(paymentRequest);
            } else if (paymentOrderStatus == PaymentOrderStatus.CANCELLED) {
                log.info("Cancelling payment for orderId: {}", orderId);
                paymentRequestMessageListener.cancelPayment(paymentRequest);
            }
        });
    }
}
