package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.valueobject.PaymentStatus;
import com.food.ordering.system.order.service.dataaccess.outbox.payment.entity.PaymentOutboxEntity;
import com.food.ordering.system.order.service.dataaccess.outbox.payment.repository.PaymentOutboxJpaRepository;
import com.food.ordering.system.order.service.domain.dto.message.PaymentResponse;
import com.food.ordering.system.saga.SagaStatus;
import com.food.ordering.system.saga.order.SagaConstants;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@Slf4j
@SpringBootTest(classes = OrderServiceApplication.class) // will start the Spring Boot context in test
@Sql(value = "classpath:sql/order_payment_saga_test_setup.sql") // before each test method
@Sql(value = "classpath:sql/order_payment_saga_test_cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
// after each method
public class OrderPaymentSagaTest {

    private static final UUID SAGA_ID = UUID.fromString("d662d372-e909-4111-973c-646c23e6c28c");
    private static final UUID ORDER_ID = UUID.fromString("ef420b7d-7126-4e1b-bb50-0355d70c4f87");
    private static final UUID CUSTOMER_ID = UUID.fromString("0a63b5fb-51d7-49d6-ba1c-fbbb7edf6a61");
    private static final UUID PAYMENT_ID = UUID.fromString("5770b42c-9c0c-44fc-b2c0-cb2dd6d3caec");
    private static final BigDecimal PRICE = new BigDecimal("100");

    @Autowired
    private OrderPaymentSaga orderPaymentSaga;

    @Autowired
    private PaymentOutboxJpaRepository paymentOutboxJpaRepository;


    @Test
    void testDoublePayment() {
        orderPaymentSaga.process(getPaymentResponse());
        orderPaymentSaga.process(getPaymentResponse());
    }

    @Test
    void testDoublePaymentWithThreads() throws InterruptedException {
        Thread thread1 = new Thread(() -> orderPaymentSaga.process(getPaymentResponse()));
        Thread thread2 = new Thread(() -> orderPaymentSaga.process(getPaymentResponse()));

        thread1.start();
        thread2.start();

        thread1.join(); // blocks the calling thread (main thread) until the thread whose join method is called has completed
        thread2.join(); // both threads are executed before the main threads exits this test method; threads called almost
        // at the same time

        assertPaymentOutbox();
    }

    @Test
    void testDoublePaymentWithLatch() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        Thread thread1 = new Thread(() -> {
            try {
                orderPaymentSaga.process(getPaymentResponse());
            } catch (OptimisticLockingFailureException e) {
                log.error("OptimisticLockingFailureException occurred for thread 1");
            } finally {
                latch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                orderPaymentSaga.process(getPaymentResponse());
            } catch (OptimisticLockingFailureException e) {
                log.error("OptimisticLockingFailureException occurred for thread 2");
            } finally {
                latch.countDown();
            }
        });

        thread1.start();
        thread2.start();

        latch.await(); // waits til latch value is 0, i.e. both threads finish it
        assertPaymentOutbox();
    }

    private void assertPaymentOutbox() {
        Optional<PaymentOutboxEntity> paymentOutboxEntityOptional = paymentOutboxJpaRepository.findByTypeAndSagaIdAndSagaStatusIn(
                SagaConstants.ORDER_SAGA_NAME, SAGA_ID, List.of(SagaStatus.PROCESSING)
        );
        Assertions.assertThat(paymentOutboxEntityOptional.isPresent()).isTrue();
    }

    private PaymentResponse getPaymentResponse() {
        return PaymentResponse.builder()
                .id(UUID.randomUUID().toString())
                .sagaId(SAGA_ID.toString())
                .paymentStatus(PaymentStatus.COMPLETED)
                .paymentId(PAYMENT_ID.toString())
                .orderId(ORDER_ID.toString())
                .customerId(CUSTOMER_ID.toString())
                .price(PRICE)
                .createdAt(Instant.now())
                .failureMessages(new ArrayList<>())
                .build();
    }


}

