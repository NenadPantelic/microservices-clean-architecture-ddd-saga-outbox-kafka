package com.food.ordering.system.order.service.domain.outbox.model.approval;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record OrderApprovalEventProduct(@JsonProperty String id,
                                        @JsonProperty Integer quantity) {
}
