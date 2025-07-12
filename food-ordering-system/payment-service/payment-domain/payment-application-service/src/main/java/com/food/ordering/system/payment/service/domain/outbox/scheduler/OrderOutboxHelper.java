package com.food.ordering.system.payment.service.domain.outbox.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer;
import com.food.ordering.system.domain.DomainConstants;
import com.food.ordering.system.domain.valueobject.PaymentStatus;
import com.food.ordering.system.outbox.OutboxScheduler;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.payment.service.domain.entity.Payment;
import com.food.ordering.system.payment.service.domain.exception.PaymentDomainException;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderEventPayload;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderOutboxMessage;
import com.food.ordering.system.payment.service.domain.port.output.repository.OrderOutboxRepository;
import com.food.ordering.system.saga.order.SagaConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class OrderOutboxHelper {

    private static final String ORDER_EVENT_PAYLOAD_SER_ERROR_MESSAGE = "Could not create an OrderEventPayload json!";
    private static final String ORDER_OUTBOX_MESSAGE_SAVE_ERROR_MESSAGE = "Could not save an OrderOutboxMessage!";
    private final OrderOutboxRepository orderOutboxRepository;
    private final ObjectMapper objectMapper;

    public OrderOutboxHelper(OrderOutboxRepository orderOutboxRepository, ObjectMapper objectMapper) {
        this.orderOutboxRepository = orderOutboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<OrderOutboxMessage> getCompletedOrderOutboxMessageBySagaIdAndPaymentStatus(UUID sagaId,
                                                                                               PaymentStatus paymentStatus) {
        return orderOutboxRepository.findByTypeAndSagaIdAndPaymentStatusAndOutboxStatus(
                SagaConstants.ORDER_SAGA_NAME, sagaId, paymentStatus, OutboxStatus.COMPLETED
        );
    }


    @Transactional(readOnly = true)
    public List<OrderOutboxMessage> getOrderOutboxMessageByOutboxStatus(OutboxStatus outboxStatus) {
        return orderOutboxRepository.findByTypeAndOutboxStatus(SagaConstants.ORDER_SAGA_NAME, outboxStatus);
    }

    @Transactional
    public void deleteOrderOutboxMessageByOutboxStatus(OutboxStatus outboxStatus) {
        orderOutboxRepository.deleteByTypeAndOutboxStatus(SagaConstants.ORDER_SAGA_NAME, outboxStatus);
    }

    @Transactional
    public void updateOutboxMessage(OrderOutboxMessage orderOutboxMessage, OutboxStatus outboxStatus) {
        orderOutboxMessage.setOutboxStatus(outboxStatus);
        save(orderOutboxMessage);
        log.info("Order outbox table status is updated as: {}", outboxStatus.name());
    }

    @Transactional
    public void saveOrderOutboxMessage(OrderEventPayload orderEventPayload,
                                       PaymentStatus paymentStatus,
                                       OutboxStatus outboxStatus,
                                       UUID sagaId) {
        save(OrderOutboxMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(orderEventPayload.getCreatedAt())
                .processedAt(ZonedDateTime.now(ZoneId.of(DomainConstants.UTC)))
                .type(SagaConstants.ORDER_SAGA_NAME)
                .payload(createPayload(orderEventPayload))
                .paymentStatus(paymentStatus)
                .outboxStatus(outboxStatus)
                .build());
    }

    private String createPayload(OrderEventPayload orderEventPayload) {
        try {
            return objectMapper.writeValueAsString(orderEventPayload);
        } catch (JsonProcessingException e) {
            log.error(ORDER_EVENT_PAYLOAD_SER_ERROR_MESSAGE);
            throw new PaymentDomainException(ORDER_EVENT_PAYLOAD_SER_ERROR_MESSAGE);
        }
    }

    private void save(OrderOutboxMessage orderOutboxMessage) {
        OrderOutboxMessage savedOutboxMessage = orderOutboxRepository.save(orderOutboxMessage);
        if (savedOutboxMessage == null) {
            log.error(ORDER_OUTBOX_MESSAGE_SAVE_ERROR_MESSAGE);
            throw new PaymentDomainException(ORDER_OUTBOX_MESSAGE_SAVE_ERROR_MESSAGE);
        }
    }
}
