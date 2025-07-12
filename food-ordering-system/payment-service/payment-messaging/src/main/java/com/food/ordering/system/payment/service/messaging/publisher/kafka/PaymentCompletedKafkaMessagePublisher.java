package com.food.ordering.system.payment.service.messaging.publisher.kafka;

import com.food.ordering.system.kafka.order.avro.model.PaymentResponseAvroModel;
import com.food.ordering.system.kafka.producer.KafkaMessageHelper;
import com.food.ordering.system.payment.service.domain.config.PaymentServiceConfigData;
import com.food.ordering.system.payment.service.domain.port.output.message.publisher.PaymentCompletedMessagePublisher;
import com.food.ordering.system.payment.service.domain.event.PaymentCompletedEvent;
import com.food.ordering.system.payment.service.messaging.mapper.PaymentMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.food.ordering.system.kafka.producer.service.KafkaProducer;

@Slf4j
@Component
// PaymentCompletedMessagePublisher - output port
public class PaymentCompletedKafkaMessagePublisher implements PaymentCompletedMessagePublisher {

    private final PaymentMessagingDataMapper paymentMessagingDataMapper;
    private final KafkaProducer<String, PaymentResponseAvroModel> kafkaProducer;
    private final PaymentServiceConfigData paymentServiceConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;

    public PaymentCompletedKafkaMessagePublisher(PaymentMessagingDataMapper paymentMessagingDataMapper,
                                                 KafkaProducer<String, PaymentResponseAvroModel> kafkaProducer,
                                                 PaymentServiceConfigData paymentServiceConfigData,
                                                 KafkaMessageHelper kafkaMessageHelper) {
        this.paymentMessagingDataMapper = paymentMessagingDataMapper;
        this.kafkaProducer = kafkaProducer;
        this.paymentServiceConfigData = paymentServiceConfigData;
        this.kafkaMessageHelper = kafkaMessageHelper;
    }

    @Override
    public void publish(PaymentCompletedEvent domainEvent) {
        String orderId = domainEvent.getPayment().getOrderId().getValue().toString();
        log.info("Received a PaymentCompletedEvent for orderId: {}", orderId);

        try {
            PaymentResponseAvroModel paymentResponseAvroModel = paymentMessagingDataMapper
                    .paymentCompletedEventToPaymentResponseAvroModel(domainEvent);
            kafkaProducer.send(
                    paymentServiceConfigData.getPaymentResponseTopicName(),
                    orderId,
                    paymentResponseAvroModel,
                    kafkaMessageHelper.getKafkaCallback(
                            paymentServiceConfigData.getPaymentResponseTopicName(),
                            paymentResponseAvroModel,
                            orderId,
                            "PaymentResponseAvroModel"
                    )
            );
            log.info("PaymentResponseAvroModel sent to Kafka for orderId: {}", orderId);
        } catch (Exception e) {
            log.error("Error while sending PaymentResponseAvroModel message to Kafka with orderId: {}. Error: {}",
                    orderId, e.getMessage()
            );
        }
    }
}
