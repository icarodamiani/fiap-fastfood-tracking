package io.fiap.fastfood.driven.core.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fiap.fastfood.driven.core.domain.model.Notification;
import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.fiap.fastfood.driven.core.domain.notification.port.outbound.NotificationPort;
import io.fiap.fastfood.driven.core.domain.tracking.port.inbound.TrackingMessageUseCase;
import io.fiap.fastfood.driven.core.domain.tracking.port.outbound.OrderTrackingPort;
import io.fiap.fastfood.driven.core.domain.tracking.port.outbound.TrackingMessagePort;
import io.fiap.fastfood.driven.core.exception.BadRequestException;
import io.fiap.fastfood.driven.core.exception.BusinessException;
import io.fiap.fastfood.driven.core.exception.NotFoundException;
import io.vavr.CheckedFunction1;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Service
public class TrackingHandler implements TrackingMessageUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackingHandler.class);
    private static final List<String> ERROR_STATUSES = List.of("6");

    private final TrackingMessagePort messageHandlerPort;
    private final OrderTrackingPort orderTrackingPort;
    private final NotificationPort notificationPort;
    private final ObjectMapper mapper;

    public TrackingHandler(TrackingMessagePort messageHandlerPort,
                           OrderTrackingPort orderTrackingPort,
                           NotificationPort notificationPort,
                           ObjectMapper mapper) {
        this.messageHandlerPort = messageHandlerPort;
        this.orderTrackingPort = orderTrackingPort;
        this.notificationPort = notificationPort;
        this.mapper = mapper;
    }

    @Override
    public Flux<DeleteMessageResponse> handle() {
        return messageHandlerPort.receiveTracking()
            .map(ReceiveMessageResponse::messages)
            .flatMapMany(messages ->
                Flux.fromIterable(messages)
                    .flatMap(m -> Mono.just(readEvent().unchecked().apply(m))
                        .flatMap(this::tracking)
                        .flatMap(orderTrackingPort::createOrderTracking)
                        .doOnSuccess(this::notify)
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
                        .flatMap(messageHandlerPort::ackTracking)
                    )
            );
    }

    private Mono<Notification> notify(OrderTracking tracking) {
        return Mono.just(tracking)
            .filter(t -> ERROR_STATUSES.contains(t.orderStatusValue()))
            .flatMap(t -> notificationPort.createNotification(new Notification()));
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
    public Flux<DeleteMessageResponse> handleDlq() {
        return messageHandlerPort.receiveTrackingDlq()
            .map(ReceiveMessageResponse::messages)
            .flatMapMany(messages ->
                Flux.fromIterable(messages)
                    .doOnEach(messageSignal -> LOGGER.info("Message retrieved from the dead-letter queue {}", messageSignal.get()))
                    .flatMap(messageHandlerPort::ackTrackingDlq)
            );
    }

    private CheckedFunction1<Message, OrderTracking> readEvent() {
        return message -> mapper.readValue(message.body(), OrderTracking.class);
    }

}
