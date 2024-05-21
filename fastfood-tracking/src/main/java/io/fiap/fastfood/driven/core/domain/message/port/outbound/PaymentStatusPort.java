package io.fiap.fastfood.driven.core.domain.message.port.outbound;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

public interface PaymentStatusPort {
    Mono<ReceiveMessageResponse> receivePaymentStatus();

    Mono<ReceiveMessageResponse> receivePaymentStatusDlq();

    Mono<DeleteMessageResponse> acknowledgePaymentStatus(Message message);

    Mono<DeleteMessageResponse> acknowledgePaymentStatusDlq(Message message);
}
