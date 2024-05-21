package io.fiap.fastfood.driven.core.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public record PaymentEvent(
    String id,
    String method,
    BigDecimal total,
    LocalDateTime dateTime,
    String orderId) {


    public static final class PaymentBuilder {
        private String id;
        private String method;
        private BigDecimal total;
        private LocalDateTime dateTime;
        private String orderId;

        private PaymentBuilder() {
        }

        public static PaymentBuilder builder() {
            return new PaymentBuilder();
        }

        public static PaymentBuilder from(PaymentEvent payment) {
            return PaymentBuilder.builder()
                .withId(payment.id)
                .withOrderId(payment.orderId)
                .withDateTime(payment.dateTime)
                .withMethod(payment.method)
                .withTotal(payment.total);
        }

        public PaymentBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public PaymentBuilder withMethod(String method) {
            this.method = method;
            return this;
        }

        public PaymentBuilder withTotal(BigDecimal total) {
            this.total = total;
            return this;
        }

        public PaymentBuilder withDateTime(LocalDateTime date) {
            this.dateTime = date;
            return this;
        }

        public PaymentBuilder withOrderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public PaymentEvent build() {
            return new PaymentEvent(id, method, total, dateTime, orderId);
        }
    }
}
