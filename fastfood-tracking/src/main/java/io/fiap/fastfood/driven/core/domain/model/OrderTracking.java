package io.fiap.fastfood.driven.core.domain.model;

import java.time.LocalDateTime;

public record OrderTracking(
        String id,
        String orderId,
        String orderNumber,
        String orderStatus,
        String orderStatusValue,
        String role,
        LocalDateTime orderDateTime,
        Long orderTimeSpent
) {

    public static final class OrderTrackingBuilder {
        private String id;
        private String orderId;
        private String orderNumber;
        private String orderStatus;
        private String orderStatusValue;
        private String role;
        private LocalDateTime orderDateTime;
        private Long orderTimeSpent;

        private OrderTrackingBuilder() {
        }

        public static OrderTrackingBuilder builder() {
            return new OrderTrackingBuilder();
        }

        public static OrderTrackingBuilder from(OrderTracking tracking) {
            return OrderTrackingBuilder.builder()
                .withId(tracking.id)
                .withOrderId(tracking.orderId)
                .withOrderNumber(tracking.orderNumber)
                .withOrderStatus(tracking.orderStatus)
                .withOrderStatusValue(tracking.orderStatusValue)
                .withRole(tracking.role)
                .withOrderTimeSpent(tracking.orderTimeSpent);
        }


        public OrderTrackingBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public OrderTrackingBuilder withOrderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public OrderTrackingBuilder withOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }

        public OrderTrackingBuilder withOrderStatus(String orderStatus) {
            this.orderStatus = orderStatus;
            return this;
        }

        public OrderTrackingBuilder withOrderStatusValue(String orderStatusValue) {
            this.orderStatusValue = orderStatusValue;
            return this;
        }

        public OrderTrackingBuilder withRole(String role) {
            this.role = role;
            return this;
        }

        public OrderTrackingBuilder withOrderDateTime(LocalDateTime orderDateTime) {
            this.orderDateTime = orderDateTime;
            return this;
        }

        public OrderTrackingBuilder withOrderTimeSpent(Long orderTimeSpent) {
            this.orderTimeSpent = orderTimeSpent;
            return this;
        }

        public OrderTracking build() {
            return new OrderTracking(id, orderId, orderNumber, orderStatus, orderStatusValue, role, orderDateTime, orderTimeSpent);
        }
    }
}
