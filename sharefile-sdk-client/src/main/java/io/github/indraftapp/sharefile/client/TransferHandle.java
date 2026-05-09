package io.github.indraftapp.sharefile.client;

import io.github.indraftapp.sharefile.core.exception.ShareFileTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Async handle returned for long-running transfers. */
public interface TransferHandle<T> {

  CompletableFuture<T> future();

  TransferProgress progress();

  boolean cancel();

  default T awaitOrThrow(Duration timeout) {
    try {
      return future().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ShareFileTimeoutException("Interrupted while waiting for transfer completion", e);
    } catch (TimeoutException e) {
      throw new ShareFileTimeoutException("Timed out waiting for transfer completion", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException(cause);
    }
  }
}
