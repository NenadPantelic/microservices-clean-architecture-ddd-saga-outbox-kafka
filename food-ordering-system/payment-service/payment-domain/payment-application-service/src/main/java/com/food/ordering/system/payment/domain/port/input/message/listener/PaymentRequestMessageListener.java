package com.food.ordering.system.payment.domain.port.input.message.listener;

import com.food.ordering.system.payment.domain.dto.PaymentRequest;

public interface PaymentRequestMessageListener {

    void completePayment(PaymentRequest paymentRequest);

    void cancelPayment(PaymentRequest paymentRequest);
}
