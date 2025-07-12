package com.food.ordering.system.payment.service.domain;

import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.PaymentStatus;
import com.food.ordering.system.payment.service.domain.dto.PaymentRequest;
import com.food.ordering.system.payment.service.domain.exception.PaymentApplicationServiceException;
import com.food.ordering.system.payment.service.domain.mapper.PaymentDataMapper;
import com.food.ordering.system.payment.service.domain.port.output.repository.CreditEntryRepository;
import com.food.ordering.system.payment.service.domain.port.output.repository.CreditHistoryRepository;
import com.food.ordering.system.payment.service.domain.port.output.repository.PaymentRepository;
import com.food.ordering.system.payment.service.domain.port.output.message.publisher.PaymentCancelledMessagePublisher;
import com.food.ordering.system.payment.service.domain.port.output.message.publisher.PaymentCompletedMessagePublisher;
import com.food.ordering.system.payment.service.domain.port.output.message.publisher.PaymentFailedMessagePublisher;
import com.food.ordering.system.payment.service.domain.entity.CreditEntry;
import com.food.ordering.system.payment.service.domain.entity.CreditHistory;
import com.food.ordering.system.payment.service.domain.entity.Payment;
import com.food.ordering.system.payment.service.domain.event.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class PaymentRequestHelper {

    private final PaymentDomainService paymentDomainService;
    private final PaymentDataMapper paymentDataMapper;
    private final PaymentRepository paymentRepository;
    private final CreditEntryRepository creditEntryRepository;
    private final CreditHistoryRepository creditHistoryRepository;
    private final PaymentCompletedMessagePublisher paymentCompletedMessagePublisher;
    private final PaymentCancelledMessagePublisher paymentCancelledMessagePublisher;
    private final PaymentFailedMessagePublisher paymentFailedMessagePublisher;

    public PaymentRequestHelper(PaymentDomainService paymentDomainService,
                                PaymentDataMapper paymentDataMapper,
                                PaymentRepository paymentRepository,
                                CreditEntryRepository creditEntryRepository,
                                CreditHistoryRepository creditHistoryRepository,
                                PaymentCompletedMessagePublisher paymentCompletedMessagePublisher,
                                PaymentCancelledMessagePublisher paymentCancelledMessagePublisher,
                                PaymentFailedMessagePublisher paymentFailedMessagePublisher) {
        this.paymentDomainService = paymentDomainService;
        this.paymentDataMapper = paymentDataMapper;
        this.paymentRepository = paymentRepository;
        this.creditEntryRepository = creditEntryRepository;
        this.creditHistoryRepository = creditHistoryRepository;
        this.paymentCompletedMessagePublisher = paymentCompletedMessagePublisher;
        this.paymentCancelledMessagePublisher = paymentCancelledMessagePublisher;
        this.paymentFailedMessagePublisher = paymentFailedMessagePublisher;
    }

    @Transactional
    public PaymentEvent persistEvent(PaymentRequest paymentRequest) {
        log.info("Received a payment complete event for orderId: {}", paymentRequest.getOrderId());
        Payment payment = paymentDataMapper.paymentRequestToPayment(paymentRequest);
        return createPaymentEvent(payment, PaymentStatus.COMPLETED);
    }

    @Transactional
    public PaymentEvent persistCancelPayment(PaymentRequest paymentRequest) {
        log.info("Received a paymentOptional rollback event for orderId: {}", paymentRequest.getOrderId());
        Optional<Payment> paymentOptional = paymentRepository.findByOrderId(UUID.fromString(paymentRequest.getOrderId()));

        if (paymentOptional.isEmpty()) {
            String errMessage = String.format("Payment[orderId = %s] not found", paymentRequest.getOrderId());
            log.error(errMessage);
            throw new PaymentApplicationServiceException(errMessage);
        }

        Payment payment = paymentOptional.get();
        return createPaymentEvent(payment, PaymentStatus.CANCELLED);
    }

    private PaymentEvent createPaymentEvent(Payment payment, PaymentStatus paymentStatus) {
        CreditEntry creditEntry = getCreditEntry(payment.getCustomerId());
        List<CreditHistory> creditHistoryList = getCreditHistory(payment.getCustomerId());
        List<String> failureMessages = new ArrayList<>();

        PaymentEvent paymentEvent = null;
        if (paymentStatus == PaymentStatus.COMPLETED) {
            paymentEvent = paymentDomainService.validateAndInitiatePayment(
                    payment, creditEntry, creditHistoryList, failureMessages,
                    paymentCompletedMessagePublisher,
                    paymentFailedMessagePublisher);
        } else if (paymentStatus == PaymentStatus.CANCELLED) {
            paymentEvent = paymentDomainService.validateAndCancelPayment(
                    payment, creditEntry, creditHistoryList, failureMessages,
                    paymentCancelledMessagePublisher,
                    paymentFailedMessagePublisher
            );
        } else {
            String errMessage = String.format("Payment with status %s cannot be handled", paymentStatus);
            log.error(errMessage);
            throw new PaymentApplicationServiceException(errMessage);
        }

        // even if the payment has failed, it should be saved (previous method sets the status to failed)
        persistEntities(payment, failureMessages, creditEntry, creditHistoryList);
        return paymentEvent;
    }

    private CreditEntry getCreditEntry(CustomerId customerId) {
        return creditEntryRepository.findByCustomerId(customerId.getValue()).orElseThrow(
                () -> {
                    String errMessage = String.format(
                            "Could not find a credit entry for customer[id = %s]", customerId.getValue()
                    );
                    log.error(errMessage);
                    return new PaymentApplicationServiceException(errMessage);
                }
        );
    }

    private List<CreditHistory> getCreditHistory(CustomerId customerId) {
        List<CreditHistory> creditHistoryList = creditHistoryRepository.findByCustomerId(customerId.getValue());
        if (creditHistoryList == null || creditHistoryList.isEmpty()) {
            String errMessage = String.format(
                    "Could not find a credit entry for customer[id = %s]", customerId.getValue()
            );
            log.error(errMessage);
            throw new PaymentApplicationServiceException(errMessage);
        }

        return creditHistoryList;
    }

    private void persistEntities(Payment payment,
                                 List<String> failureMessages,
                                 CreditEntry creditEntry,
                                 List<CreditHistory> creditHistoryList) {
        paymentRepository.save(payment);
        if (failureMessages.isEmpty()) {
            creditEntryRepository.save(creditEntry);
            creditHistoryRepository.save(creditHistoryList.get(creditHistoryList.size() - 1));
        }
    }
}
