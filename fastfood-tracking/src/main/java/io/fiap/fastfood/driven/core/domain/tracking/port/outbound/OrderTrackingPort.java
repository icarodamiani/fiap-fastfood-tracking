package io.fiap.fastfood.driven.core.domain.tracking.port.outbound;

import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.vavr.Function1;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

public interface OrderTrackingPort {
    Mono<OrderTracking> createOrderTracking(OrderTracking orderTracking);

    Flux<OrderTracking> findManyByOrderNumber(String orderId);

    Mono<OrderTracking> findByOrderNumber(String orderId);

    Flux<OrderTracking> find(Pageable pageable, String role);

    Flux<Message> readTracking(Function1<OrderTracking, Mono<OrderTracking>> handle);
}
