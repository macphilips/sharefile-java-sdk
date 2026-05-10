package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.spi.MetricsProvider;
import io.github.indraftapp.sharefile.core.exception.ShareFileChunkUploadException;
import io.github.indraftapp.sharefile.core.exception.ShareFileDownloadException;
import io.github.indraftapp.sharefile.core.exception.ShareFileDownloadUrlExpiredException;
import io.github.indraftapp.sharefile.core.exception.ShareFileDownloadWriteException;
import io.github.indraftapp.sharefile.core.exception.ShareFileTransferCancelledException;
import io.github.indraftapp.sharefile.core.exception.ShareFileUploadException;
import io.github.indraftapp.sharefile.core.exception.ShareFileUploadFinalizationException;
import io.github.indraftapp.sharefile.core.exception.ShareFileUploadNegotiationException;
import io.github.indraftapp.sharefile.core.model.enums.UploadMethod;
import io.github.indraftapp.sharefile.core.model.request.UploadRequestParams;
import io.github.indraftapp.sharefile.core.model.response.ChunkResult;
import io.github.indraftapp.sharefile.core.model.response.DownloadSpecification;
import io.github.indraftapp.sharefile.core.model.response.UploadResult;
import io.github.indraftapp.sharefile.core.model.response.UploadSpecification;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

/** Explicit ShareFile transfer client for upload and download workflows. */
@Slf4j
public final class TransferClient {
  private static final long STANDARD_UPLOAD_THRESHOLD_BYTES = 4L * 1024 * 1024;
  private static final long THREADED_UPLOAD_THRESHOLD_BYTES = 256L * 1024 * 1024;
  private static final int DOWNLOAD_BUFFER_SIZE = 64 * 1024;

  private final ShareFileHttpClient httpClient;
  private final HttpTransport storageTransport;
  private final ObjectMapper objectMapper;
  private final ShareFileConfig config;
  private final RetryConfig retryConfig;
  private final MetricsProvider metrics;
  private final ExecutorService asyncExecutor;
  private final ResourceRequestExecutor itemsExecutor;
  private final ResourceRequestExecutor sharesExecutor;

  TransferClient(
      ShareFileHttpClient httpClient,
      HttpTransport storageTransport,
      ObjectMapper objectMapper,
      ShareFileConfig config,
      RetryConfig retryConfig,
      MetricsProvider metrics) {
    this(httpClient, storageTransport, objectMapper, config, retryConfig, metrics, null);
  }

  TransferClient(
      ShareFileHttpClient httpClient,
      HttpTransport storageTransport,
      ObjectMapper objectMapper,
      ShareFileConfig config,
      RetryConfig retryConfig,
      MetricsProvider metrics,
      ExecutorService asyncExecutor) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    this.storageTransport =
        Objects.requireNonNull(storageTransport, "storageTransport must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    this.config = Objects.requireNonNull(config, "config must not be null");
    this.retryConfig = Objects.requireNonNull(retryConfig, "retryConfig must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    this.asyncExecutor = asyncExecutor == null ? ForkJoinPool.commonPool() : asyncExecutor;
    this.itemsExecutor = new ResourceRequestExecutor(httpClient, "/Items");
    this.sharesExecutor = new ResourceRequestExecutor(httpClient, "/Shares");
  }

  public UploadResult upload(String folderId, Path file, UploadOptions options) {
    Objects.requireNonNull(file, "file must not be null");
    UploadOptions resolvedOptions = options == null ? UploadOptions.defaults() : options;
    try {
      long fileSize = Files.size(file);
      return uploadInternal(
          itemsExecutor.entityActionUri(folderId, "Upload2"),
          file,
          null,
          file.getFileName().toString(),
          fileSize,
          resolvedOptions,
          new ProgressTracker(fileSize, resolvedOptions.getProgressListener()),
          new AtomicBoolean(false));
    } catch (IOException e) {
      throw new ShareFileUploadException("Failed to read upload source file", e, false, -1, 0);
    }
  }

  public UploadResult upload(
      String folderId, InputStream stream, String fileName, long fileSize, UploadOptions options) {
    UploadOptions resolvedOptions = options == null ? UploadOptions.defaults() : options;
    return uploadInternal(
        itemsExecutor.entityActionUri(folderId, "Upload2"),
        null,
        Objects.requireNonNull(stream, "stream must not be null"),
        fileName,
        fileSize,
        resolvedOptions,
        new ProgressTracker(fileSize, resolvedOptions.getProgressListener()),
        new AtomicBoolean(false));
  }

  public UploadResult uploadToShare(String shareId, Path file, UploadOptions options) {
    Objects.requireNonNull(file, "file must not be null");
    UploadOptions resolvedOptions = options == null ? UploadOptions.defaults() : options;
    try {
      long fileSize = Files.size(file);
      return uploadInternal(
          sharesExecutor.entityActionUri(shareId, "Upload2"),
          file,
          null,
          file.getFileName().toString(),
          fileSize,
          resolvedOptions,
          new ProgressTracker(fileSize, resolvedOptions.getProgressListener()),
          new AtomicBoolean(false));
    } catch (IOException e) {
      throw new ShareFileUploadException("Failed to read upload source file", e, false, -1, 0);
    }
  }

  public UploadHandle uploadAsync(String folderId, Path file, UploadOptions options) {
    UploadOptions resolvedOptions = options == null ? UploadOptions.defaults() : options;
    AtomicBoolean cancelled = new AtomicBoolean(false);
    ProgressTracker tracker =
        new ProgressTracker(estimateSize(file), resolvedOptions.getProgressListener());
    CompletableFuture<UploadResult> future =
        CompletableFuture.supplyAsync(
            () ->
                uploadInternal(
                    itemsExecutor.entityActionUri(folderId, "Upload2"),
                    file,
                    null,
                    file.getFileName().toString(),
                    estimateSize(file),
                    resolvedOptions,
                    tracker,
                    cancelled),
            asyncExecutor);
    return new UploadHandle(future, tracker.ref(), cancelled);
  }

  public UploadHandle uploadToShareAsync(String shareId, Path file, UploadOptions options) {
    UploadOptions resolvedOptions = options == null ? UploadOptions.defaults() : options;
    AtomicBoolean cancelled = new AtomicBoolean(false);
    ProgressTracker tracker =
        new ProgressTracker(estimateSize(file), resolvedOptions.getProgressListener());
    CompletableFuture<UploadResult> future =
        CompletableFuture.supplyAsync(
            () ->
                uploadInternal(
                    sharesExecutor.entityActionUri(shareId, "Upload2"),
                    file,
                    null,
                    file.getFileName().toString(),
                    estimateSize(file),
                    resolvedOptions,
                    tracker,
                    cancelled),
            asyncExecutor);
    return new UploadHandle(future, tracker.ref(), cancelled);
  }

  public void download(String itemId, Path target, DownloadOptions options) {
    DownloadOptions resolvedOptions = options == null ? DownloadOptions.defaults() : options;
    ProgressTracker tracker = new ProgressTracker(0L, resolvedOptions.getProgressListener());
    downloadInternal(itemId, target, resolvedOptions, tracker, new AtomicBoolean(false));
  }

  public InputStream downloadStream(String itemId, DownloadOptions options) {
    DownloadOptions resolvedOptions = options == null ? DownloadOptions.defaults() : options;
    DownloadSpecification specification = resolveDownloadUrl(itemId, resolvedOptions);
    URI downloadUri = requireDownloadUri(specification);
    HttpTransport.HttpResponse response =
        storageTransport.execute(
            newStorageRequest("GET", downloadUri, null, config.getDownloadTimeout()));
    int status = response.statusCode();
    if (status >= 400) {
      response.close();
      if (status == 401 || status == 403) {
        throw new ShareFileDownloadUrlExpiredException("Download URL rejected by storage zone");
      }
      throw new ShareFileDownloadException("Storage download failed with HTTP " + status);
    }
    return new FilterInputStream(response.bodyStream()) {
      @Override
      public void close() throws IOException {
        try {
          super.close();
        } finally {
          response.close();
        }
      }
    };
  }

  public DownloadSpecification resolveDownloadUrl(String itemId) {
    return resolveDownloadUrl(itemId, DownloadOptions.defaults());
  }

  public void bulkDownload(
      String parentId, List<String> itemIds, Path target, DownloadOptions options) {
    DownloadOptions resolvedOptions = options == null ? DownloadOptions.defaults() : options;
    Map<String, String> params = new LinkedHashMap<>();
    params.put("redirect", "false");
    DownloadSpecification specification =
        httpClient.post(
            itemsExecutor.uriWithParams(
                itemsExecutor.entityActionUri(parentId, "BulkDownload"), params),
            itemIds,
            DownloadSpecification.class);
    downloadToTarget(
        requireDownloadUri(specification),
        target,
        resolvedOptions,
        new ProgressTracker(0L, resolvedOptions.getProgressListener()),
        new AtomicBoolean(false));
  }

  public DownloadHandle downloadAsync(String itemId, Path target, DownloadOptions options) {
    DownloadOptions resolvedOptions = options == null ? DownloadOptions.defaults() : options;
    AtomicBoolean cancelled = new AtomicBoolean(false);
    ProgressTracker tracker = new ProgressTracker(0L, resolvedOptions.getProgressListener());
    CompletableFuture<Path> future =
        CompletableFuture.supplyAsync(
            () -> downloadInternal(itemId, target, resolvedOptions, tracker, cancelled),
            asyncExecutor);
    return new DownloadHandle(future, tracker.ref(), cancelled);
  }

  private DownloadSpecification resolveDownloadUrl(String itemId, DownloadOptions options) {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("redirect", "false");
    if (options.isIncludeAllVersions()) {
      params.put("includeAllVersions", "true");
    }
    if (options.isIncludeDeleted()) {
      params.put("includeDeleted", "true");
    }
    return httpClient.get(
        itemsExecutor.uriWithParams(itemsExecutor.entityActionUri(itemId, "Download"), params),
        DownloadSpecification.class);
  }

  private Path downloadInternal(
      String itemId,
      Path target,
      DownloadOptions options,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    DownloadSpecification specification = resolveDownloadUrl(itemId, options);
    downloadToTarget(requireDownloadUri(specification), target, options, tracker, cancelled);
    return target;
  }

  private void downloadToTarget(
      URI downloadUri,
      Path target,
      DownloadOptions options,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    Objects.requireNonNull(target, "target must not be null");
    tracker.start();
    metrics.incrementCounter(MetricNames.TRANSFER_ACTIVE, "type", "download", "event", "start");
    log.info("Starting download to {}", target);
    try (HttpTransport.HttpResponse response =
        storageTransport.execute(
            newStorageRequest("GET", downloadUri, null, config.getDownloadTimeout()))) {
      int status = response.statusCode();
      if (status >= 400) {
        if (status == 401 || status == 403) {
          throw new ShareFileDownloadUrlExpiredException("Download URL rejected by storage zone");
        }
        throw new ShareFileDownloadException("Storage download failed with HTTP " + status);
      }

      try (InputStream in = response.bodyStream();
          OutputStream out = Files.newOutputStream(target)) {
        byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
          ensureNotCancelled(cancelled, tracker);
          out.write(buffer, 0, read);
          tracker.addBytes(read);
        }
      }
      tracker.complete();
      metrics.recordValue(
          MetricNames.TRANSFER_DOWNLOAD_BYTES,
          tracker.snapshot().getBytesTransferred(),
          "result",
          "success");
      metrics.recordValue(
          MetricNames.TRANSFER_DOWNLOAD_DURATION,
          tracker.snapshot().getElapsed().toMillis(),
          "result",
          "success");
      log.info("Completed download to {}", target);
    } catch (ShareFileTransferCancelledException e) {
      tracker.cancel();
      metrics.incrementCounter(
          MetricNames.TRANSFER_ACTIVE, "type", "download", "event", "cancelled");
      throw e;
    } catch (IOException e) {
      tracker.fail();
      log.error("Download write failed for {}", target, e);
      throw new ShareFileDownloadWriteException("Failed to write downloaded bytes", e);
    } catch (RuntimeException e) {
      tracker.fail();
      log.error("Download failed for {}", target, e);
      throw e;
    }
  }

  private UploadResult uploadInternal(
      URI negotiateUri,
      Path file,
      InputStream stream,
      String fileName,
      long fileSize,
      UploadOptions options,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    tracker.start();
    metrics.incrementCounter(MetricNames.TRANSFER_ACTIVE, "type", "upload", "event", "start");
    log.info("Starting upload for {}", fileName);
    try {
      UploadMethod method = resolveUploadMethod(options.getMethod(), fileSize, file != null);
      UploadSpecification specification =
          negotiateUpload(negotiateUri, fileName, fileSize, options, method);
      UploadMethod negotiatedMethod =
          specification.getMethod() != null ? specification.getMethod() : method;
      UploadResult result =
          switch (negotiatedMethod) {
            case STANDARD, STREAMED ->
                uploadSingleRequest(
                    specification,
                    file,
                    stream,
                    fileName,
                    fileSize,
                    negotiatedMethod,
                    options,
                    tracker,
                    cancelled);
            case THREADED ->
                uploadThreaded(
                    specification, requireFile(file), fileSize, options, tracker, cancelled);
          };
      tracker.complete();
      metrics.recordValue(
          MetricNames.TRANSFER_UPLOAD_BYTES,
          tracker.snapshot().getBytesTransferred(),
          "result",
          "success");
      metrics.recordValue(
          MetricNames.TRANSFER_UPLOAD_DURATION,
          tracker.snapshot().getElapsed().toMillis(),
          "result",
          "success");
      log.info("Completed upload for {}", fileName);
      return result;
    } catch (ShareFileTransferCancelledException e) {
      tracker.cancel();
      metrics.incrementCounter(MetricNames.TRANSFER_ACTIVE, "type", "upload", "event", "cancelled");
      throw e;
    } catch (RuntimeException e) {
      tracker.fail();
      log.error("Upload failed for {}", fileName, e);
      throw e;
    }
  }

  private UploadSpecification negotiateUpload(
      URI uri, String fileName, long fileSize, UploadOptions options, UploadMethod method) {
    try {
      return httpClient.post(
          uri, buildUploadParams(fileName, fileSize, options, method), UploadSpecification.class);
    } catch (RuntimeException e) {
      throw new ShareFileUploadNegotiationException("Upload negotiation failed", e);
    }
  }

  private UploadRequestParams buildUploadParams(
      String fileName, long fileSize, UploadOptions options, UploadMethod method) {
    UploadRequestParams params = new UploadRequestParams();
    params.setMethod(method);
    params.setFileName(fileName);
    params.setFileSize(fileSize);
    params.setOverwrite(options.isOverwrite());
    params.setNotifyUsers(options.isNotifyUsers());
    params.setThreadCount(options.getThreadCount());
    params.setBatchId(options.getBatchId());
    params.setBatchLast(options.isBatchLast());
    params.setClientCreatedDate(options.getClientCreatedDate());
    params.setClientModifiedDate(options.getClientModifiedDate());
    params.setExpirationDays(options.getExpirationDays());
    return params;
  }

  private UploadResult uploadSingleRequest(
      UploadSpecification specification,
      Path file,
      InputStream providedStream,
      String fileName,
      long fileSize,
      UploadMethod method,
      UploadOptions options,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    long resumeOffset =
        options.isAutoResume()
                && Boolean.TRUE.equals(specification.getIsResume())
                && specification.getResumeOffset() != null
            ? specification.getResumeOffset()
            : 0L;
    URI chunkUri = URI.create(specification.getChunkUri());
    try (InputStream stream =
        file != null
            ? openFileStream(file, resumeOffset)
            : skipStream(providedStream, resumeOffset)) {
      HttpTransport.HttpRequest request =
          newStorageRequest(
              "POST",
              chunkUri,
              new ProgressInputStream(stream, tracker, cancelled),
              method == UploadMethod.STANDARD
                  ? OptionalLong.of(Math.max(0L, fileSize - resumeOffset))
                  : OptionalLong.empty(),
              config.getUploadTimeout());
      try (HttpTransport.HttpResponse response = storageTransport.execute(request)) {
        int status = response.statusCode();
        if (status >= 400) {
          throw new ShareFileUploadException(
              "Upload transfer failed with HTTP " + status,
              true,
              specification.getResumeIndex() == null
                  ? -1
                  : specification.getResumeIndex().intValue(),
              tracker.snapshot().getBytesTransferred());
        }
        byte[] responseBody = response.bodyBytes(10 * 1024 * 1024);
        if (responseBody.length == 0 && specification.getFinishUri() != null) {
          return finishUpload(
              specification.getFinishUri(), tracker.snapshot().getBytesTransferred());
        }
        return objectMapper.readValue(responseBody, UploadResult.class);
      }
    } catch (IOException e) {
      throw new ShareFileUploadException(
          "Upload transfer failed for " + fileName,
          e,
          true,
          specification.getResumeIndex() == null ? -1 : specification.getResumeIndex().intValue(),
          tracker.snapshot().getBytesTransferred());
    }
  }

  private UploadResult uploadThreaded(
      UploadSpecification specification,
      Path file,
      long fileSize,
      UploadOptions options,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    int chunkSize = options.getChunkSizeBytes();
    int totalChunks = Math.max(1, (int) ((fileSize + chunkSize - 1) / chunkSize));
    int startChunk =
        options.isAutoResume()
                && Boolean.TRUE.equals(specification.getIsResume())
                && specification.getResumeIndex() != null
            ? specification.getResumeIndex().intValue()
            : 0;
    tracker.setTotalChunks(totalChunks);
    for (int i = 0; i < startChunk; i++) {
      tracker.markChunkCompleted();
    }

    ExecutorService executor =
        Executors.newFixedThreadPool(
            Math.max(1, options.getThreadCount()),
            runnable -> {
              Thread thread = new Thread(runnable, "sharefile-upload-chunk");
              thread.setDaemon(true);
              return thread;
            });
    try {
      List<Future<ChunkResult>> futures =
          java.util.stream.IntStream.range(startChunk, totalChunks)
              .mapToObj(
                  chunkIndex ->
                      executor.submit(
                          () ->
                              uploadChunkWithRetry(
                                  specification,
                                  file,
                                  chunkIndex,
                                  chunkSize,
                                  fileSize,
                                  tracker,
                                  cancelled)))
              .toList();
      for (Future<ChunkResult> future : futures) {
        try {
          future.get();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new ShareFileTransferCancelledException("Threaded upload interrupted");
        } catch (Exception e) {
          Throwable cause = e.getCause();
          if (cause instanceof RuntimeException runtimeException) {
            throw runtimeException;
          }
          throw new CompletionException(cause);
        }
      }
      return finishUpload(specification.getFinishUri(), tracker.snapshot().getBytesTransferred());
    } finally {
      executor.shutdownNow();
    }
  }

  private ChunkResult uploadChunkWithRetry(
      UploadSpecification specification,
      Path file,
      int chunkIndex,
      int chunkSize,
      long fileSize,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    long offset = (long) chunkIndex * chunkSize;
    for (int attempt = 0; attempt <= retryConfig.getMaxRetries(); attempt++) {
      ensureNotCancelled(cancelled, tracker);
      try (InputStream stream =
          new ProgressInputStream(
              openChunkStream(file, offset, Math.min(chunkSize, fileSize - offset)),
              tracker,
              cancelled)) {
        HttpTransport.HttpRequest request =
            newStorageRequest(
                "POST",
                URI.create(specification.getChunkUri()),
                stream,
                OptionalLong.of(Math.min(chunkSize, fileSize - offset)),
                config.getUploadTimeout());
        try (HttpTransport.HttpResponse response = storageTransport.execute(request)) {
          int status = response.statusCode();
          if (status >= 400) {
            throw new IOException("Chunk upload failed with HTTP " + status);
          }
          byte[] responseBody = response.bodyBytes(1024 * 1024);
          ChunkResult result =
              responseBody.length == 0
                  ? new ChunkResult()
                  : objectMapper.readValue(responseBody, ChunkResult.class);
          tracker.markChunkCompleted();
          return result;
        }
      } catch (ShareFileTransferCancelledException e) {
        throw e;
      } catch (Exception e) {
        if (attempt >= retryConfig.getMaxRetries()) {
          throw new ShareFileChunkUploadException(
              "Chunk upload failed after retries",
              e,
              chunkIndex,
              offset,
              Math.max(-1, chunkIndex - 1),
              tracker.snapshot().getBytesTransferred());
        }
        log.warn("Retrying chunk {} after failure", chunkIndex, e);
        sleepBackoff(attempt);
      }
    }
    throw new ShareFileChunkUploadException(
        "Chunk upload failed after retries",
        null,
        chunkIndex,
        offset,
        Math.max(-1, chunkIndex - 1),
        tracker.snapshot().getBytesTransferred());
  }

  private UploadResult finishUpload(String finishUri, long bytesTransferred) {
    if (finishUri == null || finishUri.isBlank()) {
      throw new ShareFileUploadFinalizationException("Missing upload finish URI", bytesTransferred);
    }
    try (HttpTransport.HttpResponse response =
        storageTransport.execute(
            newStorageRequest("POST", URI.create(finishUri), null, config.getUploadTimeout()))) {
      int status = response.statusCode();
      if (status >= 400) {
        throw new ShareFileUploadFinalizationException(
            "Upload finalization failed with HTTP " + status, bytesTransferred);
      }
      return objectMapper.readValue(response.bodyBytes(10 * 1024 * 1024), UploadResult.class);
    } catch (IOException e) {
      throw new ShareFileUploadFinalizationException(
          "Failed to parse upload finalization response", e, bytesTransferred);
    }
  }

  private HttpTransport.HttpRequest newStorageRequest(
      String method, URI uri, InputStream bodyStream, Duration timeout) {
    return newStorageRequest(method, uri, bodyStream, OptionalLong.empty(), timeout);
  }

  private HttpTransport.HttpRequest newStorageRequest(
      String method,
      URI uri,
      InputStream bodyStream,
      OptionalLong contentLength,
      Duration timeout) {
    return new HttpTransport.HttpRequest() {
      @Override
      public String method() {
        return method;
      }

      @Override
      public URI uri() {
        return uri;
      }

      @Override
      public Map<String, String> headers() {
        return bodyStream == null ? Map.of() : Map.of("Content-Type", "application/octet-stream");
      }

      @Override
      public Optional<InputStream> bodyStream() {
        return Optional.ofNullable(bodyStream);
      }

      @Override
      public OptionalLong contentLength() {
        return bodyStream == null ? OptionalLong.empty() : contentLength;
      }

      @Override
      public Duration timeout() {
        return timeout;
      }
    };
  }

  private static Path requireFile(Path file) {
    if (file == null) {
      throw new IllegalArgumentException("Threaded upload requires a file-backed source");
    }
    return file;
  }

  private static URI requireDownloadUri(DownloadSpecification specification) {
    if (specification == null
        || specification.getDownloadUrl() == null
        || specification.getDownloadUrl().isBlank()) {
      throw new ShareFileDownloadUrlExpiredException(
          "Download URL was not present in the API response");
    }
    return URI.create(specification.getDownloadUrl());
  }

  private static UploadMethod resolveUploadMethod(
      UploadMethod requested, long fileSize, boolean fileBacked) {
    if (requested != null) {
      return requested;
    }
    if (!fileBacked && fileSize > THREADED_UPLOAD_THRESHOLD_BYTES) {
      return UploadMethod.STREAMED;
    }
    if (fileSize < STANDARD_UPLOAD_THRESHOLD_BYTES) {
      return UploadMethod.STANDARD;
    }
    if (fileSize <= THREADED_UPLOAD_THRESHOLD_BYTES) {
      return UploadMethod.STREAMED;
    }
    return UploadMethod.THREADED;
  }

  private static InputStream openFileStream(Path file, long skipBytes) throws IOException {
    InputStream stream = Files.newInputStream(file);
    return skipStream(stream, skipBytes);
  }

  private static InputStream skipStream(InputStream stream, long skipBytes) throws IOException {
    long remaining = skipBytes;
    while (remaining > 0) {
      long skipped = stream.skip(remaining);
      if (skipped <= 0) {
        if (stream.read() == -1) {
          break;
        }
        skipped = 1;
      }
      remaining -= skipped;
    }
    return stream;
  }

  private static InputStream openChunkStream(Path file, long offset, long length)
      throws IOException {
    SeekableByteChannel channel = Files.newByteChannel(file);
    channel.position(offset);
    return new InputStream() {
      private long remaining = length;

      @Override
      public int read() throws IOException {
        byte[] buffer = new byte[1];
        int read = read(buffer, 0, 1);
        return read == -1 ? -1 : buffer[0] & 0xFF;
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) {
          return -1;
        }
        java.nio.ByteBuffer buffer =
            java.nio.ByteBuffer.wrap(b, off, (int) Math.min(len, remaining));
        int read = channel.read(buffer);
        if (read == -1) {
          return -1;
        }
        remaining -= read;
        return read;
      }

      @Override
      public void close() throws IOException {
        channel.close();
      }
    };
  }

  private static void ensureNotCancelled(AtomicBoolean cancelled, ProgressTracker tracker) {
    if (cancelled.get() || Thread.currentThread().isInterrupted()) {
      tracker.cancel();
      throw new ShareFileTransferCancelledException("Transfer cancelled");
    }
  }

  private void sleepBackoff(int attempt) {
    try {
      long initial = retryConfig.getInitialBackoff().toMillis();
      long delay = (long) (initial * Math.pow(retryConfig.getBackoffMultiplier(), attempt));
      Thread.sleep(Math.max(0L, delay));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ShareFileTransferCancelledException("Interrupted during transfer retry backoff");
    }
  }

  private static long estimateSize(Path file) {
    try {
      return Files.size(file);
    } catch (IOException e) {
      throw new ShareFileUploadException("Failed to determine file size", e, false, -1, 0);
    }
  }

  private static final class ProgressInputStream extends FilterInputStream {
    private final ProgressTracker tracker;
    private final AtomicBoolean cancelled;

    ProgressInputStream(InputStream in, ProgressTracker tracker, AtomicBoolean cancelled) {
      super(in);
      this.tracker = tracker;
      this.cancelled = cancelled;
    }

    @Override
    public int read() throws IOException {
      ensureNotCancelled(cancelled, tracker);
      int read = super.read();
      if (read != -1) {
        tracker.addBytes(1);
      }
      return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      ensureNotCancelled(cancelled, tracker);
      int read = super.read(b, off, len);
      if (read > 0) {
        tracker.addBytes(read);
      }
      return read;
    }
  }

  private static final class ProgressTracker {
    private final AtomicReference<TransferProgress> progress =
        new AtomicReference<>(
            new TransferProgress(0L, 0L, Duration.ZERO, TransferState.PENDING, 0, 0));
    private final long totalBytes;
    private final TransferProgressListener listener;
    private volatile Instant startedAt = Instant.now();
    private volatile int totalChunks;
    private volatile int chunksCompleted;

    ProgressTracker(long totalBytes, TransferProgressListener listener) {
      this.totalBytes = Math.max(0L, totalBytes);
      this.listener = listener;
      update(0L, TransferState.PENDING);
    }

    void start() {
      startedAt = Instant.now();
      update(progress.get().getBytesTransferred(), TransferState.IN_PROGRESS);
    }

    void addBytes(long delta) {
      update(progress.get().getBytesTransferred() + delta, TransferState.IN_PROGRESS);
    }

    void setTotalChunks(int totalChunks) {
      this.totalChunks = totalChunks;
      update(progress.get().getBytesTransferred(), progress.get().getState());
    }

    void markChunkCompleted() {
      chunksCompleted++;
      update(progress.get().getBytesTransferred(), progress.get().getState());
    }

    void complete() {
      update(progress.get().getBytesTransferred(), TransferState.COMPLETED);
    }

    void fail() {
      update(progress.get().getBytesTransferred(), TransferState.FAILED);
    }

    void cancel() {
      update(progress.get().getBytesTransferred(), TransferState.CANCELLED);
    }

    TransferProgress snapshot() {
      return progress.get();
    }

    AtomicReference<TransferProgress> ref() {
      return progress;
    }

    private void update(long bytesTransferred, TransferState state) {
      TransferProgress snapshot =
          new TransferProgress(
              bytesTransferred,
              totalBytes,
              Duration.between(startedAt, Instant.now()),
              state,
              chunksCompleted,
              totalChunks);
      progress.set(snapshot);
      if (listener != null) {
        listener.onProgress(snapshot.getBytesTransferred(), snapshot.getTotalBytes());
      }
    }
  }
}
