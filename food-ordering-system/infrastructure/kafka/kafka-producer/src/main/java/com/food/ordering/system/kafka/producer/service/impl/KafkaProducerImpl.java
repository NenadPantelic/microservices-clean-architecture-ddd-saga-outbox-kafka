package com.food.ordering.system.kafka.producer.service.impl;

import com.food.ordering.system.kafka.producer.exception.KafkaProducerException;
import com.food.ordering.system.kafka.producer.service.KafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.PreDestroy;
import java.io.Serializable;

// generic producer that can be used from any service with any model
@Slf4j
@Component
public class KafkaProducerImpl<K extends Serializable, V extends SpecificRecordBase> implements KafkaProducer<K, V> {

    private final KafkaTemplate<K, V> kafkaTemplate;

    public KafkaProducerImpl(KafkaTemplate<K, V> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topicName, K key, V message, ListenableFutureCallback<SendResult<K, V>> callback) {
        log.info("Sending message={} to topic={}", message, topicName);
        ListenableFuture<SendResult<K, V>> kafkaResultFuture = null;
        try {
            kafkaResultFuture = kafkaTemplate.send(topicName, key, message);
            kafkaResultFuture.addCallback(callback); // callback will get the result of the send method
        } catch (KafkaException e) {
            String errMessage = String.format("Error on Kafka producer[key = %s, message = %s", key, message);
            log.error("{}. Exception: {}", errMessage, e.getMessage(), e);
            throw new KafkaProducerException(errMessage);
        }
    }

    @PreDestroy // called when the application is shutting down
    public void close() {
        if (kafkaTemplate != null) {
            log.info("Closing Kafka producer...");
            kafkaTemplate.destroy();
        }
    }
}
