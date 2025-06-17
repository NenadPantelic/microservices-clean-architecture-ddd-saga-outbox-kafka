package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.order.service.domain.constant.MessageConstant;
import com.food.ordering.system.order.service.domain.dto.message.PaymentResponse;
import com.food.ordering.system.order.service.domain.port.input.message.listener.payment.PaymentResponseMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
@Service
// listener implementations will be triggered by the domain events from other bonded contexts
public class PaymentResponseMessageListenerImpl implements PaymentResponseMessageListener {

    private final OrderPaymentSaga orderPaymentSaga;

    public PaymentResponseMessageListenerImpl(OrderPaymentSaga orderPaymentSaga) {
        this.orderPaymentSaga = orderPaymentSaga;
    }

    @Override
    public void paymentCompleted(PaymentResponse paymentResponse) {
        orderPaymentSaga.process(paymentResponse);
        log.info("Order payment saga process operation is completed for order with id = {}", paymentResponse.orderId());
    }

    @Override
    public void paymentCancelled(PaymentResponse paymentResponse) {
        orderPaymentSaga.rollback(paymentResponse);
        log.info("Order[id = {}] is rollback with failure messages: {}",
                paymentResponse.orderId(),
                String.join(MessageConstant.FAILURE_MESSAGE_DELIMITER, paymentResponse.failureMessages())
        );
    }
}
