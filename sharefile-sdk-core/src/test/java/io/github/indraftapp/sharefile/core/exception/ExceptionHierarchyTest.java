package io.github.indraftapp.sharefile.core.exception;

import io.github.indraftapp.sharefile.core.model.AsyncOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-03: Verify every exception can be created and its fields are accessible via getters.
 * Also verifies the inheritance hierarchy.
 */
class ExceptionHierarchyTest {

    // ── Base ──────────────────────────────────────────────────────────────

    @Test
    void shareFileException_messageOnly() {
        var ex = new ShareFileException("base error");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("base error");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void shareFileException_messageAndCause() {
        var cause = new RuntimeException("root");
        var ex = new ShareFileException("base error", cause);
        assertThat(ex.getMessage()).isEqualTo("base error");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    // ── API Exceptions ───────────────────────────────────────────────────

    @Test
    void shareFileApiException_fieldsAccessible() {
        var ex = new ShareFileApiException(500, "ServerError", "Internal failure",
                "req-123", "GET", "/sf/v3/Items(id)");

        assertThat(ex).isInstanceOf(ShareFileException.class);
        assertThat(ex.getHttpStatus()).isEqualTo(500);
        assertThat(ex.getErrorCode()).isEqualTo("ServerError");
        assertThat(ex.getErrorMessage()).isEqualTo("Internal failure");
        assertThat(ex.getRequestId()).isEqualTo("req-123");
        assertThat(ex.getRequestMethod()).isEqualTo("GET");
        assertThat(ex.getRequestUri()).isEqualTo("/sf/v3/Items(id)");
        assertThat(ex.getMessage()).contains("500", "Internal failure", "req-123", "GET");
    }

    @Test
    void shareFileApiException_withCause() {
        var cause = new RuntimeException("network");
        var ex = new ShareFileApiException(502, "BadGateway", "upstream timeout",
                "req-456", "POST", "/sf/v3/Items", cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(502);
    }

    @Test
    void shareFileNotFoundException_fixedStatus404() {
        var ex = new ShareFileNotFoundException("NotFound", "Item not found",
                "req-1", "GET", "/sf/v3/Items(id)");

        assertThat(ex).isInstanceOf(ShareFileApiException.class);
        assertThat(ex.getHttpStatus()).isEqualTo(404);
    }

    @Test
    void shareFileBadRequestException_fixedStatus400() {
        var ex = new ShareFileBadRequestException("BadRequest", "Invalid param",
                "req-2", "POST", "/sf/v3/Items");

        assertThat(ex).isInstanceOf(ShareFileApiException.class);
        assertThat(ex.getHttpStatus()).isEqualTo(400);
    }

    @Test
    void shareFileUnauthorizedException_fixedStatus401() {
        var ex = new ShareFileUnauthorizedException("Unauthorized", "Token expired",
                "req-3", "GET", "/sf/v3/Users");

        assertThat(ex).isInstanceOf(ShareFileApiException.class);
        assertThat(ex.getHttpStatus()).isEqualTo(401);
    }

    @Test
    void shareFileForbiddenException_fixedStatus403() {
        var ex = new ShareFileForbiddenException("Forbidden", "Access denied",
                "req-4", "DELETE", "/sf/v3/Items(id)");

        assertThat(ex).isInstanceOf(ShareFileApiException.class);
        assertThat(ex.getHttpStatus()).isEqualTo(403);
    }

    @Test
    void shareFileConflictException_fixedStatus409() {
        var ex = new ShareFileConflictException("Conflict", "Already exists",
                "req-5", "POST", "/sf/v3/Items");

        assertThat(ex).isInstanceOf(ShareFileApiException.class);
        assertThat(ex.getHttpStatus()).isEqualTo(409);
    }

    @Test
    void shareFileRateLimitException_fixedStatus429_withRetryAfter() {
        var ex = new ShareFileRateLimitException("RateLimit", "Too many requests",
                "req-6", "GET", "/sf/v3/Items", 30);

        assertThat(ex).isInstanceOf(ShareFileApiException.class);
        assertThat(ex.getHttpStatus()).isEqualTo(429);
        assertThat(ex.getRetryAfterSeconds()).isEqualTo(30);
    }

    @Test
    void shareFileServerException_variableStatus() {
        var ex = new ShareFileServerException(503, "ServiceUnavailable", "Maintenance",
                "req-7", "GET", "/sf/v3/Items");

        assertThat(ex).isInstanceOf(ShareFileApiException.class);
        assertThat(ex.getHttpStatus()).isEqualTo(503);
    }

    @Test
    void shareFileServerException_withCause() {
        var cause = new RuntimeException("upstream");
        var ex = new ShareFileServerException(502, "BadGateway", "Upstream failed",
                "req-8", "POST", "/sf/v3/Items", cause);

        assertThat(ex.getCause()).isSameAs(cause);
    }

    // ── Authentication Exceptions ────────────────────────────────────────

    @Test
    void shareFileAuthenticationException_hierarchy() {
        var ex = new ShareFileAuthenticationException("auth failed");
        assertThat(ex).isInstanceOf(ShareFileException.class);
        assertThat(ex.getMessage()).isEqualTo("auth failed");
    }

    @Test
    void shareFileAuthenticationException_withCause() {
        var cause = new RuntimeException("token error");
        var ex = new ShareFileAuthenticationException("auth failed", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void credentialResolutionException_hierarchy() {
        var ex = new CredentialResolutionException("no creds");
        assertThat(ex).isInstanceOf(ShareFileAuthenticationException.class);
        assertThat(ex).isInstanceOf(ShareFileException.class);
    }

    @Test
    void credentialResolutionException_withCause() {
        var cause = new RuntimeException("vault error");
        var ex = new CredentialResolutionException("no creds", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    // ── Infrastructure Exceptions ────────────────────────────────────────

    @Test
    void shareFileNetworkException_hierarchy() {
        var ex = new ShareFileNetworkException("connection refused");
        assertThat(ex).isInstanceOf(ShareFileException.class);
        assertThat(ex.getMessage()).isEqualTo("connection refused");
    }

    @Test
    void shareFileNetworkException_withCause() {
        var cause = new java.net.ConnectException("refused");
        var ex = new ShareFileNetworkException("connection refused", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void shareFileSerializationException_hierarchy() {
        var ex = new ShareFileSerializationException("bad json");
        assertThat(ex).isInstanceOf(ShareFileException.class);
    }

    @Test
    void shareFileSerializationException_withCause() {
        var cause = new RuntimeException("parse error");
        var ex = new ShareFileSerializationException("bad json", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void shareFileTimeoutException_hierarchy() {
        var ex = new ShareFileTimeoutException("polling timed out");
        assertThat(ex).isInstanceOf(ShareFileException.class);
    }

    @Test
    void shareFileTimeoutException_withCause() {
        var cause = new RuntimeException("deadline");
        var ex = new ShareFileTimeoutException("polling timed out", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void shareFileAsyncOperationException_holdsOperation() {
        var op = new AsyncOperation();
        op.setId("op-abc");
        var ex = new ShareFileAsyncOperationException("pending operation", op);

        assertThat(ex).isInstanceOf(ShareFileException.class);
        assertThat(ex.getAsyncOperation()).isSameAs(op);
        assertThat(ex.getAsyncOperation().getId()).isEqualTo("op-abc");
    }

    // ── Transfer Exceptions ──────────────────────────────────────────────

    @Test
    void shareFileTransferException_hierarchy() {
        var ex = new ShareFileTransferException("transfer failed");
        assertThat(ex).isInstanceOf(ShareFileException.class);
    }

    @Test
    void shareFileTransferException_withCause() {
        var cause = new RuntimeException("io");
        var ex = new ShareFileTransferException("transfer failed", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void shareFileTransferCancelledException_hierarchy() {
        var ex = new ShareFileTransferCancelledException("cancelled by user");
        assertThat(ex).isInstanceOf(ShareFileTransferException.class);
        assertThat(ex.getMessage()).isEqualTo("cancelled by user");
    }

    // ── Upload Exceptions ────────────────────────────────────────────────

    @Test
    void shareFileUploadException_fieldsAccessible() {
        var ex = new ShareFileUploadException("upload failed", true, 5, 1024);

        assertThat(ex).isInstanceOf(ShareFileTransferException.class);
        assertThat(ex.isResumable()).isTrue();
        assertThat(ex.getLastSuccessfulChunkIndex()).isEqualTo(5);
        assertThat(ex.getBytesTransferred()).isEqualTo(1024);
    }

    @Test
    void shareFileUploadException_withCause() {
        var cause = new RuntimeException("io");
        var ex = new ShareFileUploadException("upload failed", cause, false, -1, 0);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.isResumable()).isFalse();
    }

    @Test
    void shareFileUploadNegotiationException_defaults() {
        var ex = new ShareFileUploadNegotiationException("negotiation failed");

        assertThat(ex).isInstanceOf(ShareFileUploadException.class);
        assertThat(ex.isResumable()).isFalse();
        assertThat(ex.getLastSuccessfulChunkIndex()).isEqualTo(-1);
        assertThat(ex.getBytesTransferred()).isEqualTo(0);
    }

    @Test
    void shareFileUploadNegotiationException_withCause() {
        var cause = new RuntimeException("http");
        var ex = new ShareFileUploadNegotiationException("negotiation failed", cause);

        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void shareFileChunkUploadException_fieldsAccessible() {
        var cause = new RuntimeException("timeout");
        var ex = new ShareFileChunkUploadException("chunk 3 failed", cause, 3, 3072, 2, 2048);

        assertThat(ex).isInstanceOf(ShareFileUploadException.class);
        assertThat(ex.getChunkIndex()).isEqualTo(3);
        assertThat(ex.getOffset()).isEqualTo(3072);
        assertThat(ex.isResumable()).isTrue();
        assertThat(ex.getLastSuccessfulChunkIndex()).isEqualTo(2);
        assertThat(ex.getBytesTransferred()).isEqualTo(2048);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void shareFileUploadFinalizationException_defaults() {
        var ex = new ShareFileUploadFinalizationException("finalization failed", 5000);

        assertThat(ex).isInstanceOf(ShareFileUploadException.class);
        assertThat(ex.isResumable()).isFalse();
        assertThat(ex.getLastSuccessfulChunkIndex()).isEqualTo(-1);
        assertThat(ex.getBytesTransferred()).isEqualTo(5000);
    }

    @Test
    void shareFileUploadFinalizationException_withCause() {
        var cause = new RuntimeException("api");
        var ex = new ShareFileUploadFinalizationException("finalization failed", cause, 5000);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getBytesTransferred()).isEqualTo(5000);
    }

    // ── Download Exceptions ──────────────────────────────────────────────

    @Test
    void shareFileDownloadException_hierarchy() {
        var ex = new ShareFileDownloadException("download failed");
        assertThat(ex).isInstanceOf(ShareFileTransferException.class);
    }

    @Test
    void shareFileDownloadException_withCause() {
        var cause = new RuntimeException("io");
        var ex = new ShareFileDownloadException("download failed", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void shareFileDownloadUrlExpiredException_hierarchy() {
        var ex = new ShareFileDownloadUrlExpiredException("url expired");
        assertThat(ex).isInstanceOf(ShareFileDownloadException.class);
    }

    @Test
    void shareFileDownloadWriteException_hierarchy() {
        var cause = new java.io.IOException("disk full");
        var ex = new ShareFileDownloadWriteException("write failed", cause);

        assertThat(ex).isInstanceOf(ShareFileDownloadException.class);
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
