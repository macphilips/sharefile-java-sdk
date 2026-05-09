package io.github.indraftapp.sharefile.client.retry;

/**
 * Per-request retry policy override.
 *
 * <p>By default, POST, PATCH, and DELETE are not retried on server errors. Callers can opt in:
 *
 * <pre>{@code
 * client.items().createFolder(parentId, request, RetryPolicy.retryOnServerError(2));
 * }</pre>
 */
public final class RetryPolicy {

  /** Sentinel policy meaning "use the engine's default behavior for this HTTP method." */
  public static final RetryPolicy DEFAULT = new RetryPolicy(false, -1);

  private final boolean retryNonIdempotent;
  private final int maxRetries;

  private RetryPolicy(boolean retryNonIdempotent, int maxRetries) {
    this.retryNonIdempotent = retryNonIdempotent;
    this.maxRetries = maxRetries;
  }

  /**
   * Creates a policy that allows retrying non-idempotent requests on server errors.
   *
   * @param maxRetries the maximum number of retries (overrides config for this request)
   * @return a retry policy that opts in to retries
   */
  public static RetryPolicy retryOnServerError(int maxRetries) {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries must be >= 0");
    }
    return new RetryPolicy(true, maxRetries);
  }

  /** Returns {@code true} if non-idempotent requests should be retried. */
  public boolean isRetryNonIdempotent() {
    return retryNonIdempotent;
  }

  /** Returns the max retries override, or -1 if the engine's default should be used. */
  public int getMaxRetries() {
    return maxRetries;
  }
}
