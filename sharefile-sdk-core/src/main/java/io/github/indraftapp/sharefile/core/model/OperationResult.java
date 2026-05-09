package io.github.indraftapp.sharefile.core.model;

import io.github.indraftapp.sharefile.core.exception.ShareFileAsyncOperationException;

import java.util.Optional;

/**
 * Represents the outcome of a ShareFile operation that may complete immediately or continue asynchronously.
 *
 * @param <T> the entity type produced when the operation completes synchronously
 */
public sealed interface OperationResult<T> {

    /**
     * Synchronous operation result containing the completed entity payload.
     *
     * @param entity the entity returned by the completed operation
     * @param <T> the entity type
     */
    record Completed<T>(T entity) implements OperationResult<T> {}

    /**
     * Asynchronous operation result containing the server-side operation to poll.
     *
     * @param operation the asynchronous operation to poll
     * @param <T> the eventual entity type
     */
    record Pending<T>(AsyncOperation operation) implements OperationResult<T> {}

    /**
     * Returns {@code true} when the operation is pending and must be polled for completion.
     *
     * @return {@code true} when the result is asynchronous
     */
    default boolean isAsync() {
        return this instanceof Pending;
    }

    /**
     * Returns the completed entity or throws when the server responded with an asynchronous operation instead.
     *
     * @return the completed entity
     * @throws ShareFileAsyncOperationException if the operation completed asynchronously
     */
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

    /**
     * Returns the asynchronous operation when the result is pending.
     *
     * @return the asynchronous operation, if present
     */
    @SuppressWarnings("unchecked")
    default Optional<AsyncOperation> asyncOperation() {
        if (this instanceof Pending) {
            return Optional.of(((Pending<T>) this).operation());
        }
        return Optional.empty();
    }
}
