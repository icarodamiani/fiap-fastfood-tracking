package io.fiap.fastfood.driver.server;


import com.google.protobuf.Timestamp;
import io.fiap.fastfood.FindAllOrderTrackingRequest;
import io.fiap.fastfood.FindOrderTrackingByOrderIdRequest;
import io.fiap.fastfood.OrderTrackingResponse;
import io.fiap.fastfood.OrderTrackingRole;
import io.fiap.fastfood.OrderTrackingServiceGrpc;
import io.fiap.fastfood.OrderTrackingStatus;
import io.fiap.fastfood.SaveOrderTrackingRequest;
import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.fiap.fastfood.driven.core.service.OrderTrackingService;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@GrpcService
public class OrderTrackingGrpcServer extends OrderTrackingServiceGrpc.OrderTrackingServiceImplBase {

    public static final int FIXED_PAGE_SIZE = 10000;
    private final OrderTrackingService service;

    private final GrpcStatusConverter statusConverter;

    @Autowired
    public OrderTrackingGrpcServer(OrderTrackingService service, GrpcStatusConverter statusConverter) {
        this.service = service;
        this.statusConverter = statusConverter;
    }

    @Override
    public void saveOrderTracking(SaveOrderTrackingRequest request, StreamObserver<OrderTrackingResponse> responseObserver) {
        service.create(OrderTracking.OrderTrackingBuilder.builder()
                .withOrderNumber(request.getOrderNumber())
                .withOrderStatus(request.getOrderStatus().name())
                .withOrderStatusValue(String.valueOf(request.getOrderStatus().getNumber()))
                .withOrderId(request.getOrderId())
                .withRole(assignRole(request.getOrderStatus()).name())
                .withOrderDateTime(toLocalDate(request.getOrderDateTime()))
                .build())
            .doOnError(throwable -> responseObserver.onError(statusConverter.toGrpcException(throwable)))
            .map(tracking ->
                OrderTrackingResponse.newBuilder()
                    .setId(tracking.id())
                    .setOrderId(tracking.orderId())
                    .setOrderNumber(tracking.orderNumber())
                    .setRole(OrderTrackingRole.valueOf(tracking.role()))
                    .setOrderStatus(OrderTrackingStatus.valueOf(tracking.orderStatus()))
                    .setOrderDateTime(toTimestamp(tracking.orderDateTime()))
                    .setTotalTimeSpent(tracking.orderTimeSpent())
                    .build()
            )
            .map(response -> {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return response;
            })
            .subscribe();
    }

    @Override
    public void findAllOrderTracking(FindAllOrderTrackingRequest request, StreamObserver<OrderTrackingResponse> responseObserver) {
        service.find(PageRequest.of(0, FIXED_PAGE_SIZE), request.getRole().name())
            .doOnError(throwable -> responseObserver.onError(statusConverter.toGrpcException(throwable)))
            .map(tracking -> OrderTrackingResponse.newBuilder()
                .setId(tracking.id())
                .setOrderId(tracking.orderId())
                .setOrderNumber(tracking.orderNumber())
                .setRole(OrderTrackingRole.valueOf(tracking.role()))
                .setOrderStatus(OrderTrackingStatus.valueOf(tracking.orderStatus()))
                .setOrderDateTime(toTimestamp(tracking.orderDateTime()))
                .setTotalTimeSpent(tracking.orderTimeSpent())
                .build()
            )
            .map(response -> {
                responseObserver.onNext(response);
                return response;
            })
            .doOnComplete(responseObserver::onCompleted)
            .subscribe();
    }

    @Override
    public void findOrderTrackingByOrderId(FindOrderTrackingByOrderIdRequest request, StreamObserver<OrderTrackingResponse> responseObserver) {
        service.findByOrderId(request.getOrderId())
            .doOnError(throwable -> responseObserver.onError(statusConverter.toGrpcException(throwable)))
            .map(tracking -> OrderTrackingResponse.newBuilder()
                .setId(tracking.id())
                .setOrderId(tracking.orderId())
                .setOrderNumber(tracking.orderNumber())
                .setRole(OrderTrackingRole.valueOf(tracking.role()))
                .setOrderStatus(OrderTrackingStatus.valueOf(tracking.orderStatus()))
                .setOrderDateTime(toTimestamp(tracking.orderDateTime()))
                .setTotalTimeSpent(tracking.orderTimeSpent())
                .build()
            )
            .map(response -> {
                responseObserver.onNext(response);
                return response;
            })
            .doOnSuccess(__ -> responseObserver.onCompleted())
            .subscribe();
    }

    private static LocalDateTime toLocalDate(Timestamp ts) {
        return Instant
            .ofEpochSecond(ts.getSeconds(), ts.getNanos())
            .atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC))
            .toLocalDateTime();
    }

    protected Timestamp toTimestamp(LocalDateTime localDateTime) {
        if (localDateTime != null) {
            Instant instant = localDateTime.toInstant(ZoneOffset.UTC);

            return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
        }
        return null;
    }

    public OrderTrackingRole assignRole(OrderTrackingStatus status) {
        return switch (status) {
            case PAYMENT_CONFIRMED, PREPARING, READY -> OrderTrackingRole.ALL;
            default -> OrderTrackingRole.EMPLOYEE;
        };
    }
}