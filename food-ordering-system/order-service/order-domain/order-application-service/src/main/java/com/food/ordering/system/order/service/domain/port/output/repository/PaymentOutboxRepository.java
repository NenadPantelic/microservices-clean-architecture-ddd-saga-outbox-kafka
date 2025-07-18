package com.food.ordering.system.order.service.domain.port.output.repository;

import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ports are implemented in infrastructure module
public interface PaymentOutboxRepository {

    OrderPaymentOutboxMessage save(OrderPaymentOutboxMessage orderPaymentOutboxMessage);

    List<OrderPaymentOutboxMessage> findByTypeAndOutboxStatusAndSagaStatus(String type,
                                                                           OutboxStatus outboxStatus,
                                                                           SagaStatus... sagaStatuses);

    Optional<OrderPaymentOutboxMessage> findByTypeAndSagaIdAndSagaStatus(String type,
                                                                         UUID sagaId,
                                                                         SagaStatus... sagaStatuses);

    void deleteByTypeAndOutboxStatusAndSagaStatus(String type,
                                                  OutboxStatus outboxStatus,
                                                  SagaStatus... sagaStatuses);

}
