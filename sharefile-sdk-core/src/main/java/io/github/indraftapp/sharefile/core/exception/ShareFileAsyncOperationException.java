package io.github.indraftapp.sharefile.core.exception;

import io.github.indraftapp.sharefile.core.model.AsyncOperation;
import lombok.Getter;

/**
 * Thrown when an API call returns an {@code AsyncOperation} instead of the expected entity.
 */
@Getter
public class ShareFileAsyncOperationException extends ShareFileException {

    private final AsyncOperation asyncOperation;

    public ShareFileAsyncOperationException(String message, AsyncOperation asyncOperation) {
        super(message);
        this.asyncOperation = asyncOperation;
    }
}
