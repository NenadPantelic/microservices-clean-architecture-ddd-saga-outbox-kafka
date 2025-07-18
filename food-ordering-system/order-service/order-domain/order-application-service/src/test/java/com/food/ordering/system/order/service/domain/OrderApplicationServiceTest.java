package com.food.ordering.system.order.service.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.ordering.system.domain.valueobject.*;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderCommand;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderResponse;
import com.food.ordering.system.order.service.domain.dto.create.OrderAddress;
import com.food.ordering.system.order.service.domain.dto.create.OrderItem;
import com.food.ordering.system.order.service.domain.entity.Customer;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.Product;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentEventPayload;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.port.input.service.OrderApplicationService;
import com.food.ordering.system.order.service.domain.port.output.repository.CustomerRepository;
import com.food.ordering.system.order.service.domain.port.output.repository.OrderRepository;
import com.food.ordering.system.order.service.domain.port.output.repository.PaymentOutboxRepository;
import com.food.ordering.system.order.service.domain.port.output.repository.RestaurantRepository;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import com.food.ordering.system.saga.order.SagaConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.food.ordering.system.saga.order.SagaConstants.ORDER_SAGA_NAME;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // for each method a new instance of the class is created
// if it has a different Lifecycle mode, beforeAll method must be static and also all fields must be static
@SpringBootTest(classes = OrderTestConfiguration.class)
class OrderApplicationServiceTest {

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private OrderDataMapper orderDataMapper;

    @Autowired // use a bean from OrderTestConfiguration
    private OrderRepository orderRepository;

    @Autowired // use a bean from OrderTestConfiguration
    private CustomerRepository customerRepository;

    @Autowired // use a bean from OrderTestConfiguration
    private RestaurantRepository restaurantRepository;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateOrderCommand createdOrderCommand;
    private CreateOrderCommand createOrderCommandWrongPrice;
    private CreateOrderCommand createOrderCommandWrongProductPrice;
    private CreateOrderCommand createOrderCommandWithInactiveRestaurant;

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final UUID INACTIVE_RESTAURANT_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final BigDecimal PRICE = new BigDecimal("200.00");


    private static final String ORDER_CREATED_MESSAGE = "Order created successfully";

    @BeforeAll
    private void init() {
        createdOrderCommand = CreateOrderCommand.builder()
                .customerId(CUSTOMER_ID)
                .restaurantId(RESTAURANT_ID)
                .address(OrderAddress.builder()
                        .street("Street 1")
                        .postalCode("1000AB")
                        .city("Paris")
                        .build())
                .price(PRICE)
                .items(List.of(
                        OrderItem.builder()
                                .productId(PRODUCT_ID)
                                .quantity(1)
                                .price(new BigDecimal("50.00"))
                                .subtotal(new BigDecimal("50.00"))
                                .build(),
                        OrderItem.builder()
                                .productId(PRODUCT_ID)
                                .quantity(3)
                                .price(new BigDecimal("50.00"))
                                .subtotal(new BigDecimal("150.00"))
                                .build()
                ))
                .build();

        createOrderCommandWrongPrice = CreateOrderCommand.builder()
                .customerId(CUSTOMER_ID)
                .restaurantId(RESTAURANT_ID)
                .address(OrderAddress.builder()
                        .street("Street 1")
                        .postalCode("1000AB")
                        .city("Paris")
                        .build())
                .price(new BigDecimal("250.00"))
                .items(List.of(
                        OrderItem.builder()
                                .productId(PRODUCT_ID)
                                .quantity(1)
                                .price(new BigDecimal("50.00"))
                                .subtotal(new BigDecimal("50.00"))
                                .build(),
                        OrderItem.builder()
                                .productId(PRODUCT_ID)
                                .quantity(3)
                                .price(new BigDecimal("50.00"))
                                .subtotal(new BigDecimal("150.00"))
                                .build()
                ))
                .build();

        createOrderCommandWrongProductPrice = CreateOrderCommand.builder()
                .customerId(CUSTOMER_ID)
                .restaurantId(RESTAURANT_ID)
                .address(OrderAddress.builder()
                        .street("Street 1")
                        .postalCode("1000AB")
                        .city("Paris")
                        .build())
                .price(new BigDecimal("210.00"))
                .items(List.of(
                        OrderItem.builder()
                                .productId(PRODUCT_ID)
                                .quantity(1)
                                .price(new BigDecimal("60.00"))
                                .subtotal(new BigDecimal("60.00"))
                                .build(),
                        OrderItem.builder()
                                .productId(PRODUCT_ID)
                                .quantity(3)
                                .price(new BigDecimal("50.00"))
                                .subtotal(new BigDecimal("150.00"))
                                .build()
                ))
                .build();

        createOrderCommandWithInactiveRestaurant = CreateOrderCommand.builder()
                .customerId(CUSTOMER_ID)
                .restaurantId(INACTIVE_RESTAURANT_ID)
                .address(OrderAddress.builder()
                        .street("Street 1")
                        .postalCode("1000AB")
                        .city("Paris")
                        .build())
                .price(PRICE)
                .items(List.of(
                        OrderItem.builder()
                                .productId(PRODUCT_ID)
                                .quantity(1)
                                .price(new BigDecimal("50.00"))
                                .subtotal(new BigDecimal("50.00"))
                                .build(),
                        OrderItem.builder()
                                .productId(PRODUCT_ID)
                                .quantity(3)
                                .price(new BigDecimal("50.00"))
                                .subtotal(new BigDecimal("150.00"))
                                .build()
                ))
                .build();

        Customer customer = new Customer();
        customer.setId(new CustomerId(CUSTOMER_ID));

        List<Product> products = List.of(
                new Product(new ProductId(PRODUCT_ID), "product-1", new Money(new BigDecimal("50.00"))),
                new Product(new ProductId(PRODUCT_ID), "product-2", new Money(new BigDecimal("50.00")))
        );
        Restaurant restaurant = Restaurant.builder()
                .id(new RestaurantId(RESTAURANT_ID))
                .products(products)
                .active(true)
                .build();

        Order order = orderDataMapper.createOrderCommandToOrder(createdOrderCommand);
        order.setId(new OrderId(ORDER_ID));

        Mockito.doReturn(Optional.of(customer)).when(customerRepository).findById(CUSTOMER_ID);
        Mockito.doReturn(Optional.of(restaurant)).when(restaurantRepository)
                .findRestaurantInformation(orderDataMapper.createOrderCommandToRestaurant(createdOrderCommand));
        Mockito.doReturn(order).when(orderRepository).save(Mockito.any(Order.class));
        Mockito.doReturn(getOrderPaymentOutboxMessage())
                .when(paymentOutboxRepository)
                .save(Mockito.any(OrderPaymentOutboxMessage.class));
    }

    @Test
    public void testCreateOder() {
        CreateOrderResponse createOrderResponse = orderApplicationService.createOrder(createdOrderCommand);
        Assertions.assertEquals(createOrderResponse.orderStatus(), OrderStatus.PENDING);
        Assertions.assertEquals(createOrderResponse.message(), ORDER_CREATED_MESSAGE);
        Assertions.assertNotNull(createOrderResponse.orderTrackingId());
    }

    @Test
    public void testCreateOderWithWrongTotalPrice() {
        OrderDomainException orderDomainException = Assertions.assertThrows(OrderDomainException.class,
                () -> orderApplicationService.createOrder(createOrderCommandWrongPrice)
        );
        Assertions.assertEquals(
                "Total price 250.00 is not equal to order items total price 200.00",
                orderDomainException.getMessage()
        );
    }

    @Test
    public void testCreateOderWithWrongProductPrice() {
        OrderDomainException orderDomainException = Assertions.assertThrows(OrderDomainException.class,
                () -> orderApplicationService.createOrder(createOrderCommandWrongProductPrice)
        );
        Assertions.assertEquals(
                "Order item price 60.00 is not valid for product " + PRODUCT_ID,
                orderDomainException.getMessage()
        );
    }

    @Test
    public void testCreateOrderWithInactiveRestaurant() {
        Restaurant restaurant = Restaurant.builder()
                .id(new RestaurantId(INACTIVE_RESTAURANT_ID))
                .products(List.of())
                .active(false)
                .build();

        Mockito.doReturn(Optional.of(restaurant))
                .when(restaurantRepository).findRestaurantInformation(
                        orderDataMapper.createOrderCommandToRestaurant(createOrderCommandWithInactiveRestaurant)

                );

        OrderDomainException orderDomainException = Assertions.assertThrows(OrderDomainException.class,
                () -> orderApplicationService.createOrder(createOrderCommandWithInactiveRestaurant)
        );
        Assertions.assertEquals(
                "Restaurant[id = " + INACTIVE_RESTAURANT_ID + "] is not active",
                orderDomainException.getMessage()
        );
    }


    private OrderPaymentOutboxMessage getOrderPaymentOutboxMessage() {
        OrderPaymentEventPayload orderPaymentEventPayload = OrderPaymentEventPayload.builder()
                .orderId(ORDER_ID.toString())
                .customerId(CUSTOMER_ID.toString())
                .price(PRICE)
                .createdAt(ZonedDateTime.now())
                .paymentOrderStatus(PaymentOrderStatus.PENDING.name())
                .build();

        return OrderPaymentOutboxMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(SAGA_ID)
                .createdAt(ZonedDateTime.now())
                .type(SagaConstants.ORDER_SAGA_NAME)
                .payload(createPayload(orderPaymentEventPayload))
                .orderStatus(OrderStatus.PENDING)
                .sagaStatus(SagaStatus.STARTED)
                .outboxStatus(OutboxStatus.STARTED)
                .version(0)
                .build();
    }

    private String createPayload(OrderPaymentEventPayload orderPaymentEventPayload) {
        try {
            return objectMapper.writeValueAsString(orderPaymentEventPayload);
        } catch (JsonProcessingException e) {
            throw new OrderDomainException("Cannot create OrderPaymentEventPayload object!");
        }
    }

}