package io.fiap.fastfood.driven.core.domain.tracking.port.inbound;

import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderTrackingUseCase {
    Mono<OrderTracking> create(OrderTracking orderTracking);

    Mono<OrderTracking> findByOrderId(String orderId);

    Flux<OrderTracking> find(Pageable pageable);
}
