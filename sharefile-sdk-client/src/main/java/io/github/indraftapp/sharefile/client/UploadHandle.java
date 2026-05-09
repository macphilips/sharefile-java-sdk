package io.github.indraftapp.sharefile.client;

import io.github.indraftapp.sharefile.core.model.response.UploadResult;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Async handle for an upload transfer. */
public final class UploadHandle implements TransferHandle<UploadResult> {

  private final CompletableFuture<UploadResult> future;
  private final AtomicReference<TransferProgress> progress;
  private final AtomicBoolean cancelled;

  UploadHandle(
      CompletableFuture<UploadResult> future,
      AtomicReference<TransferProgress> progress,
      AtomicBoolean cancelled) {
    this.future = Objects.requireNonNull(future, "future must not be null");
    this.progress = Objects.requireNonNull(progress, "progress must not be null");
    this.cancelled = Objects.requireNonNull(cancelled, "cancelled must not be null");
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
    return future.cancel(true);
  }
}
