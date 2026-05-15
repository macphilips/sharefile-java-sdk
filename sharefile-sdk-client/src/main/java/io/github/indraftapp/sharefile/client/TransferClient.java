package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
import io.github.indraftapp.sharefile.core.model.response.DownloadSpecification;
import io.github.indraftapp.sharefile.core.model.response.UploadResult;
import io.github.indraftapp.sharefile.core.model.response.UploadSpecification;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

import static java.lang.Boolean.TRUE;

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
    long fileSize = estimateSize(file);
    ProgressTracker tracker = new ProgressTracker(fileSize, resolvedOptions.getProgressListener());
    CompletableFuture<UploadResult> future =
        CompletableFuture.supplyAsync(
            () ->
                uploadInternal(
                    itemsExecutor.entityActionUri(folderId, "Upload2"),
                    file,
                    null,
                    file.getFileName().toString(),
                    fileSize,
                    resolvedOptions,
                    tracker,
                    cancelled),
            asyncExecutor);
    return new UploadHandle(future, tracker.ref(), cancelled);
  }

  public UploadHandle uploadToShareAsync(String shareId, Path file, UploadOptions options) {
    UploadOptions resolvedOptions = options == null ? UploadOptions.defaults() : options;
    AtomicBoolean cancelled = new AtomicBoolean(false);
    long fileSize = estimateSize(file);
    ProgressTracker tracker = new ProgressTracker(fileSize, resolvedOptions.getProgressListener());
    CompletableFuture<UploadResult> future =
        CompletableFuture.supplyAsync(
            () ->
                uploadInternal(
                    sharesExecutor.entityActionUri(shareId, "Upload2"),
                    file,
                    null,
                    file.getFileName().toString(),
                    fileSize,
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
    ensureNotCancelled(cancelled, tracker);
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
    ensureNotCancelled(cancelled, tracker);
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
      UploadMethod method = resolveUploadMethod(options.getMethod());
      if (method == UploadMethod.STREAMED && file == null) {
        throw new IllegalArgumentException("Streamed upload requires a file-backed source");
      }
      UploadSpecification specification =
          negotiateUpload(negotiateUri, fileName, fileSize, options, method);
      UploadMethod negotiatedMethod =
          specification.getMethod() != null ? specification.getMethod() : method;
      UploadResult result =
          switch (negotiatedMethod) {
            case STANDARD ->
                uploadStandard(
                    specification,
                    file,
                    stream,
                    fileName,
                    fileSize,
                    tracker,
                    cancelled);
            case STREAMED ->
                uploadStreamed(
                    specification,
                    requireFile(file, "Streamed upload requires a file-backed source"),
                    fileName,
                    fileSize,
                    options,
                    tracker,
                    cancelled);
            case THREADED ->
                uploadThreaded(
                    specification,
                    file,
                    stream,
                    fileName,
                    fileSize,
                    options,
                    tracker,
                    cancelled);
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
    params.setRaw(true);
    params.setFileName(fileName);
    params.setFileSize(fileSize);
    params.setOverwrite(options.isOverwrite());
    params.setNotify(options.isNotifyUsers());
    params.setThreadCount(options.getThreadCount());
    params.setCanResume(options.isAutoResume() && method != UploadMethod.STANDARD);
    params.setBatchId(options.getBatchId());
    params.setBatchLast(options.isBatchLast());
    params.setClientCreatedDate(options.getClientCreatedDate());
    params.setClientModifiedDate(options.getClientModifiedDate());
    params.setExpirationDays(options.getExpirationDays());
    return params;
  }

  private UploadResult uploadStandard(
      UploadSpecification specification,
      Path file,
      InputStream providedStream,
      String fileName,
      long fileSize,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    URI chunkUri = URI.create(specification.getChunkUri());
    try (InputStream stream = file != null ? openFileStream(file, 0L) : providedStream) {
      HttpTransport.HttpRequest request =
          newStorageRequest(
              "POST",
              chunkUri,
              new ProgressInputStream(stream, tracker, cancelled),
              OptionalLong.of(fileSize),
              config.getUploadTimeout());
      try (HttpTransport.HttpResponse response = storageTransport.execute(request)) {
        StorageUploadResponse uploadResponse =
            parseStorageUploadResponse(
                response,
                "upload transfer",
                chunkUri,
                tracker.snapshot().getBytesTransferred(),
                false);
        if (uploadResponse.result() != null) {
          return uploadResponse.result();
        }
        if (specification.getFinishUri() != null) {
          return finishUpload(
              specification.getFinishUri(),
              fileName,
              fileSize,
              tracker.snapshot().getBytesTransferred());
        }
        return fallbackUploadResult(fileName, fileSize);
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

  private UploadResult uploadStreamed(
      UploadSpecification specification,
      Path file,
      String fileName,
      long fileSize,
      UploadOptions options,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    int chunkSize = Math.max(1, options.getChunkSizeBytes());
    int totalChunks = Math.max(1, (int) ((fileSize + chunkSize - 1) / chunkSize));
    tracker.setTotalChunks(totalChunks);
    String fileHash = md5Hex(file);
    UploadResult jsonResult = null;
    for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
      ensureNotCancelled(cancelled, tracker);
      long offset = (long) chunkIndex * chunkSize;
      long chunkLength = chunkLength(chunkSize, fileSize, chunkIndex);
      Map<String, String> params = uploadChunkParams(file, chunkIndex, offset, chunkLength);
      if (chunkIndex == totalChunks - 1) {
        params.put("finish", "true");
        params.put("filehash", fileHash);
      }
      URI chunkUri = uriWithParams(URI.create(specification.getChunkUri()), params);
      try (InputStream stream = openChunkStream(file, offset, chunkLength);
          HttpTransport.HttpResponse response =
              storageTransport.execute(
                  newStorageRequest(
                      "POST",
                      chunkUri,
                      new ProgressInputStream(stream, tracker, cancelled),
                      OptionalLong.of(chunkLength),
                      config.getUploadTimeout()))) {
        StorageUploadResponse uploadResponse =
            parseStorageUploadResponse(
                response,
                "streamed upload chunk",
                chunkUri,
                tracker.snapshot().getBytesTransferred(),
                false);
        if (uploadResponse.result() != null) {
          jsonResult = uploadResponse.result();
        }
        tracker.markChunkCompleted(chunkLength);
      } catch (IOException e) {
        throw new ShareFileUploadException(
            "Streamed upload failed for " + fileName,
            e,
            true,
            Math.max(-1, chunkIndex - 1),
            tracker.snapshot().getBytesTransferred());
      }
    }
    return jsonResult == null ? fallbackUploadResult(fileName, fileSize) : jsonResult;
  }

  private UploadResult uploadThreaded(
      UploadSpecification specification,
      Path file,
      InputStream stream,
      String fileName,
      long fileSize,
      UploadOptions options,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    if (file == null) {
      return uploadThreadedStream(
          specification, stream, fileName, fileSize, options, tracker, cancelled);
    }
    return uploadThreadedFile(specification, file, fileName, fileSize, options, tracker, cancelled);
  }

  private UploadResult uploadThreadedFile(
      UploadSpecification specification,
      Path file,
      String fileName,
      long fileSize,
      UploadOptions options,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    int chunkSize = options.getChunkSizeBytes();
    int totalChunks = Math.max(1, (int) ((fileSize + chunkSize - 1) / chunkSize));
    int startChunk =
        options.isAutoResume()
                && TRUE.equals(specification.getIsResume())
                && specification.getResumeIndex() != null
            ? specification.getResumeIndex().intValue()
            : 0;
    tracker.setTotalChunks(totalChunks);
    for (int i = 0; i < startChunk; i++) {
      tracker.markChunkCompleted(chunkLength(chunkSize, fileSize, i));
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
      List<Future<StorageUploadResponse>> futures =
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
      UploadResult chunkUploadResult = null;
      for (Future<StorageUploadResponse> future : futures) {
        try {
          StorageUploadResponse uploadResponse = future.get();
          if (uploadResponse.result() != null) {
            chunkUploadResult = uploadResponse.result();
          }
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
      UploadResult finishResult =
          finishUpload(
              specification.getFinishUri(),
              fileName,
              fileSize,
              tracker.snapshot().getBytesTransferred());
      return finishResult.getItemId() == null && chunkUploadResult != null
          ? chunkUploadResult
          : finishResult;
    } finally {
      executor.shutdownNow();
    }
  }

  private StorageUploadResponse uploadChunkWithRetry(
      UploadSpecification specification,
      Path file,
      int chunkIndex,
      int chunkSize,
      long fileSize,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    long offset = (long) chunkIndex * chunkSize;
      long chunkLength = chunkLength(chunkSize, fileSize, chunkIndex);
    for (int attempt = 0; attempt <= retryConfig.getMaxRetries(); attempt++) {
      ensureNotCancelled(cancelled, tracker);
      try (InputStream stream = openChunkStream(file, offset, chunkLength)) {
        Map<String, String> params = uploadChunkParams(file, chunkIndex, offset, chunkLength);
        params.put("fmt", "json");
        URI chunkUri = uriWithParams(URI.create(specification.getChunkUri()), params);
        HttpTransport.HttpRequest request =
            newStorageRequest(
                "POST",
                chunkUri,
                stream,
                OptionalLong.of(chunkLength),
                config.getUploadTimeout());
        try (HttpTransport.HttpResponse response = storageTransport.execute(request)) {
          StorageUploadResponse uploadResponse =
              parseStorageUploadResponse(
              response,
              "threaded upload chunk",
              chunkUri,
              tracker.snapshot().getBytesTransferred(),
              false);
          tracker.markChunkCompleted(chunkLength);
          return uploadResponse;
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

  private UploadResult uploadThreadedStream(
      UploadSpecification specification,
      InputStream stream,
      String fileName,
      long fileSize,
      UploadOptions options,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    int chunkSize = Math.max(1, options.getChunkSizeBytes());
    int totalChunks = Math.max(1, (int) ((fileSize + chunkSize - 1) / chunkSize));
    long resumeOffset =
        options.isAutoResume()
                && TRUE.equals(specification.getIsResume())
                && specification.getResumeOffset() != null
            ? specification.getResumeOffset()
            : 0L;
    int startChunk =
        options.isAutoResume()
                && TRUE.equals(specification.getIsResume())
                && specification.getResumeIndex() != null
            ? specification.getResumeIndex().intValue()
            : (int) (resumeOffset / chunkSize);
    tracker.setTotalChunks(totalChunks);
    for (int i = 0; i < startChunk; i++) {
      tracker.markChunkCompleted(chunkLength(chunkSize, fileSize, i));
    }

    UploadResult chunkUploadResult = null;
    try (InputStream uploadStream = skipStream(stream, resumeOffset)) {
      long offset = resumeOffset;
      int chunkIndex = startChunk;
      while (offset < fileSize || (fileSize == 0L && chunkIndex == 0)) {
        ensureNotCancelled(cancelled, tracker);
        long chunkLength = Math.min(chunkSize, Math.max(0L, fileSize - offset));
        byte[] chunk = readChunk(uploadStream, chunkLength);
        StorageUploadResponse uploadResponse =
            uploadBufferedChunkWithRetry(
                specification,
                chunk,
                chunkIndex,
                offset,
                tracker,
                cancelled);
        if (uploadResponse.result() != null) {
          chunkUploadResult = uploadResponse.result();
        }
        tracker.markChunkCompleted(chunk.length);
        offset += chunk.length;
        chunkIndex++;
        if (fileSize == 0L) {
          break;
        }
      }
    } catch (IOException e) {
      throw new ShareFileUploadException(
          "Threaded stream upload failed for " + fileName,
          e,
          true,
          Math.max(-1, startChunk - 1),
          tracker.snapshot().getBytesTransferred());
    }

    UploadResult finishResult =
        finishUpload(
            specification.getFinishUri(), fileName, fileSize, tracker.snapshot().getBytesTransferred());
    return finishResult.getItemId() == null && chunkUploadResult != null
        ? chunkUploadResult
        : finishResult;
  }

  private StorageUploadResponse uploadBufferedChunkWithRetry(
      UploadSpecification specification,
      byte[] chunk,
      int chunkIndex,
      long offset,
      ProgressTracker tracker,
      AtomicBoolean cancelled) {
    for (int attempt = 0; attempt <= retryConfig.getMaxRetries(); attempt++) {
      ensureNotCancelled(cancelled, tracker);
      Map<String, String> params = new LinkedHashMap<>();
      params.put("index", Integer.toString(chunkIndex));
      params.put("byteOffset", Long.toString(offset));
      params.put("hash", md5Hex(chunk));
      params.put("fmt", "json");
      URI chunkUri = uriWithParams(URI.create(specification.getChunkUri()), params);
      HttpTransport.HttpRequest request =
          newStorageRequest(
              "POST",
              chunkUri,
              new ByteArrayInputStream(chunk),
              OptionalLong.of(chunk.length),
              config.getUploadTimeout());
      try (HttpTransport.HttpResponse response = storageTransport.execute(request)) {
        return parseStorageUploadResponse(
            response,
            "threaded upload chunk",
            chunkUri,
            tracker.snapshot().getBytesTransferred(),
            false);
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
        log.warn("Retrying stream chunk {} after failure", chunkIndex, e);
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

  private UploadResult finishUpload(
      String finishUri, String fileName, long fileSize, long bytesTransferred) {
    if (finishUri == null || finishUri.isBlank()) {
      throw new ShareFileUploadFinalizationException("Missing upload finish URI", bytesTransferred);
    }
    try (HttpTransport.HttpResponse response =
        storageTransport.execute(
            newStorageRequest(
                "POST",
                uriWithParams(URI.create(finishUri), Map.of("fmt", "json")),
                null,
                config.getUploadTimeout()))) {

      StorageUploadResponse uploadResponse =
          parseStorageUploadResponse(
              response, "upload finalization", URI.create(finishUri), bytesTransferred, true);
      return uploadResponse.result() == null
          ? fallbackUploadResult(fileName, fileSize)
          : uploadResponse.result();
    } catch (IOException e) {
      throw new ShareFileUploadFinalizationException(
          "Failed to parse upload finalization response", e, bytesTransferred);
    }
  }

  private StorageUploadResponse parseStorageUploadResponse(
      HttpTransport.HttpResponse response,
      String phase,
      URI uri,
      long bytesTransferred,
      boolean finalization)
      throws IOException {
    int status = response.statusCode();
    byte[] responseBody = response.bodyBytes(10 * 1024 * 1024);
      String preview = bodyPreview(responseBody);

    if (status >= 400) {
      throwStorageUploadException(
          phase, "failed with HTTP %d: %s".formatted(status, preview), null, finalization, bytesTransferred);
    }
    if (responseBody.length == 0
        || "OK".equalsIgnoreCase(preview)
        || preview.regionMatches(true, 0, "OK:", 0, "OK:".length())) {
      return new StorageUploadResponse(null);
    }
    if (preview.regionMatches(true, 0, "ERROR:", 0, "ERROR:".length())) {
      throwStorageUploadException(
          phase, "returned " + preview, null, finalization, bytesTransferred);
    }
    if (preview.startsWith("{")) {
      try {
        return new StorageUploadResponse(parseUploadResultJson(responseBody));
      } catch (IOException e) {
        throwStorageUploadException(
            phase, "returned invalid JSON: " + preview, e, finalization, bytesTransferred);
      }
    }
    throwStorageUploadException(
        phase,
        "returned unexpected response: status="
            + status
            + ", contentType="
            + firstHeader(response.headers(), "Content-Type")
            + ", bodyPreview="
            + preview,
        null,
        finalization,
        bytesTransferred);
    throw new IllegalStateException("unreachable");
  }

  private UploadResult parseUploadResultJson(byte[] responseBody) throws IOException {
    JsonNode node = objectMapper.readTree(responseBody);
    JsonNode value = node.get("value");
    if (value != null && value.isArray() && !value.isEmpty()) {
      return uploadResultFromStorageNode(value.get(0));
    }
    return objectMapper.treeToValue(node, UploadResult.class);
  }

  private UploadResult uploadResultFromStorageNode(JsonNode node) {
    UploadResult result = new UploadResult();
    result.setItemId(textValue(node, "id"));
    result.setFileName(textValue(node, "filename"));
    JsonNode size = node.get("size");
    if (size != null && size.canConvertToLong()) {
      result.setFileSize(size.longValue());
    }
    return result;
  }

  private String textValue(JsonNode node, String fieldName) {
    JsonNode value = node.get(fieldName);
    return value == null || value.isNull() ? null : value.asText();
  }

  private void throwStorageUploadException(
      String phase,
      String message,
      Throwable cause,
      boolean finalization,
      long bytesTransferred) {
    String fullMessage = "ShareFile " + phase + " " + message;
    if (finalization) {
      throw cause == null
          ? new ShareFileUploadFinalizationException(fullMessage, bytesTransferred)
          : new ShareFileUploadFinalizationException(fullMessage, cause, bytesTransferred);
    }
    throw cause == null
        ? new ShareFileUploadException(fullMessage, true, -1, bytesTransferred)
        : new ShareFileUploadException(fullMessage, cause, true, -1, bytesTransferred);
  }

  private UploadResult fallbackUploadResult(String fileName, long fileSize) {
    UploadResult result = new UploadResult();
    result.setFileName(fileName);
    result.setFileSize(fileSize);
    return result;
  }

  private Map<String, String> uploadChunkParams(
      Path file, int chunkIndex, long offset, long chunkLength) {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("index", Integer.toString(chunkIndex));
    params.put("byteOffset", Long.toString(offset));
    params.put("hash", md5Hex(file, offset, chunkLength));
    return params;
  }

  private URI uriWithParams(URI uri, Map<String, String> params) {
    if (params == null || params.isEmpty()) {
      return uri;
    }
    StringBuilder builder = new StringBuilder(uri.toString());
    builder.append(uri.getQuery() == null || uri.getQuery().isEmpty() ? '?' : '&');
    boolean first = true;
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (!first) {
        builder.append('&');
      }
      builder.append(encode(entry.getKey()));
      builder.append('=');
      builder.append(encode(entry.getValue()));
      first = false;
    }
    return URI.create(builder.toString());
  }

  private String md5Hex(Path file) {
    try (InputStream stream = Files.newInputStream(file)) {
      MessageDigest digest = newMd5Digest();
      byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
      int read;
      while ((read = stream.read(buffer)) != -1) {
        digest.update(buffer, 0, read);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (IOException e) {
      throw new ShareFileUploadException("Failed to hash upload source file", e, true, -1, 0);
    }
  }

  private String md5Hex(Path file, long offset, long length) {
    try (InputStream stream = openChunkStream(file, offset, length)) {
      MessageDigest digest = newMd5Digest();
      byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
      long remaining = length;
      while (remaining > 0) {
        int read = stream.read(buffer, 0, (int) Math.min(buffer.length, remaining));
        if (read == -1) {
          break;
        }
        digest.update(buffer, 0, read);
        remaining -= read;
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (IOException e) {
      throw new ShareFileUploadException("Failed to hash upload chunk", e, true, -1, 0);
    }
  }

  private String md5Hex(byte[] bytes) {
    MessageDigest digest = newMd5Digest();
    digest.update(bytes);
    return HexFormat.of().formatHex(digest.digest());
  }

  private byte[] readChunk(InputStream stream, long expectedLength) throws IOException {
    if (expectedLength == 0L) {
      return new byte[0];
    }
    byte[] chunk = new byte[Math.toIntExact(expectedLength)];
    int offset = 0;
    while (offset < chunk.length) {
      int read = stream.read(chunk, offset, chunk.length - offset);
      if (read == -1) {
        throw new IOException(
          "Upload stream ended before expected chunk length: expected %d bytes, read %d bytes".formatted(chunk.length, offset));
      }
      offset += read;
    }
    return chunk;
  }

  private MessageDigest newMd5Digest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 digest is not available", e);
    }
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private record StorageUploadResponse(UploadResult result) {}

  private String firstHeader(Map<String, List<String>> headers, String name) {
    if (headers == null || headers.isEmpty()) {
      return "<missing>";
    }
    return headers.entrySet().stream()
        .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(name))
        .flatMap(entry -> entry.getValue().stream())
        .findFirst()
        .orElse("<missing>");
  }

  private String bodyPreview(byte[] body) {
    if (body == null || body.length == 0) {
      return "<empty>";
    }
    String value = new String(body, 0, Math.min(body.length, 256), StandardCharsets.UTF_8)
        .replaceAll("[\\r\\n\\t]+", " ")
        .trim();
    return body.length > 256 ? value + "... (" + body.length + " bytes)" : value;
  }

  private String safeUri(URI uri) {
    if (uri == null) {
      return "<missing>";
    }
    StringBuilder builder = new StringBuilder();
    if (uri.getScheme() != null) {
      builder.append(uri.getScheme()).append("://");
    }
    if (uri.getHost() != null) {
      builder.append(uri.getHost());
    }
    if (uri.getPath() != null) {
      builder.append(uri.getPath());
    }
    return builder.isEmpty() ? uri.toString() : builder.toString();
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

  private static Path requireFile(Path file, String message) {
    if (file == null) {
      throw new IllegalArgumentException(message);
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

  private static UploadMethod resolveUploadMethod(UploadMethod requested) {
    if (requested != null) {
      return requested;
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

  private static long chunkLength(int chunkSize, long fileSize, int chunkIndex) {
    long offset = (long) chunkIndex * chunkSize;
    return Math.min(chunkSize, fileSize - offset);
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
    private final AtomicLong bytesTransferred = new AtomicLong();
    private final AtomicReference<TransferProgress> progress =
        new AtomicReference<>(
            new TransferProgress(0L, 0L, Duration.ZERO, TransferState.PENDING, 0, 0));
    private final long totalBytes;
    private final TransferProgressListener listener;
    private volatile Instant startedAt = Instant.now();
    private volatile int totalChunks;
    private final AtomicInteger chunksCompleted = new AtomicInteger();

    ProgressTracker(long totalBytes, TransferProgressListener listener) {
      this.totalBytes = Math.max(0L, totalBytes);
      this.listener = listener;
      update(0L, TransferState.PENDING);
    }

    void start() {
      startedAt = Instant.now();
      update(bytesTransferred.get(), TransferState.IN_PROGRESS);
    }

    void addBytes(long delta) {
      update(bytesTransferred.addAndGet(delta), TransferState.IN_PROGRESS);
    }

    void setTotalChunks(int totalChunks) {
      this.totalChunks = totalChunks;
      update(bytesTransferred.get(), progress.get().getState());
    }

    void markChunkCompleted(long committedBytes) {
      chunksCompleted.incrementAndGet();
      update(bytesTransferred.addAndGet(committedBytes), progress.get().getState());
    }

    void complete() {
      update(bytesTransferred.get(), TransferState.COMPLETED);
    }

    void fail() {
      update(bytesTransferred.get(), TransferState.FAILED);
    }

    void cancel() {
      update(bytesTransferred.get(), TransferState.CANCELLED);
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
              chunksCompleted.get(),
              totalChunks);
      progress.set(snapshot);
      if (listener != null) {
        listener.onProgress(snapshot.getBytesTransferred(), snapshot.getTotalBytes());
      }
    }
  }
}
