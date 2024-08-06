package io.fiap.fastfood.driven.core.domain.tracking.port.inbound;

import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;

public interface TrackingMessageUseCase {
    Flux<DeleteMessageResponse> handle();

    Flux<DeleteMessageResponse> handleDlq();
}
