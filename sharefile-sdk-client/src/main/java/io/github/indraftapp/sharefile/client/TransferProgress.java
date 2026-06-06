package io.github.indraftapp.sharefile.client;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/** Snapshot of transfer progress for an upload or download operation. */
public final class TransferProgress {

  private final long bytesTransferred;
  private final long totalBytes;
  private final Duration elapsed;
  private final TransferState state;
  private final int chunksCompleted;
  private final int totalChunks;

  public TransferProgress(
      long bytesTransferred,
      long totalBytes,
      Duration elapsed,
      TransferState state,
      int chunksCompleted,
      int totalChunks) {
    this.bytesTransferred = Math.max(0L, bytesTransferred);
    this.totalBytes = Math.max(0L, totalBytes);
    this.elapsed = Objects.requireNonNull(elapsed, "elapsed must not be null");
    this.state = Objects.requireNonNull(state, "state must not be null");
    this.chunksCompleted = Math.max(0, chunksCompleted);
    this.totalChunks = Math.max(0, totalChunks);
  }

  public long getBytesTransferred() {
    return bytesTransferred;
  }

  public long getTotalBytes() {
    return totalBytes;
  }

  public double getPercentComplete() {
    if (totalBytes <= 0L) {
      return 0.0d;
    }
    return Math.min(100.0d, (bytesTransferred * 100.0d) / totalBytes);
  }

  public Duration getElapsed() {
    return elapsed;
  }

  public OptionalDouble getBytesPerSecond() {
    if (elapsed.isZero() || elapsed.isNegative()) {
      return OptionalDouble.empty();
    }
    double seconds = elapsed.toNanos() / 1_000_000_000.0d;
    if (seconds <= 0.0d) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(bytesTransferred / seconds);
  }

  public Optional<Duration> getEstimatedTimeRemaining() {
    if (bytesTransferred <= 0L || totalBytes <= bytesTransferred) {
      return Optional.empty();
    }
    OptionalDouble rate = getBytesPerSecond();
    if (rate.isEmpty() || rate.getAsDouble() <= 0.0d) {
      return Optional.empty();
    }
    long remainingBytes = totalBytes - bytesTransferred;
    long nanos = (long) ((remainingBytes / rate.getAsDouble()) * 1_000_000_000L);
    return Optional.of(Duration.ofNanos(Math.max(0L, nanos)));
  }

  public TransferState getState() {
    return state;
  }

  public int getChunksCompleted() {
    return chunksCompleted;
  }

  public int getTotalChunks() {
    return totalChunks;
  }
}
