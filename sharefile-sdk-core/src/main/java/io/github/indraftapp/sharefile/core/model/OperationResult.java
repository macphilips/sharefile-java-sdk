package io.github.indraftapp.sharefile.core.model;

import io.github.indraftapp.sharefile.core.exception.ShareFileAsyncOperationException;

import java.util.Optional;

public sealed interface OperationResult<T> {

    record Completed<T>(T entity) implements OperationResult<T> {}

    record Pending<T>(AsyncOperation operation) implements OperationResult<T> {}

    default boolean isAsync() {
        return this instanceof Pending;
    }

    @SuppressWarnings("unchecked")
    default T getEntityOrThrow() {
        if (this instanceof Completed) {
            return ((Completed<T>) this).entity();
        } else if (this instanceof Pending) {
            AsyncOperation op = ((Pending<T>) this).operation();
            throw new ShareFileAsyncOperationException(
                    "Operation returned AsyncOperation (id=%s). Poll via asyncOperations().awaitCompletion()."
                            .formatted(op.getId()), op);
        }
        throw new IllegalStateException("Unknown OperationResult type");
    }

    @SuppressWarnings("unchecked")
    default Optional<AsyncOperation> asyncOperation() {
        if (this instanceof Pending) {
            return Optional.of(((Pending<T>) this).operation());
        }
        return Optional.empty();
    }
}
