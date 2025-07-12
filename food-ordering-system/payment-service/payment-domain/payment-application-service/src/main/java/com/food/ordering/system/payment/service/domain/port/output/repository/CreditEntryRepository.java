package com.food.ordering.system.payment.service.domain.port.output.repository;

import com.food.ordering.system.payment.service.domain.entity.CreditEntry;

import java.util.Optional;
import java.util.UUID;

public interface CreditEntryRepository {

    CreditEntry save(CreditEntry payment);

    Optional<CreditEntry> findByCustomerId(UUID customerId);
}
