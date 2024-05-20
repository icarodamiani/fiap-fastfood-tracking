package io.fiap.fastfood.driven.adapter;

import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.fiap.fastfood.driven.core.domain.tracking.mapper.OrderTrackingMapper;
import io.fiap.fastfood.driven.core.domain.tracking.port.outbound.OrderTrackingPort;
import io.fiap.fastfood.driven.core.entity.PaginatedOrderTrackingEntity;
import io.fiap.fastfood.driven.core.exception.NotFoundException;
import io.fiap.fastfood.driven.repository.OrderTrackingRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OrderTrackingAdapter implements OrderTrackingPort {

    private final OrderTrackingRepository orderTrackingRepository;
    private final OrderTrackingMapper orderTrackingMapper;


    public OrderTrackingAdapter(OrderTrackingRepository orderTrackingRepository,
                                OrderTrackingMapper orderTrackingMapper) {
        this.orderTrackingRepository = orderTrackingRepository;
        this.orderTrackingMapper = orderTrackingMapper;
    }

    @Override
    public Mono<OrderTracking> createOrderTracking(OrderTracking orderTracking) {
        return orderTrackingRepository.save(orderTrackingMapper.entityFromDomain(orderTracking))
            .map(orderTrackingMapper::domainFromEntity);
    }

    @Override
    public Mono<OrderTracking> findByOrderId(String orderId) {
        return orderTrackingRepository.findByOrderIdOrderByOrderDateTime(orderId)
            .takeLast(1)
            .next()
            .map(orderTrackingMapper::domainFromEntity)
            .switchIfEmpty(Mono.defer(() -> Mono.error(NotFoundException::new)));
    }

    @Override
    public Flux<OrderTracking> find(Pageable pageable, String role) {
        return orderTrackingRepository.findTracking(
                pageable.getPageNumber(),
                pageable.getPageNumber() * pageable.getPageSize(),
                pageable.getPageSize()
            )
            .flatMapIterable(PaginatedOrderTrackingEntity::data)
            .filter(tracking -> tracking.role().equals(role)
                || tracking.role().equals("ALL") || tracking.role().equals("UNRECOGNIZED"))
            .map(orderTrackingMapper::domainFromEntity)
            .filter(orderTracking -> !"FINISHED".equals(orderTracking.orderStatus()))
            .switchIfEmpty(Mono.defer(() -> Mono.error(NotFoundException::new)));
    }
}
