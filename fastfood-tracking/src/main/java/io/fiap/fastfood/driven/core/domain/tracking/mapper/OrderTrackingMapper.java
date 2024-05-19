package io.fiap.fastfood.driven.core.domain.tracking.mapper;

import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.fiap.fastfood.driven.core.entity.OrderTrackingEntity;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OrderTrackingMapper {

    @Mapping(source = "orderTracking", target = "orderDateTime", qualifiedByName = "orderDateTimeEntity")
    OrderTrackingEntity entityFromDomain(OrderTracking orderTracking);

    @Mapping(source = "orderTrackingEntity", target = "orderDateTime", qualifiedByName = "orderDateTimeDomain")
    OrderTracking domainFromEntity(OrderTrackingEntity orderTrackingEntity);

    @Named("orderDateTimeEntity")
    default LocalDateTime orderDateTimeEntity(OrderTracking orderTracking) {
        return LocalDateTime.now();
    }

    @Named("orderDateTimeDomain")
    default LocalDateTime orderDateTimeDomain(OrderTrackingEntity orderTrackingEntity) {
        return LocalDateTime.parse(orderTrackingEntity.orderDateTime().toString());
    }
}
