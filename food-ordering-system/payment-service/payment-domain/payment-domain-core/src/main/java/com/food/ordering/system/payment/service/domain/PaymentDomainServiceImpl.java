package com.food.ordering.system.payment.service.domain;

import com.food.ordering.system.domain.DomainConstants;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.PaymentStatus;
import com.food.ordering.system.payment.service.domain.entity.CreditEntry;
import com.food.ordering.system.payment.service.domain.entity.CreditHistory;
import com.food.ordering.system.payment.service.domain.entity.Payment;
import com.food.ordering.system.payment.service.domain.event.PaymentCancelledEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentCompletedEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentFailedEvent;
import com.food.ordering.system.payment.service.domain.valueobject.CreditHistoryId;
import com.food.ordering.system.payment.service.domain.valueobject.TransactionType;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
public class PaymentDomainServiceImpl implements PaymentDomainService {

    @Override
    public PaymentEvent validateAndInitiatePayment(Payment payment,
                                                   CreditEntry creditEntry,
                                                   List<CreditHistory> creditHistoryList,
                                                   List<String> failureMessages) {
        payment.validatePayment(failureMessages);
        payment.initializePayment();
        validateCreditEntry(payment, creditEntry, failureMessages);
        subtractCreditEntry(payment, creditEntry);
        updateCreditHistory(payment, creditHistoryList, TransactionType.DEBIT);
        validateCreditHistory(creditEntry, creditHistoryList, failureMessages);

        if (failureMessages.isEmpty()) {
            log.info("Payment is initiated for order[id = {}]", payment.getOrderId().getValue());
            payment.updateStatus(PaymentStatus.COMPLETED);
            return new PaymentCompletedEvent(payment, currentUTCTime());
        } else {
            log.info("Payment initialization failed for order[id = {}]", payment.getOrderId().getValue());
            payment.updateStatus(PaymentStatus.FAILED);
            return new PaymentFailedEvent(payment, currentUTCTime(), failureMessages);
        }
    }

    @Override
    public PaymentEvent validateAndCancelPayment(Payment payment,
                                                 CreditEntry creditEntry,
                                                 List<CreditHistory> creditHistoryList,
                                                 List<String> failureMessages) {
        payment.validatePayment(failureMessages);
        addCreditEntry(payment, creditEntry);
        updateCreditHistory(payment, creditHistoryList, TransactionType.CREDIT);

        if (failureMessages.isEmpty()) {
            log.info("Payment is cancelled for order[id = {}]", payment.getOrderId().getValue());
            payment.updateStatus(PaymentStatus.CANCELLED);
            return new PaymentCancelledEvent(payment, currentUTCTime());
        } else {
            log.info("Payment cancellation failed for order[id = {}]", payment.getOrderId().getValue());
            payment.updateStatus(PaymentStatus.FAILED);
            return new PaymentFailedEvent(payment, currentUTCTime(), failureMessages);
        }
    }

    private void validateCreditEntry(Payment payment, CreditEntry creditEntry, List<String> failureMessages) {
        if (payment.getPrice().isGreaterThan(creditEntry.getTotalCreditAmount())) {
            String errMessage = String.format(
                    "Customer[id = %s] doesn't have enough credit for payment!", payment.getCustomerId().getValue()
            );
            log.error(errMessage);
            failureMessages.add(errMessage);
        }
    }

    private void subtractCreditEntry(Payment payment, CreditEntry creditEntry) {
        creditEntry.subtractCreditAmount(payment.getPrice());
    }

    private void updateCreditHistory(Payment payment,
                                     List<CreditHistory> creditHistoryList,
                                     TransactionType transactionType) {
        creditHistoryList.add(
                CreditHistory.builder()
                        .id(new CreditHistoryId(UUID.randomUUID()))
                        .customerId(payment.getCustomerId())
                        .amount(payment.getPrice())
                        .transactionType(transactionType)
                        .build()
        );
    }

    private void validateCreditHistory(CreditEntry creditEntry,
                                       List<CreditHistory> creditHistoryList,
                                       List<String> failureMessages) {
        Money totalCreditHistory = getTotalHistoryAmount(creditHistoryList, TransactionType.CREDIT);
        Money totalDebitHistory = getTotalHistoryAmount(creditHistoryList, TransactionType.DEBIT);
        if (totalDebitHistory.isGreaterThan(totalCreditHistory)) {
            String errMessage = String.format(
                    "Customer[id = %s] doesn't have enough resources according to credit history!",
                    creditEntry.getCustomerId().getValue()
            );
            log.warn(errMessage);
            failureMessages.add(errMessage);
        }

        if (!creditEntry.getTotalCreditAmount().equals(totalCreditHistory.subtract(totalDebitHistory))) {
            String errMessage = String.format(
                    "Credit history total is not equal to current credits for customer[id = %s]!",
                    creditEntry.getCustomerId().getValue()
            );
            log.warn(errMessage);
            failureMessages.add(errMessage);
        }
    }

    private static Money getTotalHistoryAmount(List<CreditHistory> creditHistoryList, TransactionType credit) {
        return creditHistoryList.stream()
                .filter(creditHistory -> credit == creditHistory.getTransactionType())
                .map(CreditHistory::getAmount)
                .reduce(Money.ZERO, Money::add);
    }

    private void addCreditEntry(Payment payment, CreditEntry creditEntry) {
        creditEntry.addCreditAmount(payment.getPrice());
    }

    private ZonedDateTime currentUTCTime() {
        return ZonedDateTime.now(ZoneId.of(DomainConstants.UTC));
    }
}
