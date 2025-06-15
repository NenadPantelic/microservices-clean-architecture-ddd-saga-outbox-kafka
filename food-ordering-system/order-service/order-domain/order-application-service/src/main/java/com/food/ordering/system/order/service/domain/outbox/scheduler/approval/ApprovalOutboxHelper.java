package com.food.ordering.system.order.service.domain.outbox.scheduler.approval;

import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.order.service.domain.port.output.repository.ApprovalOutboxRepository;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import com.food.ordering.system.saga.order.SagaConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ApprovalOutboxHelper {

    private final ApprovalOutboxRepository approvalOutboxRepository;

    public ApprovalOutboxHelper(ApprovalOutboxRepository approvalOutboxRepository) {
        this.approvalOutboxRepository = approvalOutboxRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderApprovalOutboxMessage> getApprovalOutboxMessageByOutboxStatusAndSagaStatus(
            OutboxStatus outboxStatus, SagaStatus... sagaStatuses) {
        return approvalOutboxRepository.findByTypeAndOutboxStatusAndSagaStatus(
                SagaConstants.ORDER_SAGA_NAME, outboxStatus, sagaStatuses
        );
    }

    @Transactional(readOnly = true)
    public Optional<OrderApprovalOutboxMessage> getApprovalOutboxMessageBySagaIdAndSagaStatus(UUID sagaId,
                                                                                              SagaStatus... sagaStatuses) {
        return approvalOutboxRepository.findByTypeAndSagaIdAndSagaStatus(
                SagaConstants.ORDER_SAGA_NAME, sagaId, sagaStatuses
        );
    }

    @Transactional
    public void save(OrderApprovalOutboxMessage orderApprovalOutboxMessage) {
        OrderApprovalOutboxMessage savedOutboxMessage = approvalOutboxRepository.save(orderApprovalOutboxMessage);
        if (savedOutboxMessage == null) {
            String errMessage = String.format("Could not save OrderApprovalOutboxMessage with outbox id: %s",
                    orderApprovalOutboxMessage.getId());
            log.error(errMessage);
            throw new OrderDomainException(errMessage);
        }

        log.info("OrderApprovalOutboxMessage[id = {}] successfully saved.", orderApprovalOutboxMessage.getId());
    }

    public void deleteOrderApprovalOutboxMessageByOutboxStatusAndSagaStatus(OutboxStatus outboxStatus,
                                                                            SagaStatus... sagaStatuses) {
        approvalOutboxRepository.deleteByTypeAndOutboxStatusAndSagaStatus(
                SagaConstants.ORDER_SAGA_NAME, outboxStatus, sagaStatuses
        );
    }
}
