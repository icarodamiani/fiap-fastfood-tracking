package io.fiap.fastfood.driven.core.entity;

import java.util.List;

public record PaginatedOrderTrackingEntity(
        List<OrderTrackingEntity> data
) {
}
