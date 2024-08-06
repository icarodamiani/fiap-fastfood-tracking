package io.fiap.fastfood.driven.core.domain.message.port.inbound;

import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;

public interface PaymentStatusUseCase {
    Flux<DeleteMessageResponse> handle();

    Flux<DeleteMessageResponse> handleUpdateDlq();
}
