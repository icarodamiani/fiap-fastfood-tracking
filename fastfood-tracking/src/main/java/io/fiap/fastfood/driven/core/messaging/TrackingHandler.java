package io.fiap.fastfood.driven.core.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fiap.fastfood.driven.core.domain.message.port.inbound.PaymentStatusUseCase;
import io.fiap.fastfood.driven.core.domain.message.port.outbound.TrackingUpdatePort;
import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.fiap.fastfood.driven.core.domain.tracking.port.outbound.OrderTrackingPort;
import io.fiap.fastfood.driven.core.exception.BadRequestException;
import io.fiap.fastfood.driven.core.exception.BusinessException;
import io.fiap.fastfood.driven.core.exception.NotFoundException;
import io.vavr.CheckedFunction1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Service
public class TrackingHandler implements PaymentStatusUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackingHandler.class);

    private final TrackingUpdatePort paymentStatusPort;
    private final OrderTrackingPort orderTrackingPort;
    private final ObjectMapper mapper;

    public TrackingHandler(TrackingUpdatePort paymentStatusPort,
                           OrderTrackingPort orderTrackingPort,
                           ObjectMapper mapper) {
        this.paymentStatusPort = paymentStatusPort;
        this.orderTrackingPort = orderTrackingPort;
        this.mapper = mapper;
    }

    @Override
    public Flux<DeleteMessageResponse> handle() {
        return paymentStatusPort.receiveTracking()
            .map(ReceiveMessageResponse::messages)
            .flatMapMany(messages ->
                Flux.fromIterable(messages)
                    .flatMap(m -> Mono.just(readEvent().unchecked().apply(m))
                        .flatMap(this::tracking)
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
                        .flatMap(paymentStatusPort::ackTracking)
                    )
            );
    }

    private Mono<OrderTracking> tracking(OrderTracking tracking) {
        return orderTrackingPort.findByOrderId(tracking.orderId())
            .switchIfEmpty(Mono.defer(() -> Mono.just(tracking)))
            .map(t -> OrderTracking.OrderTrackingBuilder.from(t)
                .withId(null)
                .withOrderStatus(tracking.orderStatus())
                .withOrderStatus(tracking.orderStatusValue())
                .build());
    }

    @Override
    public Flux<DeleteMessageResponse> handleUpdateDlq() {
        return paymentStatusPort.receiveTrackingDlq()
            .map(ReceiveMessageResponse::messages)
            .flatMapMany(messages ->
                Flux.fromIterable(messages)
                    .doOnEach(messageSignal -> LOGGER.info("Message retrieved from the dead-letter queue {}", messageSignal.get()))
                    .flatMap(paymentStatusPort::ackTrackingDlq)
            );
    }

    private CheckedFunction1<Message, OrderTracking> readEvent() {
        return message -> mapper.readValue(message.body(), OrderTracking.class);
    }

}
