package com.food.ordering.system.order.service.domain.dto.message;

import com.food.ordering.system.domain.valueobject.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record PaymentResponse(String id,
                              String sagaId,
                              String orderId,
                              String paymentId,
                              String customerId,
                              BigDecimal price,
                              Instant createdAt,
                              PaymentStatus paymentStatus,
                              List<String> failureMessages) {
}
