package io.fiap.fastfood.driven.core.domain.message.port.outbound;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

public interface TrackingUpdatePort {
    Mono<ReceiveMessageResponse> receiveTracking();

    Mono<ReceiveMessageResponse> receiveTrackingDlq();

    Mono<DeleteMessageResponse> ackTracking(Message message);

    Mono<DeleteMessageResponse> ackTrackingDlq(Message message);
}
