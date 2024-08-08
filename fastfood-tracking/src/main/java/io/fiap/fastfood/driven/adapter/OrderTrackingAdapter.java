package io.fiap.fastfood.driven.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.fiap.fastfood.driven.core.domain.tracking.mapper.OrderTrackingMapper;
import io.fiap.fastfood.driven.core.domain.tracking.port.outbound.OrderTrackingPort;
import io.fiap.fastfood.driven.core.entity.PaginatedOrderTrackingEntity;
import io.fiap.fastfood.driven.core.exception.NotFoundException;
import io.fiap.fastfood.driven.core.messaging.MessagingPort;
import io.fiap.fastfood.driven.repository.OrderTrackingRepository;
import io.vavr.CheckedFunction1;
import io.vavr.Function1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

@Component
public class OrderTrackingAdapter implements OrderTrackingPort {

    private final String queue;
    private final OrderTrackingRepository orderTrackingRepository;
    private final OrderTrackingMapper orderTrackingMapper;
    private final MessagingPort messagingPort;
    private final ObjectMapper objectMapper;


    public OrderTrackingAdapter(@Value("${aws.sqs.tracking.queue}") String queue,
                                OrderTrackingRepository orderTrackingRepository,
                                OrderTrackingMapper orderTrackingMapper,
                                MessagingPort messagingPort,
                                ObjectMapper objectMapper) {
        this.queue = queue;
        this.orderTrackingRepository = orderTrackingRepository;
        this.orderTrackingMapper = orderTrackingMapper;
        this.messagingPort = messagingPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<OrderTracking> createOrderTracking(OrderTracking orderTracking) {
        return orderTrackingRepository.save(orderTrackingMapper.entityFromDomain(orderTracking))
            .map(orderTrackingMapper::domainFromEntity);
    }

    @Override
    public Flux<OrderTracking> findManyByOrderNumber(String orderId) {
        return orderTrackingRepository.findByOrderNumberOrderByOrderDateTime(orderId)
            .map(orderTrackingMapper::domainFromEntity);
    }

    @Override
    public Mono<OrderTracking> findByOrderNumber(String orderNumber) {
        return orderTrackingRepository.findByOrderNumberOrderByOrderDateTime(orderNumber)
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

    @Override
    public Flux<Message> readTracking(Function1<OrderTracking, Mono<OrderTracking>> handle) {
        return messagingPort.read(queue, handle, readEvent());
    }

    private CheckedFunction1<Message, OrderTracking> readEvent() {
        return message -> objectMapper.readValue(message.body(), OrderTracking.class);
    }

}
