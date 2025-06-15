package com.food.ordering.system.order.service.domain.outbox.scheduler.approval;

import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.outbox.OutboxScheduler;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RestaurantApprovalOutboxCleanerScheduler implements OutboxScheduler {

    private final ApprovalOutboxHelper approvalOutboxHelper;

    public RestaurantApprovalOutboxCleanerScheduler(ApprovalOutboxHelper approvalOutboxHelper) {
        this.approvalOutboxHelper = approvalOutboxHelper;
    }

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        List<OrderApprovalOutboxMessage> outboxMessages = approvalOutboxHelper.getApprovalOutboxMessageByOutboxStatusAndSagaStatus(
                OutboxStatus.COMPLETED,
                SagaStatus.SUCCEEDED, SagaStatus.FAILED, SagaStatus.COMPENSATED
        );

        if (outboxMessages != null && !outboxMessages.isEmpty()) {
            log.info("Received {} OrderApprovalOutboxMessage for clean-up. Payloads: {}",
                    outboxMessages.size(),
                    outboxMessages.stream()
                            .map(OrderApprovalOutboxMessage::getPayload)
                            .collect(Collectors.joining("\n"))
            );
            approvalOutboxHelper.deleteOrderApprovalOutboxMessageByOutboxStatusAndSagaStatus(
                    OutboxStatus.COMPLETED, // OutboxStatus.FAILED is set only if Kafka cannot sent the message
                    SagaStatus.SUCCEEDED, SagaStatus.FAILED, SagaStatus.COMPENSATED
            );
            log.info("{} OrderApprovalOutboxMessage records have been deleted.", outboxMessages.size());
        }
    }
}