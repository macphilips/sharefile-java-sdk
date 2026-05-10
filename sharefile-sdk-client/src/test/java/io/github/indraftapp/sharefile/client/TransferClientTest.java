package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.client.internal.RecordingMetricsProvider;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.spi.MetricsProvider;
import io.github.indraftapp.sharefile.core.exception.ShareFileDownloadUrlExpiredException;
import io.github.indraftapp.sharefile.core.model.enums.UploadMethod;
import io.github.indraftapp.sharefile.core.model.response.UploadResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransferClientTest {

  @TempDir Path tempDir;

  @Test
  void standardUploadNegotiatesAndPostsUnauthenticatedStorageBody() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "Method": "Standard",
          "ChunkUri": "https://storage.example.com/upload"
        }
        """);
    transport.enqueueJsonResponse(
        200,
        """
        {
          "ItemId": "item-1",
          "FileName": "hello.txt",
          "FileSize": 5
        }
        """);

    Path file = Files.writeString(tempDir.resolve("hello.txt"), "hello", StandardCharsets.UTF_8);
    RecordingMetricsProvider metrics = new RecordingMetricsProvider();

    try (ClientTestSupport.TestContext context =
        ClientTestSupport.createContext(
            transport,
            io.github.indraftapp.sharefile.client.retry.RetryConfig.builder().maxRetries(0).build(),
            metrics)) {
      UploadResult result =
          context.transferClient().upload("folder-1", file, UploadOptions.defaults());

      assertEquals("item-1", result.getItemId());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(folder-1)/Upload2",
          transport.requests.get(0).uri().toString());
      assertTrue(transport.requests.get(0).body().contains("\"Method\":\"Standard\""));
      assertEquals(
          "https://storage.example.com/upload", transport.requests.get(1).uri().toString());
      assertEquals("POST", transport.requests.get(1).method());
      assertEquals("hello", transport.requests.get(1).body());
      assertFalse(transport.requests.get(1).headers().containsKey("Authorization"));
      assertTrue(metrics.values.contains(MetricNames.TRANSFER_UPLOAD_BYTES));
      assertTrue(metrics.values.contains(MetricNames.TRANSFER_UPLOAD_DURATION));
    }
  }

  @Test
  void threadedUploadUsesMultipleChunkPostsAndFinishUri() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "Method": "Threaded",
          "ChunkUri": "https://storage.example.com/chunk",
          "FinishUri": "https://storage.example.com/finish"
        }
        """);
    transport.enqueueJsonResponse(200, "{\"ChunkNumber\":0,\"IsComplete\":false}");
    transport.enqueueJsonResponse(200, "{\"ChunkNumber\":1,\"IsComplete\":true}");
    transport.enqueueJsonResponse(
        200,
        """
        {
          "ItemId": "item-2",
          "FileName": "threaded.bin",
          "FileSize": 10
        }
        """);

    Path file =
        Files.write(tempDir.resolve("threaded.bin"), "abcdefghij".getBytes(StandardCharsets.UTF_8));

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      UploadResult result =
          context
              .transferClient()
              .upload(
                  "folder-1",
                  file,
                  UploadOptions.builder()
                      .method(UploadMethod.THREADED)
                      .chunkSizeBytes(5)
                      .threadCount(2)
                      .build());

      assertEquals("item-2", result.getItemId());
      assertEquals(4, transport.requests.size());
      assertEquals("https://storage.example.com/chunk", transport.requests.get(1).uri().toString());
      assertEquals("https://storage.example.com/chunk", transport.requests.get(2).uri().toString());
      assertEquals(
          "https://storage.example.com/finish", transport.requests.get(3).uri().toString());
    }
  }

  @Test
  void resumeUploadStartsAtResumeIndex() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "Method": "Threaded",
          "ChunkUri": "https://storage.example.com/chunk",
          "FinishUri": "https://storage.example.com/finish",
          "IsResume": true,
          "ResumeIndex": 1
        }
        """);
    transport.enqueueJsonResponse(200, "{\"ChunkNumber\":1,\"IsComplete\":true}");
    transport.enqueueJsonResponse(
        200,
        """
        {
          "ItemId": "item-3",
          "FileName": "resume.bin",
          "FileSize": 10
        }
        """);

    Path file =
        Files.write(tempDir.resolve("resume.bin"), "abcdefghij".getBytes(StandardCharsets.UTF_8));

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      context
          .transferClient()
          .upload(
              "folder-1",
              file,
              UploadOptions.builder().method(UploadMethod.THREADED).chunkSizeBytes(5).build());

      assertEquals(3, transport.requests.size());
      assertEquals("fghij", transport.requests.get(1).body());
    }
  }

  @Test
  void downloadStreamsBytesToFileAndDownloadStreamUsesStorageUrl() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        "{\"DownloadUrl\":\"https://storage.example.com/download.bin\",\"PrepStatus\":\"Ready\"}");
    transport.enqueueResponse(200, "payload".getBytes(StandardCharsets.UTF_8));
    transport.enqueueJsonResponse(
        200,
        "{\"DownloadUrl\":\"https://storage.example.com/download.bin\",\"PrepStatus\":\"Ready\"}");
    transport.enqueueResponse(200, "payload".getBytes(StandardCharsets.UTF_8));

    Path target = tempDir.resolve("download.bin");
    RecordingMetricsProvider metrics = new RecordingMetricsProvider();

    try (ClientTestSupport.TestContext context =
        ClientTestSupport.createContext(
            transport,
            io.github.indraftapp.sharefile.client.retry.RetryConfig.builder().maxRetries(0).build(),
            metrics)) {
      context.transferClient().download("item-1", target, DownloadOptions.defaults());
      byte[] bytes;
      try (var in = context.transferClient().downloadStream("item-1", DownloadOptions.defaults())) {
        bytes = in.readAllBytes();
      }

      assertArrayEquals("payload".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));
      assertArrayEquals("payload".getBytes(StandardCharsets.UTF_8), bytes);
      assertEquals(
          "https://storage.example.com/download.bin", transport.requests.get(1).uri().toString());
      assertEquals(
          "https://storage.example.com/download.bin", transport.requests.get(3).uri().toString());
      assertFalse(transport.requests.get(1).headers().containsKey("Authorization"));
      assertTrue(metrics.values.contains(MetricNames.TRANSFER_DOWNLOAD_BYTES));
      assertTrue(metrics.values.contains(MetricNames.TRANSFER_DOWNLOAD_DURATION));
    }
  }

  @Test
  void failedDownloadDoesNotTruncateExistingTarget() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        "{\"DownloadUrl\":\"https://storage.example.com/download.bin\",\"PrepStatus\":\"Ready\"}");
    transport.enqueueResponse(403, "expired".getBytes(StandardCharsets.UTF_8));

    Path target =
        Files.writeString(tempDir.resolve("existing.bin"), "keep-me", StandardCharsets.UTF_8);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      assertThrows(
          ShareFileDownloadUrlExpiredException.class,
          () -> context.transferClient().download("item-1", target, DownloadOptions.defaults()));

      assertEquals("keep-me", Files.readString(target, StandardCharsets.UTF_8));
    }
  }

  @Test
  void uploadAsyncCompletesAndEmitsProgress() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "Method": "Streamed",
          "ChunkUri": "https://storage.example.com/upload"
        }
        """);
    transport.enqueueJsonResponse(
        200,
        """
        {
          "ItemId": "item-4",
          "FileName": "async.txt",
          "FileSize": 11
        }
        """);

    Path file =
        Files.writeString(tempDir.resolve("async.txt"), "hello world", StandardCharsets.UTF_8);
    List<Long> progressEvents = new CopyOnWriteArrayList<>();

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      UploadHandle handle =
          context
              .transferClient()
              .uploadAsync(
                  "folder-1",
                  file,
                  UploadOptions.builder()
                      .progressListener((sent, total) -> progressEvents.add(sent))
                      .build());

      UploadResult result = handle.awaitOrThrow(java.time.Duration.ofSeconds(5));

      assertEquals("item-4", result.getItemId());
      assertFalse(progressEvents.isEmpty());
      assertEquals(TransferState.COMPLETED, handle.progress().getState());
    }
  }

  @Test
  void asyncWrappersUseInjectedExecutor() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "Method": "Streamed",
          "ChunkUri": "https://storage.example.com/upload"
        }
        """);
    transport.enqueueJsonResponse(
        200,
        """
        {
          "ItemId": "item-5",
          "FileName": "executor.txt",
          "FileSize": 4
        }
        """);

    Path file = Files.writeString(tempDir.resolve("executor.txt"), "data", StandardCharsets.UTF_8);
    RetryConfig retryConfig = RetryConfig.builder().maxRetries(0).build();
    TrackingExecutor executor = new TrackingExecutor();

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      ShareFileHttpClient httpClient =
          new ShareFileHttpClient(
              transport,
              context.tokenManager(),
              ClientTestSupport.MAPPER,
              retryConfig,
              MetricsProvider.noop(),
              ClientTestSupport.BASE_URL,
              Duration.ofSeconds(30));
      TransferClient transferClient =
          new TransferClient(
              httpClient,
              transport,
              ClientTestSupport.MAPPER,
              io.github.indraftapp.sharefile.client.config.ShareFileConfig.builder()
                  .subdomain("testco")
                  .build(),
              retryConfig,
              MetricsProvider.noop(),
              executor);

      UploadHandle handle = transferClient.uploadAsync("folder-1", file, UploadOptions.defaults());
      UploadResult result = handle.awaitOrThrow(Duration.ofSeconds(5));

      assertEquals("item-5", result.getItemId());
      assertTrue(executor.submissions > 0);
    }
  }

  private static final class TrackingExecutor extends AbstractExecutorService {
    private boolean shutdown;
    private int submissions;

    @Override
    public void shutdown() {
      shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
      shutdown = true;
      return List.of();
    }

    @Override
    public boolean isShutdown() {
      return shutdown;
    }

    @Override
    public boolean isTerminated() {
      return shutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return true;
    }

    @Override
    public void execute(Runnable command) {
      submissions++;
      command.run();
    }
  }
}
