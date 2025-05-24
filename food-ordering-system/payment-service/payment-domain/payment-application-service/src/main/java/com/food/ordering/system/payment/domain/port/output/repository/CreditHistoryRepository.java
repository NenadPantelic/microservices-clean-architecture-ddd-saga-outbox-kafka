package com.food.ordering.system.payment.domain.port.output.repository;

import com.food.ordering.system.payment.service.domain.entity.CreditHistory;

import java.util.List;
import java.util.UUID;

public interface CreditHistoryRepository {

    CreditHistory save(CreditHistory payment);

    List<CreditHistory> findByCustomerId(UUID customerId);
}
