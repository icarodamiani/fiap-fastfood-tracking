package io.fiap.fastfood.driven.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fiap.fastfood.driven.core.domain.message.port.inbound.PaymentStatusUseCase;
import io.fiap.fastfood.driven.core.domain.message.port.outbound.PaymentStatusPort;
import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.fiap.fastfood.driven.core.domain.model.PaymentEvent;
import io.fiap.fastfood.driven.core.domain.tracking.port.outbound.OrderTrackingPort;
import io.fiap.fastfood.driven.core.exception.BadRequestException;
import io.fiap.fastfood.driven.core.exception.BusinessException;
import io.fiap.fastfood.driven.core.exception.NotFoundException;
import io.vavr.CheckedFunction1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Service
public class PaymentStatusService implements PaymentStatusUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentStatusService.class);

    private final PaymentStatusPort paymentStatusPort;
    private final OrderTrackingPort orderTrackingPort;
    private final ObjectMapper mapper;

    public PaymentStatusService(PaymentStatusPort paymentStatusPort,
                                OrderTrackingPort orderTrackingPort,
                                ObjectMapper mapper) {
        this.paymentStatusPort = paymentStatusPort;
        this.orderTrackingPort = orderTrackingPort;
        this.mapper = mapper;
    }

    @Override
    public Flux<DeleteMessageResponse> receiveAndHandlePaymentStatus() {
        return paymentStatusPort.receivePaymentStatus()
            .map(ReceiveMessageResponse::messages)
            .flatMapMany(messages ->
                Flux.fromIterable(messages)
                    .flatMap(m -> Mono.just(readEvent().unchecked().apply(m))
                        .flatMap(this::paymentConfirmedTracking)
                        .flatMap(orderTrackingPort::createOrderTracking)
                        .map(__ -> m)
                        .onErrorResume(t ->
                                t instanceof NotFoundException
                                    || t instanceof BusinessException
                                    || t instanceof BadRequestException,
                            throwable -> {
                                LOGGER.error(throwable.getMessage(), throwable);
                                return Mono.just(m);
                            }
                        )
                        .flatMap(paymentStatusPort::acknowledgePaymentStatus)
                    )
            );
    }

    @Override
    public Flux<DeleteMessageResponse> receiveAndHandlePaymentStatusDlq() {
        return paymentStatusPort.receivePaymentStatusDlq()
            .map(ReceiveMessageResponse::messages)
            .flatMapMany(messages ->
                Flux.fromIterable(messages)
                    .doOnEach(messageSignal -> LOGGER.info("Message retrieved from the dead-letter queue {}", messageSignal.get()))
                    .flatMap(m -> Mono.just(readEvent().unchecked().apply(m))
                        .flatMap(this::paymentConfirmedTracking)
                        .flatMap(orderTrackingPort::createOrderTracking)
                        .map(__ -> m)
                        .onErrorResume(t ->
                                t instanceof NotFoundException
                                    || t instanceof BusinessException
                                    || t instanceof BadRequestException,
                            throwable -> {
                                LOGGER.error("Error processing a message obtained from the dead-letter queue. {}", m, throwable);
                                return Mono.just(m);
                            }
                        )
                        .flatMap(paymentStatusPort::acknowledgePaymentStatus)
                    )
            );
    }

    private Mono<OrderTracking> paymentConfirmedTracking(PaymentEvent payment) {
        return orderTrackingPort.findByOrderId(payment.orderId())
            .map(tracking -> OrderTracking.OrderTrackingBuilder.from(tracking)
                .withId(null)
                .withOrderStatus("PAYMENT_CONFIRMED")
                .withOrderStatusValue("1")
                .build());
    }

    private CheckedFunction1<Message, PaymentEvent> readEvent() {
        return message -> mapper.readValue(message.body(), PaymentEvent.class);
    }

}
