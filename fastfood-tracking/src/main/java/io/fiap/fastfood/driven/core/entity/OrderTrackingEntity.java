package io.fiap.fastfood.driven.core.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("pedido_acompanhamento")
public record OrderTrackingEntity(
    @Id
    String id,
    @Field("id_pedido")
    String orderId,

    @Field("numero_pedido")
    String orderNumber,

    @Field("status")
    String orderStatus,

    @Field("status_value")
    String orderStatusValue,

    @Field("visibilidade")
    String role,

    @Field("data_hora")
    LocalDateTime orderDateTime,

    @Field("tempo_decorrido")
    Long orderTimeSpent
) {


    public static final class OrderTrackingEntityBuilder {
        private String id;
        private String orderId;
        private String orderNumber;
        private String orderStatus;
        String orderStatusValue;
        private String role;
        private LocalDateTime orderDateTime;
        private Long orderTimeSpent;

        private OrderTrackingEntityBuilder() {
        }

        public static OrderTrackingEntityBuilder builder() {
            return new OrderTrackingEntityBuilder();
        }

        public OrderTrackingEntityBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public OrderTrackingEntityBuilder withOrderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public OrderTrackingEntityBuilder withOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }

        public OrderTrackingEntityBuilder withOrderStatus(String orderStatus) {
            this.orderStatus = orderStatus;
            return this;
        }

        public OrderTrackingEntityBuilder withOrderStatusValue(String orderStatusValue) {
            this.orderStatusValue = orderStatusValue;
            return this;
        }

        public OrderTrackingEntityBuilder withRole(String role) {
            this.role = role;
            return this;
        }

        public OrderTrackingEntityBuilder withOrderDateTime(LocalDateTime orderDateTime) {
            this.orderDateTime = orderDateTime;
            return this;
        }

        public OrderTrackingEntityBuilder withOrderTimeSpent(Long orderTimeSpent) {
            this.orderTimeSpent = orderTimeSpent;
            return this;
        }

        public OrderTrackingEntity build() {
            return new OrderTrackingEntity(id, orderId, orderNumber, orderStatus, orderStatusValue, role,
                orderDateTime, orderTimeSpent);
        }
    }
}
