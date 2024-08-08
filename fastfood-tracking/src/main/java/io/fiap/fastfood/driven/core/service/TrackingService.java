package io.fiap.fastfood.driven.core.service;

import io.fiap.fastfood.driven.core.domain.model.Notification;
import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.fiap.fastfood.driven.core.domain.notification.port.outbound.NotificationPort;
import io.fiap.fastfood.driven.core.domain.tracking.port.inbound.OrderTrackingUseCase;
import io.fiap.fastfood.driven.core.domain.tracking.port.outbound.OrderTrackingPort;
import io.vavr.Function1;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

@Service
public class TrackingService implements OrderTrackingUseCase {
    private static final List<String> ERROR_STATUSES = List.of("6");

    private final NotificationPort notificationPort;
    private final OrderTrackingPort orderTrackingPort;

    public TrackingService(NotificationPort notificationPort, OrderTrackingPort orderTrackingPort) {
        this.notificationPort = notificationPort;
        this.orderTrackingPort = orderTrackingPort;
    }


    @Override
    public Flux<Message> handleEvent() {
        return orderTrackingPort.readTracking(handle());
    }

    private Function1<OrderTracking, Mono<OrderTracking>> handle() {
        return tracking -> Mono.just(tracking)
            .flatMap(this::tracking)
            .flatMap(this::create)
            .doOnSuccess(this::notify);
    }

    private Mono<Notification> notify(OrderTracking tracking) {
        return Mono.just(tracking)
            .filter(t -> ERROR_STATUSES.contains(t.orderStatusValue()))
            .flatMap(t -> notificationPort.createNotification(new Notification()));
    }

    private Mono<OrderTracking> tracking(OrderTracking tracking) {
        return orderTrackingPort.findManyByOrderNumber(tracking.orderNumber())
            .next()
            .switchIfEmpty(Mono.defer(() -> Mono.just(tracking)))
            .map(t -> OrderTracking.OrderTrackingBuilder.from(t)
                .withId(null)
                .withOrderStatus(tracking.orderStatus())
                .withOrderStatusValue(tracking.orderStatusValue())
                .withOrderTimeSpent(timeSpent().apply(t.orderDateTime()))
                .build());
    }

    private Function1<LocalDateTime, Long> timeSpent() {
        return then -> {
            var now = LocalDateTime.now();
            return ChronoUnit.SECONDS.between(then != null ? then : LocalDateTime.now(), now);
        };
    }


    @Override
    public Mono<OrderTracking> create(OrderTracking orderTracking) {
        return orderTrackingPort.createOrderTracking(orderTracking);
    }

    @Override
    public Mono<OrderTracking> findByOrderNumber(String orderNumber) {
        return orderTrackingPort.findByOrderNumber(orderNumber);
    }

    @Override
    public Flux<OrderTracking> find(Pageable pageable, String role) {
        return orderTrackingPort.find(pageable, role);
    }

}
