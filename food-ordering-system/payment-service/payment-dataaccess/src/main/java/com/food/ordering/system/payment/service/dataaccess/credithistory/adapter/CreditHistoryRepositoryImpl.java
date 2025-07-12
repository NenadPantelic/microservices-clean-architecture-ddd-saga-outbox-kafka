package com.food.ordering.system.payment.service.dataaccess.credithistory.adapter;

import com.food.ordering.system.payment.service.domain.port.output.repository.CreditHistoryRepository;
import com.food.ordering.system.payment.service.dataaccess.credithistory.entity.CreditHistoryEntity;
import com.food.ordering.system.payment.service.dataaccess.credithistory.mapper.CreditHistoryDataAccessMapper;
import com.food.ordering.system.payment.service.dataaccess.credithistory.repository.CreditHistoryJpaRepository;
import com.food.ordering.system.payment.service.domain.entity.CreditHistory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CreditHistoryRepositoryImpl implements CreditHistoryRepository {

    private final CreditHistoryJpaRepository creditHistoryJpaRepository;
    private final CreditHistoryDataAccessMapper creditHistoryDataAccessMapper;

    public CreditHistoryRepositoryImpl(CreditHistoryJpaRepository creditHistoryJpaRepository,
                                       CreditHistoryDataAccessMapper creditHistoryDataAccessMapper) {
        this.creditHistoryJpaRepository = creditHistoryJpaRepository;
        this.creditHistoryDataAccessMapper = creditHistoryDataAccessMapper;
    }

    @Override
    public CreditHistory save(CreditHistory creditHistory) {
        return creditHistoryDataAccessMapper.creditHistoryEntityToCreditHistory(creditHistoryJpaRepository
                .save(creditHistoryDataAccessMapper.creditHistoryToCreditHistoryEntity(creditHistory)));
    }

    @Override
    public List<CreditHistory> findByCustomerId(UUID customerId) {
        List<CreditHistoryEntity> creditHistory = creditHistoryJpaRepository.findByCustomerId(customerId);
        return creditHistory.stream()
                .map(creditHistoryDataAccessMapper::creditHistoryEntityToCreditHistory)
                .collect(Collectors.toList());
    }
}