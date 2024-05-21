package io.fiap.fastfood.driver.server;

import io.fiap.fastfood.driven.core.exception.BadRequestException;
import io.fiap.fastfood.driven.core.exception.BusinessException;
import io.fiap.fastfood.driven.core.exception.NotFoundException;
import io.fiap.fastfood.driven.core.exception.TechnicalException;
import io.fiap.fastfood.driven.core.exception.UnavailableException;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.CompletionException;
import org.springframework.stereotype.Component;

/**
 * Convert {@link Throwable} instances to and from GRPC {@link Status},
 * {@link Code}, {@link StatusException} and {@link StatusRuntimeException}.
 */
@Component
public class GrpcStatusConverter {

    public static final Key<String> CODE_KEY = Key.of("code", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * Convert {@link Throwable} into a GRPC {@link StatusRuntimeException}.
     *
     * @param throwable exception to convert
     * @return {@link StatusRuntimeException}
     */
    public StatusRuntimeException toGrpcException(final Throwable throwable) {
        if (throwable instanceof BusinessException)
            return this.createStatusException(Status.FAILED_PRECONDITION, throwable);
        else if (throwable instanceof NotFoundException)
            return this.createStatusException(Status.NOT_FOUND, throwable);
        else if (throwable instanceof TechnicalException)
            return this.createStatusException(Status.INTERNAL, throwable);
        else if (throwable instanceof UnavailableException)
            return this.createStatusException(Status.UNAVAILABLE, throwable);
        else if (throwable instanceof BadRequestException)
            return this.createStatusException(Status.INVALID_ARGUMENT, throwable);
        else if (throwable instanceof IllegalArgumentException)
            return this.createStatusException(Status.INVALID_ARGUMENT, throwable);
        else if (throwable instanceof CompletionException && throwable.getCause() != null)
            return this.toGrpcException(throwable.getCause());
        else return this.createStatusException(Status.INTERNAL, throwable);
    }

    private StatusRuntimeException createStatusException(final Status status, final Throwable throwable) {
        return status.withDescription(throwable.getMessage())
            .withCause(throwable)
            .asRuntimeException();
    }
}