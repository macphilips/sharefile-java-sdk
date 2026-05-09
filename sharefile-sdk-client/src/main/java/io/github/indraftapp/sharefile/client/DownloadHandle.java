package io.github.indraftapp.sharefile.client;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Async handle for a download transfer. */
public final class DownloadHandle implements TransferHandle<Path> {

  private final CompletableFuture<Path> future;
  private final AtomicReference<TransferProgress> progress;
  private final AtomicBoolean cancelled;

  DownloadHandle(
      CompletableFuture<Path> future,
      AtomicReference<TransferProgress> progress,
      AtomicBoolean cancelled) {
    this.future = Objects.requireNonNull(future, "future must not be null");
    this.progress = Objects.requireNonNull(progress, "progress must not be null");
    this.cancelled = Objects.requireNonNull(cancelled, "cancelled must not be null");
  }

  @Override
  public CompletableFuture<Path> future() {
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
