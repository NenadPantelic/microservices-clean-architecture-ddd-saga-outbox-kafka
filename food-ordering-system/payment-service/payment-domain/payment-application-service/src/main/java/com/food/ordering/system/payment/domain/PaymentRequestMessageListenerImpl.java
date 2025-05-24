package com.food.ordering.system.payment.domain;

import com.food.ordering.system.payment.domain.dto.PaymentRequest;
import com.food.ordering.system.payment.domain.port.input.message.listener.PaymentRequestMessageListener;
import com.food.ordering.system.payment.domain.port.output.repository.message.publisher.PaymentCancelledMessagePublisher;
import com.food.ordering.system.payment.domain.port.output.repository.message.publisher.PaymentCompletedMessagePublisher;
import com.food.ordering.system.payment.domain.port.output.repository.message.publisher.PaymentFailedMessagePublisher;
import com.food.ordering.system.payment.service.domain.event.PaymentCancelledEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentCompletedEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentRequestMessageListenerImpl implements PaymentRequestMessageListener {

    private final PaymentRequestHelper paymentRequestHelper;
    private final PaymentCompletedMessagePublisher paymentCompletedMessagePublisher;
    private final PaymentCancelledMessagePublisher paymentCancelledMessagePublisher;
    private final PaymentFailedMessagePublisher paymentFailedMessagePublisher;

    public PaymentRequestMessageListenerImpl(PaymentRequestHelper paymentRequestHelper,
                                             PaymentCompletedMessagePublisher paymentCompletedMessagePublisher,
                                             PaymentCancelledMessagePublisher paymentCancelledMessagePublisher,
                                             PaymentFailedMessagePublisher paymentFailedMessagePublisher) {
        this.paymentRequestHelper = paymentRequestHelper;
        this.paymentCompletedMessagePublisher = paymentCompletedMessagePublisher;
        this.paymentCancelledMessagePublisher = paymentCancelledMessagePublisher;
        this.paymentFailedMessagePublisher = paymentFailedMessagePublisher;
    }

    @Override
    public void completePayment(PaymentRequest paymentRequest) {
        PaymentEvent paymentEvent = paymentRequestHelper.persistEvent(paymentRequest);
        fireEvent(paymentEvent);
    }

    @Override
    public void cancelPayment(PaymentRequest paymentRequest) {
        PaymentEvent paymentEvent = paymentRequestHelper.persistCancelPayment(paymentRequest);
        fireEvent(paymentEvent);
    }

    private void fireEvent(PaymentEvent paymentEvent) {
        log.info("Publishing a payment event[id = {}, orderId = {}",
                paymentEvent.getPayment().getId().getValue(),
                paymentEvent.getPayment().getOrderId().getValue()
        );

        // TODO: refactor
        if (paymentEvent instanceof PaymentCompletedEvent) {
            paymentCompletedMessagePublisher.publish((PaymentCompletedEvent) paymentEvent);
        } else if (paymentEvent instanceof PaymentCancelledEvent) {
            paymentCancelledMessagePublisher.publish((PaymentCancelledEvent) paymentEvent);
        } else if (paymentEvent instanceof PaymentFailedEvent) {
            paymentFailedMessagePublisher.publish((PaymentFailedEvent) paymentEvent);
        }
    }
}


