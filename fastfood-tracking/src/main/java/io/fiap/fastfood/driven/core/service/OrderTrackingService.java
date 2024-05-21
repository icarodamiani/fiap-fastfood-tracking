package io.fiap.fastfood.driven.core.service;

import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.fiap.fastfood.driven.core.domain.tracking.port.inbound.OrderTrackingUseCase;
import io.fiap.fastfood.driven.core.domain.tracking.port.outbound.OrderTrackingPort;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderTrackingService implements OrderTrackingUseCase {

    private final OrderTrackingPort orderTrackingPort;

    public OrderTrackingService(OrderTrackingPort orderTrackingPort) {
        this.orderTrackingPort = orderTrackingPort;
    }

    @Override
    public Mono<OrderTracking> create(OrderTracking orderTracking) {
        return orderTrackingPort.createOrderTracking(orderTracking);
    }

    @Override
    public Mono<OrderTracking> findByOrderId(String orderId) {
        return orderTrackingPort.findByOrderId(orderId);
    }

    @Override
    public Flux<OrderTracking> find(Pageable pageable, String role) {
        return orderTrackingPort.find(pageable, role);
    }

}
