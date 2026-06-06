package io.github.indraftapp.sharefile.client;

import io.github.indraftapp.sharefile.core.model.response.UploadResult;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Async handle for an upload transfer. */
public final class UploadHandle implements TransferHandle<UploadResult> {

  private final CompletableFuture<UploadResult> future;
  private final CompletableFuture<UploadResult> cancellationFuture;
  private final AtomicReference<TransferProgress> progress;
  private final AtomicBoolean cancelled;
  private final Runnable cancellationCallback;

  UploadHandle(
      CompletableFuture<UploadResult> future,
      CompletableFuture<UploadResult> cancellationFuture,
      AtomicReference<TransferProgress> progress,
      AtomicBoolean cancelled,
      Runnable cancellationCallback) {
    this.future = Objects.requireNonNull(future, "future must not be null");
    this.cancellationFuture =
        Objects.requireNonNull(cancellationFuture, "cancellationFuture must not be null");
    this.progress = Objects.requireNonNull(progress, "progress must not be null");
    this.cancelled = Objects.requireNonNull(cancelled, "cancelled must not be null");
    this.cancellationCallback = cancellationCallback;
  }

  @Override
  public CompletableFuture<UploadResult> future() {
    return future;
  }

  @Override
  public TransferProgress progress() {
    return progress.get();
  }

  @Override
  public boolean cancel() {
    cancelled.set(true);
    boolean cancelledFuture = future.cancel(true);
    boolean cancelledRawFuture = cancellationFuture.cancel(true);
    boolean cancelledAny = cancelledFuture || cancelledRawFuture;
    if (cancelledAny && cancellationCallback != null) {
      cancellationCallback.run();
    }
    return cancelledAny;
  }
}
