package com.food.ordering.system.order.service.domain.port.input.message.listener.payment;

import com.food.ordering.system.order.service.domain.dto.message.PaymentResponse;

// a special kind of Application services that is triggered by domain events
public interface PaymentResponseMessageListener {

    void paymentCompleted(PaymentResponse paymentResponse);

    void paymentCancelled(PaymentResponse paymentResponse);
}
