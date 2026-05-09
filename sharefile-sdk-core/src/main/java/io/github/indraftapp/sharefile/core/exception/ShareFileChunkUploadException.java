package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when a single chunk upload fails after retries.
 */
public class ShareFileChunkUploadException extends ShareFileUploadException {

    private final int chunkIndex;
    private final long offset;

    public ShareFileChunkUploadException(String message, Throwable cause,
                                         int chunkIndex, long offset,
                                         int lastSuccessfulChunkIndex, long bytesTransferred) {
        super(message, cause, true, lastSuccessfulChunkIndex, bytesTransferred);
        this.chunkIndex = chunkIndex;
        this.offset = offset;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public long getOffset() {
        return offset;
    }
}
