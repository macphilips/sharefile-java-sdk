package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.core.exception.ShareFileTimeoutException;
import io.github.indraftapp.sharefile.core.model.AsyncOperation;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Explicit ShareFile resource client for `/AsyncOperations` endpoints. */
public final class AsyncOperationsClient {

  private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);
  private static final TypeReference<ODataFeed<AsyncOperation>> ASYNC_OPERATION_FEED_TYPE =
      new TypeReference<>() {};

  private final ResourceRequestExecutor executor;

  AsyncOperationsClient(ResourceRequestExecutor executor) {
    this.executor = Objects.requireNonNull(executor, "executor must not be null");
  }

  AsyncOperationsClient(ShareFileHttpClient httpClient) {
    this(new ResourceRequestExecutor(httpClient, "/AsyncOperations"));
  }

  /**
   * Retrieves a single async operation by identifier.
   *
   * <pre>{@code
   * AsyncOperation operation = client.asyncOperations().getById("op-123");
   * }</pre>
   *
   * @param operationId async operation identifier
   * @return resolved async operation
   */
  public AsyncOperation getById(String operationId) {
    validateOperationId(operationId);
    return executor.get(executor.entityUri(operationId), ODataQuery.empty(), AsyncOperation.class);
  }

  /**
   * Lists async operations with no additional query options.
   *
   * <pre>{@code
   * ODataFeed<AsyncOperation> feed = client.asyncOperations().list();
   * }</pre>
   *
   * @return feed of async operations
   */
  public ODataFeed<AsyncOperation> list() {
    return list(ODataQuery.empty());
  }

  /**
   * Lists async operations using OData query options.
   *
   * <pre>{@code
   * ODataFeed<AsyncOperation> feed =
   *     client.asyncOperations().list(ODataQuery.builder().top(25).build());
   * }</pre>
   *
   * @param query OData query options
   * @return feed of async operations
   */
  public ODataFeed<AsyncOperation> list(ODataQuery query) {
    return executor.getCollection(executor.collectionUri(), query, ASYNC_OPERATION_FEED_TYPE);
  }

  /**
   * Polls an async operation until it reaches a terminal state or the timeout expires.
   *
   * <pre>{@code
   * AsyncOperation completed =
   *     client.asyncOperations().awaitCompletion("op-123", Duration.ofMinutes(2));
   * }</pre>
   *
   * @param operationId async operation identifier
   * @param timeout maximum time to wait
   * @return terminal async operation state
   * @throws ShareFileTimeoutException if the timeout expires before completion or polling is
   *     interrupted
   */
  public AsyncOperation awaitCompletion(String operationId, Duration timeout) {
    validateOperationId(operationId);
    validateTimeout(timeout);

    Instant deadline = Instant.now().plus(timeout);
    while (true) {
      AsyncOperation operation = getById(operationId);
      if (operation.isTerminal()) {
        return operation;
      }

      Duration remaining = Duration.between(Instant.now(), deadline);
      if (remaining.isZero() || remaining.isNegative()) {
        break;
      }

      try {
        Thread.sleep(Math.min(POLL_INTERVAL.toMillis(), remaining.toMillis()));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ShareFileTimeoutException(
            "Interrupted while waiting for AsyncOperation %s to complete".formatted(operationId),
            e);
      }
    }

    throw new ShareFileTimeoutException(
        "AsyncOperation %s did not complete within %s".formatted(operationId, timeout));
  }

  private static void validateOperationId(String operationId) {
    if (operationId == null || operationId.isBlank()) {
      throw new IllegalArgumentException("operationId must not be blank");
    }
  }

  private static void validateTimeout(Duration timeout) {
    Objects.requireNonNull(timeout, "timeout must not be null");
    if (timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException("timeout must be positive");
    }
  }
}
