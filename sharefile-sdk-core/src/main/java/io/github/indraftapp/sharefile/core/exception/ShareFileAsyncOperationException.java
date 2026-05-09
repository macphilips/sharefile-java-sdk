package io.github.indraftapp.sharefile.core.exception;

import io.github.indraftapp.sharefile.core.model.AsyncOperation;

/**
 * Thrown when an API call returns an {@code AsyncOperation} instead of the expected entity.
 */
public class ShareFileAsyncOperationException extends ShareFileException {

    private final AsyncOperation asyncOperation;

    public ShareFileAsyncOperationException(String message, AsyncOperation asyncOperation) {
        super(message);
        this.asyncOperation = asyncOperation;
    }

    public AsyncOperation getAsyncOperation() {
        return asyncOperation;
    }
}
