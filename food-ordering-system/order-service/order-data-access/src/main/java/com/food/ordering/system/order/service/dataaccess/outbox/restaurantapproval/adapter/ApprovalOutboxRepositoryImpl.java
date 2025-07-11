package com.food.ordering.system.order.service.dataaccess.outbox.restaurantapproval.adapter;


import com.food.ordering.system.order.service.dataaccess.outbox.restaurantapproval.exception.ApprovalOutboxNotFoundException;
import com.food.ordering.system.order.service.dataaccess.outbox.restaurantapproval.mapper.ApprovalOutboxDataAccessMapper;
import com.food.ordering.system.order.service.dataaccess.outbox.restaurantapproval.repository.ApprovalOutboxJpaRepository;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.order.service.domain.port.output.repository.ApprovalOutboxRepository;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ApprovalOutboxRepositoryImpl implements ApprovalOutboxRepository {

    private final ApprovalOutboxJpaRepository approvalOutboxJpaRepository;
    private final ApprovalOutboxDataAccessMapper approvalOutboxDataAccessMapper;

    public ApprovalOutboxRepositoryImpl(ApprovalOutboxJpaRepository approvalOutboxJpaRepository,
                                        ApprovalOutboxDataAccessMapper approvalOutboxDataAccessMapper) {
        this.approvalOutboxJpaRepository = approvalOutboxJpaRepository;
        this.approvalOutboxDataAccessMapper = approvalOutboxDataAccessMapper;
    }

    @Override
    public OrderApprovalOutboxMessage save(OrderApprovalOutboxMessage orderApprovalOutboxMessage) {
        return approvalOutboxDataAccessMapper
                .approvalOutboxEntityToOrderApprovalOutboxMessage(approvalOutboxJpaRepository
                        .save(approvalOutboxDataAccessMapper
                                .orderCreatedOutboxMessageToOutboxEntity(orderApprovalOutboxMessage)));
    }

    @Override
    public List<OrderApprovalOutboxMessage> findByTypeAndOutboxStatusAndSagaStatus(String sagaType,
                                                                                   OutboxStatus outboxStatus,
                                                                                   SagaStatus... sagaStatus) {
        List<OrderApprovalOutboxMessage> approvalOutboxMessages = approvalOutboxJpaRepository.findByTypeAndOutboxStatusAndSagaStatusIn(sagaType, outboxStatus,
                        Arrays.asList(sagaStatus))

                .stream()
                .map(approvalOutboxDataAccessMapper::approvalOutboxEntityToOrderApprovalOutboxMessage)
                .collect(Collectors.toList());

        if (approvalOutboxMessages.isEmpty()) {
            throw new ApprovalOutboxNotFoundException(
                    String.format("Approval outbox object " +
                            "could be found for saga type %s", sagaType)
            );
        }

        return approvalOutboxMessages;
    }

    @Override
    public Optional<OrderApprovalOutboxMessage> findByTypeAndSagaIdAndSagaStatus(String type,
                                                                                 UUID sagaId,
                                                                                 SagaStatus... sagaStatus) {
        return approvalOutboxJpaRepository
                .findByTypeAndSagaIdAndSagaStatusIn(type, sagaId,
                        Arrays.asList(sagaStatus))
                .map(approvalOutboxDataAccessMapper::approvalOutboxEntityToOrderApprovalOutboxMessage);

    }

    @Override
    public void deleteByTypeAndOutboxStatusAndSagaStatus(String type, OutboxStatus outboxStatus, SagaStatus... sagaStatus) {
        approvalOutboxJpaRepository.deleteByTypeAndOutboxStatusAndSagaStatusIn(type, outboxStatus,
                Arrays.asList(sagaStatus));
    }
}