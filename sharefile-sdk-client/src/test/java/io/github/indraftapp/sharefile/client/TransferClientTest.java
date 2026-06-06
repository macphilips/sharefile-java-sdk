package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.client.internal.RecordingMetricsProvider;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.spi.MetricsProvider;
import io.github.indraftapp.sharefile.core.exception.ShareFileDownloadUrlExpiredException;
import io.github.indraftapp.sharefile.core.exception.ShareFileUploadFinalizationException;
import io.github.indraftapp.sharefile.core.model.enums.UploadMethod;
import io.github.indraftapp.sharefile.core.model.response.UploadResult;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    transport.enqueueResponse(200, new byte[0]);

    Path file = Files.writeString(tempDir.resolve("hello.txt"), "hello", StandardCharsets.UTF_8);
    RecordingMetricsProvider metrics = new RecordingMetricsProvider();

    try (ClientTestSupport.TestContext context =
        ClientTestSupport.createContext(
            transport,
            io.github.indraftapp.sharefile.client.retry.RetryConfig.builder().maxRetries(0).build(),
            metrics)) {
      UploadResult result =
          context
              .transferClient()
              .upload(
                  "folder-1", file, UploadOptions.builder().method(UploadMethod.STANDARD).build());

      assertNull(result.getItemId());
      assertEquals("hello.txt", result.getFileName());
      assertEquals(5L, result.getFileSize());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(folder-1)/Upload2",
          transport.requests.get(0).uri().toString());
      assertTrue(transport.requests.get(0).body().contains("\"Method\":\"Standard\""));
      assertTrue(transport.requests.get(0).body().contains("\"Raw\":true"));
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
  void standardUploadSucceedsWithOkStorageResponse() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "Method": "Standard",
          "ChunkUri": "https://storage.example.com/upload"
        }
        """);
    transport.enqueueResponse(200, "OK".getBytes(StandardCharsets.UTF_8));

    Path file = Files.writeString(tempDir.resolve("hello.txt"), "hello", StandardCharsets.UTF_8);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      UploadResult result =
          context
              .transferClient()
              .upload(
                  "folder-1", file, UploadOptions.builder().method(UploadMethod.STANDARD).build());

      assertNull(result.getItemId());
      assertEquals("hello.txt", result.getFileName());
      assertEquals(5L, result.getFileSize());
      assertEquals(2, transport.requests.size());
      assertEquals(
          "https://storage.example.com/upload", transport.requests.get(1).uri().toString());
    }
  }

  @Test
  void standardUploadSucceedsWithOkFilenameStorageResponse() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "Method": "Standard",
          "ChunkUri": "https://storage.example.com/upload"
        }
        """);
    transport.enqueueResponse(200, "OK:hello.txt".getBytes(StandardCharsets.UTF_8));

    Path file = Files.writeString(tempDir.resolve("hello.txt"), "hello", StandardCharsets.UTF_8);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      UploadResult result =
          context
              .transferClient()
              .upload(
                  "folder-1", file, UploadOptions.builder().method(UploadMethod.STANDARD).build());

      assertNull(result.getItemId());
      assertEquals("hello.txt", result.getFileName());
      assertEquals(5L, result.getFileSize());
    }
  }

  @Test
  void standardUploadFailsCleanlyWithErrorStorageResponse() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "Method": "Standard",
          "ChunkUri": "https://storage.example.com/upload"
        }
        """);
    transport.enqueueResponse(200, "ERROR:System error occurred".getBytes(StandardCharsets.UTF_8));

    Path file = Files.writeString(tempDir.resolve("hello.txt"), "hello", StandardCharsets.UTF_8);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      io.github.indraftapp.sharefile.core.exception.ShareFileUploadException exception =
          assertThrows(
              io.github.indraftapp.sharefile.core.exception.ShareFileUploadException.class,
              () ->
                  context
                      .transferClient()
                      .upload(
                          "folder-1",
                          file,
                          UploadOptions.builder().method(UploadMethod.STANDARD).build()));

      assertTrue(exception.getMessage().contains("ERROR:System error occurred"));
    }
  }

  @Test
  void defaultFileBackedUploadUsesThreadedUploaderForItemId() throws Exception {
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
    transport.enqueueResponse(200, "OK".getBytes(StandardCharsets.UTF_8));
    transport.enqueueJsonResponse(
        200,
        """
        {
          "value": [
            {
              "id": "item-1",
              "filename": "hello.txt",
              "size": 5
            }
          ],
          "error": false
        }
        """);

    Path file = Files.writeString(tempDir.resolve("hello.txt"), "hello", StandardCharsets.UTF_8);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      UploadResult result =
          context.transferClient().upload("folder-1", file, UploadOptions.defaults());

      assertEquals("item-1", result.getItemId());
      assertEquals("hello.txt", result.getFileName());
      assertEquals(5L, result.getFileSize());
      assertTrue(transport.requests.get(0).body().contains("\"Method\":\"Threaded\""));
      assertEquals(
          "https://storage.example.com/chunk?index=0&byteOffset=0&hash=5d41402abc4b2a76b9719d911017c592&fmt=json",
          transport.requests.get(1).uri().toString());
      assertEquals(
          "https://storage.example.com/finish?fmt=json",
          transport.requests.get(2).uri().toString());
    }
  }

  @Test
  void threadedFinalizationFailsCleanlyWithErrorStorageResponse() throws Exception {
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
    transport.enqueueResponse(200, "OK".getBytes(StandardCharsets.UTF_8));
    transport.enqueueResponse(200, "ERROR:finalization failed".getBytes(StandardCharsets.UTF_8));

    Path file =
        Files.write(tempDir.resolve("threaded.bin"), "abcde".getBytes(StandardCharsets.UTF_8));

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      ShareFileUploadFinalizationException exception =
          assertThrows(
              ShareFileUploadFinalizationException.class,
              () ->
                  context
                      .transferClient()
                      .upload(
                          "folder-1",
                          file,
                          UploadOptions.builder()
                              .method(UploadMethod.THREADED)
                              .chunkSizeBytes(5)
                              .threadCount(1)
                              .build()));

      assertTrue(exception.getMessage().contains("ERROR:finalization failed"));
    }
  }

  @Test
  void streamedUploadAppendsFinishAndFileHashToFinalChunk() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "Method": "Streamed",
          "ChunkUri": "https://storage.example.com/stream"
        }
        """);
    transport.enqueueResponse(200, "OK".getBytes(StandardCharsets.UTF_8));

    Path file =
        Files.writeString(tempDir.resolve("streamed.txt"), "hello world", StandardCharsets.UTF_8);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      UploadResult result =
          context
              .transferClient()
              .upload(
                  "folder-1",
                  file,
                  UploadOptions.builder().method(UploadMethod.STREAMED).chunkSizeBytes(16).build());

      assertNull(result.getItemId());
      assertEquals(
          "https://storage.example.com/stream?index=0&byteOffset=0&hash=5eb63bbbe01eeed093cb22bb8f5acdc3&finish=true&filehash=5eb63bbbe01eeed093cb22bb8f5acdc3",
          transport.requests.get(1).uri().toString());
    }
  }

  @Test
  void defaultInputStreamUploadUsesThreadedUploaderForItemId() throws Exception {
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
    transport.enqueueResponse(200, "OK".getBytes(StandardCharsets.UTF_8));
    transport.enqueueJsonResponse(
        200,
        """
        {
          "ItemId": "item-stream",
          "FileName": "stream.bin",
          "FileSize": 5
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      UploadResult result =
          context
              .transferClient()
              .upload(
                  "folder-1",
                  new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
                  "stream.bin",
                  5L,
                  UploadOptions.defaults());

      assertEquals("item-stream", result.getItemId());
      assertTrue(transport.requests.get(0).body().contains("\"Method\":\"Threaded\""));
      assertTrue(transport.requests.get(0).body().contains("\"Raw\":true"));
      assertEquals(
          "https://storage.example.com/chunk?index=0&byteOffset=0&hash=5d41402abc4b2a76b9719d911017c592&fmt=json",
          transport.requests.get(1).uri().toString());
      assertEquals("hello", transport.requests.get(1).body());
      assertEquals(
          "https://storage.example.com/finish?fmt=json",
          transport.requests.get(2).uri().toString());
    }
  }

  @Test
  void threadedInputStreamUploadRetriesBufferedChunkWithoutRereadingStream() throws Exception {
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
    transport.enqueueResponse(500, "temporary".getBytes(StandardCharsets.UTF_8));
    transport.enqueueResponse(200, "OK".getBytes(StandardCharsets.UTF_8));
    transport.enqueueResponse(200, "OK".getBytes(StandardCharsets.UTF_8));
    transport.enqueueJsonResponse(
        200,
        """
        {
          "ItemId": "item-stream",
          "FileName": "stream.bin",
          "FileSize": 6
        }
        """);

    try (ClientTestSupport.TestContext context =
        ClientTestSupport.createContext(
            transport, RetryConfig.builder().maxRetries(1).initialBackoff(Duration.ZERO).build())) {
      UploadResult result =
          context
              .transferClient()
              .upload(
                  "folder-1",
                  new ByteArrayInputStream("abcdef".getBytes(StandardCharsets.UTF_8)),
                  "stream.bin",
                  6L,
                  UploadOptions.builder().chunkSizeBytes(3).build());

      assertEquals("item-stream", result.getItemId());
      assertEquals("abc", transport.requests.get(1).body());
      assertEquals("abc", transport.requests.get(2).body());
      assertEquals("def", transport.requests.get(3).body());
      assertEquals(
          "https://storage.example.com/chunk?index=0&byteOffset=0&hash=900150983cd24fb0d6963f7d28e17f72&fmt=json",
          transport.requests.get(1).uri().toString());
      assertEquals(
          "https://storage.example.com/chunk?index=0&byteOffset=0&hash=900150983cd24fb0d6963f7d28e17f72&fmt=json",
          transport.requests.get(2).uri().toString());
      assertEquals(
          "https://storage.example.com/chunk?index=1&byteOffset=3&hash=4ed9407630eb1000c0f6b63842defa7d&fmt=json",
          transport.requests.get(3).uri().toString());
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
    transport.enqueueResponse(200, "OK".getBytes(StandardCharsets.UTF_8));
    transport.enqueueResponse(200, new byte[0]);
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
                      .threadCount(1)
                      .build());

      assertEquals("item-2", result.getItemId());
      assertEquals(4, transport.requests.size());
      assertEquals(
          "https://storage.example.com/chunk?index=0&byteOffset=0&hash=ab56b4d92b40713acc5af89985d4b786&fmt=json",
          transport.requests.get(1).uri().toString());
      assertEquals(
          "https://storage.example.com/chunk?index=1&byteOffset=5&hash=57c48dcd266eadf089325affe125151f&fmt=json",
          transport.requests.get(2).uri().toString());
      assertEquals(
          "https://storage.example.com/finish?fmt=json",
          transport.requests.get(3).uri().toString());
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
    transport.enqueueResponse(200, "OK".getBytes(StandardCharsets.UTF_8));
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
      assertEquals(
          "https://storage.example.com/chunk?index=1&byteOffset=5&hash=57c48dcd266eadf089325affe125151f&fmt=json",
          transport.requests.get(1).uri().toString());
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
          "Method": "Threaded",
          "ChunkUri": "https://storage.example.com/upload",
          "FinishUri": "https://storage.example.com/finish"
        }
        """);
    transport.enqueueResponse(200, "OK".getBytes(StandardCharsets.UTF_8));
    transport.enqueueJsonResponse(
        200,
        """
        {
          "value": [
            {
              "id": "item-4",
              "filename": "async.txt",
              "size": 11
            }
          ],
          "error": false
        }
        """);

    Path file =
        Files.writeString(tempDir.resolve("async.txt"), "hello world", StandardCharsets.UTF_8);
    List<Long> progressEvents = new CopyOnWriteArrayList<>();
    List<String> callbackEvents = new CopyOnWriteArrayList<>();

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      UploadHandle handle =
          context
              .transferClient()
              .uploadAsync(
                  "folder-1",
                  file,
                  UploadOptions.builder()
                      .method(UploadMethod.THREADED)
                      .chunkSizeBytes(11)
                      .threadCount(1)
                      .progressListener((sent, total) -> progressEvents.add(sent))
                      .callback(
                          new UploadCallback() {
                            @Override
                            public void onStarted(TransferProgress progress) {
                              callbackEvents.add("started:" + progress.getState());
                            }

                            @Override
                            public void onProgress(TransferProgress progress) {
                              callbackEvents.add("progress:" + progress.getBytesTransferred());
                            }

                            @Override
                            public void onCompleted(UploadResult result) {
                              callbackEvents.add("completed:" + result.getItemId());
                            }
                          })
                      .build());

      UploadResult result = handle.awaitOrThrow(java.time.Duration.ofSeconds(5));

      assertEquals("item-4", result.getItemId());
      assertFalse(progressEvents.isEmpty());
      assertTrue(callbackEvents.contains("started:" + TransferState.IN_PROGRESS));
      assertTrue(callbackEvents.contains("progress:11"));
      assertTrue(callbackEvents.contains("completed:item-4"));
      assertEquals(TransferState.COMPLETED, handle.progress().getState());
    }
  }

  @Test
  void uploadAsyncStreamCompletesAndReturnsFinishItemId() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "Method": "Threaded",
          "ChunkUri": "https://storage.example.com/upload",
          "FinishUri": "https://storage.example.com/finish"
        }
        """);
    transport.enqueueResponse(200, "OK".getBytes(StandardCharsets.UTF_8));
    transport.enqueueJsonResponse(
        200,
        """
        {
          "value": [
            {
              "id": "stream-item-1",
              "filename": "stream.txt",
              "size": 11
            }
          ],
          "error": false
        }
        """);
    List<String> callbackEvents = new CopyOnWriteArrayList<>();

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      UploadHandle handle =
          context
              .transferClient()
              .uploadAsync(
                  "folder-1",
                  new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)),
                  "stream.txt",
                  11L,
                  UploadOptions.builder()
                      .chunkSizeBytes(11)
                      .callback(
                          new UploadCallback() {
                            @Override
                            public void onCompleted(UploadResult result) {
                              callbackEvents.add(result.getItemId());
                            }
                          })
                      .build());

      UploadResult result = handle.awaitOrThrow(Duration.ofSeconds(5));

      assertEquals("stream-item-1", result.getItemId());
      assertTrue(transport.requests.get(0).body().contains("\"Method\":\"Threaded\""));
      assertTrue(transport.requests.get(0).body().contains("\"Raw\":true"));
      assertTrue(transport.requests.get(1).uri().toString().contains("index=0"));
      assertTrue(transport.requests.get(1).uri().toString().contains("byteOffset=0"));
      assertTrue(transport.requests.get(1).uri().toString().contains("hash="));
      assertEquals(
          "https://storage.example.com/finish?fmt=json",
          transport.requests.get(2).uri().toString());
      assertTrue(callbackEvents.contains("stream-item-1"));
    }
  }

  @Test
  void uploadAsyncFailureInvokesCallback() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "Method": "Threaded",
          "ChunkUri": "https://storage.example.com/upload",
          "FinishUri": "https://storage.example.com/finish"
        }
        """);
    transport.enqueueResponse(200, "ERROR:System error occurred".getBytes(StandardCharsets.UTF_8));

    Path file = Files.writeString(tempDir.resolve("failed.txt"), "hello", StandardCharsets.UTF_8);
    List<String> callbackEvents = new CopyOnWriteArrayList<>();

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      UploadHandle handle =
          context
              .transferClient()
              .uploadAsync(
                  "folder-1",
                  file,
                  UploadOptions.builder()
                      .method(UploadMethod.THREADED)
                      .chunkSizeBytes(5)
                      .threadCount(1)
                      .callback(
                          new UploadCallback() {
                            @Override
                            public void onFailed(Throwable error) {
                              callbackEvents.add(error.getClass().getSimpleName());
                            }
                          })
                      .build());

      assertThrows(RuntimeException.class, () -> handle.awaitOrThrow(Duration.ofSeconds(5)));
      assertTrue(callbackEvents.contains("ShareFileChunkUploadException"));
    }
  }

  @Test
  void uploadAsyncCancellationInvokesCallback() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    RetryConfig retryConfig = RetryConfig.builder().maxRetries(0).build();
    QueuedExecutor executor = new QueuedExecutor();
    List<String> callbackEvents = new CopyOnWriteArrayList<>();

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      TransferClient transferClient = newTransferClient(context, transport, retryConfig, executor);
      Path file = Files.writeString(tempDir.resolve("cancel.txt"), "data", StandardCharsets.UTF_8);

      UploadHandle handle =
          transferClient.uploadAsync(
              "folder-1",
              file,
              UploadOptions.builder()
                  .callback(
                      new UploadCallback() {
                        @Override
                        public void onCancelled() {
                          callbackEvents.add("cancelled");
                        }
                      })
                  .build());

      assertTrue(handle.cancel());
      assertTrue(callbackEvents.contains("cancelled"));
      assertEquals(1, executor.submissions);
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
      TransferClient transferClient = newTransferClient(context, transport, retryConfig, executor);

      UploadHandle handle = transferClient.uploadAsync("folder-1", file, UploadOptions.defaults());
      UploadResult result = handle.awaitOrThrow(Duration.ofSeconds(5));

      assertEquals("item-5", result.getItemId());
      assertTrue(executor.submissions > 0);
    }
  }

  @Test
  void threadedUploadRetryDoesNotOvercountTransferredBytes() throws Exception {
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
    transport.enqueueResponse(500, "temporary".getBytes(StandardCharsets.UTF_8));
    transport.enqueueJsonResponse(200, "{\"ChunkNumber\":0,\"IsComplete\":true}");
    transport.enqueueJsonResponse(
        200,
        """
        {
          "ItemId": "item-6",
          "FileName": "retry.bin",
          "FileSize": 5
        }
        """);

    Path file = Files.write(tempDir.resolve("retry.bin"), "abcde".getBytes(StandardCharsets.UTF_8));

    try (ClientTestSupport.TestContext context =
        ClientTestSupport.createContext(
            transport, RetryConfig.builder().maxRetries(1).initialBackoff(Duration.ZERO).build())) {
      UploadHandle handle =
          context
              .transferClient()
              .uploadAsync(
                  "folder-1",
                  file,
                  UploadOptions.builder()
                      .method(UploadMethod.THREADED)
                      .chunkSizeBytes(5)
                      .threadCount(1)
                      .build());

      UploadResult result = handle.awaitOrThrow(Duration.ofSeconds(5));

      assertEquals("item-6", result.getItemId());
      assertEquals(5L, handle.progress().getBytesTransferred());
      assertEquals(TransferState.COMPLETED, handle.progress().getState());
    }
  }

  @Test
  void cancelledDownloadSkipsNetworkAndTargetWritesBeforeStart() throws Exception {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    Path target =
        Files.writeString(tempDir.resolve("cancelled.bin"), "keep-me", StandardCharsets.UTF_8);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      TransferClient transferClient = context.transferClient();
      Object tracker = newProgressTracker(0L);
      AtomicBoolean cancelled = new AtomicBoolean(true);

      assertThrows(
          io.github.indraftapp.sharefile.core.exception.ShareFileTransferCancelledException.class,
          () ->
              invokeDownloadInternal(
                  transferClient,
                  "item-1",
                  target,
                  DownloadOptions.defaults(),
                  tracker,
                  cancelled));

      assertEquals("keep-me", Files.readString(target, StandardCharsets.UTF_8));
      assertEquals(0, transport.requests.size());
    }
  }

  @Test
  void progressTrackerAddBytesIsAtomic() throws Exception {
    Object tracker = newProgressTracker(1000L);
    invokeTrackerMethod(tracker, "start");

    int threadCount = 8;
    int incrementsPerThread = 250;
    java.util.concurrent.ExecutorService executor =
        java.util.concurrent.Executors.newFixedThreadPool(threadCount);
    try {
      List<? extends java.util.concurrent.Future<?>> futures =
          java.util.stream.IntStream.range(0, threadCount)
              .mapToObj(
                  i ->
                      executor.submit(
                          () -> {
                            try {
                              for (int j = 0; j < incrementsPerThread; j++) {
                                invokeTrackerMethod(tracker, "addBytes", 1L);
                              }
                            } catch (Exception e) {
                              throw new RuntimeException(e);
                            }
                          }))
              .toList();
      for (java.util.concurrent.Future<?> future : futures) {
        future.get(5, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
    }

    TransferProgress progress = snapshot(tracker);
    assertEquals(threadCount * incrementsPerThread, progress.getBytesTransferred());
    assertEquals(TransferState.IN_PROGRESS, progress.getState());
  }

  private static Object newProgressTracker(long totalBytes) throws Exception {
    Class<?> trackerClass =
        Class.forName("io.github.indraftapp.sharefile.client.TransferClient$ProgressTracker");
    var constructor =
        trackerClass.getDeclaredConstructor(long.class, TransferProgressListener.class);
    constructor.setAccessible(true);
    return constructor.newInstance(totalBytes, null);
  }

  private static TransferClient newTransferClient(
      ClientTestSupport.TestContext context,
      ClientTestSupport.TestTransport transport,
      RetryConfig retryConfig,
      java.util.concurrent.ExecutorService executor) {
    ShareFileHttpClient httpClient =
        new ShareFileHttpClient(
            transport,
            context.tokenManager(),
            ClientTestSupport.MAPPER,
            retryConfig,
            MetricsProvider.noop(),
            ClientTestSupport.BASE_URL,
            Duration.ofSeconds(30));
    return new TransferClient(
        httpClient,
        transport,
        ClientTestSupport.MAPPER,
        io.github.indraftapp.sharefile.client.config.ShareFileConfig.builder()
            .subdomain("testco")
            .build(),
        retryConfig,
        MetricsProvider.noop(),
        executor);
  }

  private static void invokeDownloadInternal(
      TransferClient transferClient,
      String itemId,
      Path target,
      DownloadOptions options,
      Object tracker,
      AtomicBoolean cancelled)
      throws Exception {
    var method =
        TransferClient.class.getDeclaredMethod(
            "downloadInternal",
            String.class,
            Path.class,
            DownloadOptions.class,
            tracker.getClass(),
            AtomicBoolean.class);
    method.setAccessible(true);
    try {
      method.invoke(transferClient, itemId, target, options, tracker, cancelled);
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw e;
    }
  }

  private static void invokeTrackerMethod(Object tracker, String methodName, Object... args)
      throws Exception {
    Class<?>[] parameterTypes =
        java.util.Arrays.stream(args)
            .map(Object::getClass)
            .map(TransferClientTest::unbox)
            .toArray(Class<?>[]::new);
    var method = tracker.getClass().getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    method.invoke(tracker, args);
  }

  private static TransferProgress snapshot(Object tracker) throws Exception {
    var method = tracker.getClass().getDeclaredMethod("snapshot");
    method.setAccessible(true);
    return (TransferProgress) method.invoke(tracker);
  }

  private static Class<?> unbox(Class<?> type) {
    if (type == Long.class) {
      return long.class;
    }
    return type;
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

  private static final class QueuedExecutor extends AbstractExecutorService {
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
    }
  }
}
