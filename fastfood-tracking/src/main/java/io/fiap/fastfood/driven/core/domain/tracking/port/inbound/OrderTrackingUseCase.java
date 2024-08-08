package io.fiap.fastfood.driven.core.domain.tracking.port.inbound;

import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

public interface OrderTrackingUseCase {
    Flux<Message> handleEvent();

    Mono<OrderTracking> create(OrderTracking orderTracking);

    Mono<OrderTracking> findByOrderNumber(String orderId);

    Flux<OrderTracking> find(Pageable pageable, String role);
}
