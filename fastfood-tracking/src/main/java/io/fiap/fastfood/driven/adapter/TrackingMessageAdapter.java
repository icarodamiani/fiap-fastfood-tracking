package io.fiap.fastfood.driven.adapter;

import io.fiap.fastfood.driven.core.domain.tracking.port.outbound.TrackingMessagePort;
import io.vavr.Function1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Service
public class TrackingMessageAdapter implements TrackingMessagePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackingMessageAdapter.class);

    private final SqsAsyncClient sqsClient;
    private final String trackingQueue;
    private final String trackingDlqQueue;
    private final String numberOfMessages;
    private final String waitTimeMessage;
    private final String visibilityTimeOut;

    public TrackingMessageAdapter(SqsAsyncClient sqsClient,
                                  @Value("${aws.sqs.tracking.queue}") String trackingQueue,
                                  @Value("${aws.sqs.tracking.dlq.queue}") String trackingDlqQueue,
                                  @Value("${aws.sqs.numberOfMessages}") String numberOfMessages,
                                  @Value("${aws.sqs.waitTimeMessage}") String waitTimeMessage,
                                  @Value("${aws.sqs.visibilityTimeOut}") String visibilityTimeOut) {
        this.sqsClient = sqsClient;
        this.trackingQueue = trackingQueue;
        this.trackingDlqQueue = trackingDlqQueue;
        this.numberOfMessages = numberOfMessages;
        this.waitTimeMessage = waitTimeMessage;
        this.visibilityTimeOut = visibilityTimeOut;
    }

    public Mono<ReceiveMessageResponse> receiveTracking() {
        return receive(trackingQueue);
    }

    public Mono<ReceiveMessageResponse> receiveTrackingDlq() {
        return receive(trackingDlqQueue);
    }

    public Mono<DeleteMessageResponse> ackTracking(Message message) {
        return acknowledge(trackingQueue, message);
    }

    public Mono<DeleteMessageResponse> ackTrackingDlq(Message message) {
        return acknowledge(trackingDlqQueue, message);
    }

    private Mono<ReceiveMessageResponse> receive(String queue) {
        return getQueueUrl().apply(queue)
            .map(GetQueueUrlResponse::queueUrl)
            .map(queueUrl -> ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(Integer.parseInt(waitTimeMessage))
                .maxNumberOfMessages(Integer.parseInt(numberOfMessages))
                .visibilityTimeout(Integer.parseInt(visibilityTimeOut))
                .build()
            ).flatMap(request -> Mono.fromFuture(sqsClient.receiveMessage(request)));
    }

    public Mono<DeleteMessageResponse> acknowledge(String queue, Message message) {
        return getQueueUrl().apply(queue)
            .flatMap(q -> Mono.fromFuture(
                    sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(q.queueUrl())
                        .receiptHandle(message.receiptHandle())
                        .build())
                )
            )
            .doOnSuccess(deleteMessageResponse -> LOGGER.info("queue message has been deleted: {}", message.messageId()))
            .onErrorResume(ReceiptHandleIsInvalidException.class, e -> Mono.empty())
            .doOnError(throwable -> LOGGER.error("an error occurred while deleting message.", throwable));
    }


    private Function1<String, Mono<GetQueueUrlResponse>> getQueueUrl() {
        return queue -> Mono.fromFuture(sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
            .queueName(queue)
            .build()))
            .doOnError(throwable -> LOGGER.error("Failed to get queueUrl", throwable));
    }
}
