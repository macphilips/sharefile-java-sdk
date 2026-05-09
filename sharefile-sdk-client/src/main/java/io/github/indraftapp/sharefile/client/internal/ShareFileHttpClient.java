package io.github.indraftapp.sharefile.client.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.client.MetricNames;
import io.github.indraftapp.sharefile.client.auth.TokenManager;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.client.spi.MetricsProvider;
import io.github.indraftapp.sharefile.core.exception.ShareFileApiException;
import io.github.indraftapp.sharefile.core.exception.ShareFileSerializationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal HTTP client that wraps {@link HttpTransport} and adds SDK concerns.
 *
 * <p>This class is package-private and not part of the public API. It handles:
 *
 * <ul>
 *   <li>Bearer token injection from {@link TokenManager}
 *   <li>X-Request-Id generation
 *   <li>JSON serialization/deserialization via Jackson
 *   <li>Error response parsing to typed exceptions
 *   <li>Retry delegation via {@link RetryEngine}
 *   <li>Metrics recording via {@link MetricsProvider}
 * </ul>
 */
@Slf4j
public final class ShareFileHttpClient {
  private static final int MAX_JSON_RESPONSE_BYTES = 10 * 1024 * 1024; // 10 MB

  private final HttpTransport transport;
  private final TokenManager tokenManager;
  private final ObjectMapper objectMapper;
  private final RetryEngine retryEngine;
  private final MetricsProvider metrics;
  private final String baseUrl;
  private final Duration readTimeout;
  private final AtomicInteger activeRequests = new AtomicInteger();

  /**
   * Creates a new ShareFileHttpClient.
   *
   * @param transport the HTTP transport
   * @param tokenManager the token manager for Bearer token injection
   * @param objectMapper the Jackson ObjectMapper for JSON handling
   * @param retryConfig retry configuration
   * @param metrics metrics provider
   * @param baseUrl the base API URL (e.g., {@code https://myco.sf-api.com/sf/v3})
   * @param readTimeout default read timeout for API requests
   */
  public ShareFileHttpClient(
      HttpTransport transport,
      TokenManager tokenManager,
      ObjectMapper objectMapper,
      RetryConfig retryConfig,
      MetricsProvider metrics,
      String baseUrl,
      Duration readTimeout) {
    this.transport = Objects.requireNonNull(transport);
    this.tokenManager = Objects.requireNonNull(tokenManager);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.retryEngine = new RetryEngine(retryConfig, metrics);
    this.metrics = Objects.requireNonNull(metrics);
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.readTimeout = Objects.requireNonNull(readTimeout);
  }

  /**
   * Sends a GET request and deserializes the response.
   *
   * @param path the API path (appended to baseUrl)
   * @param params OData query parameters (may be empty)
   * @param responseType the expected response type
   * @param <T> the response type
   * @return the deserialized response
   */
  public <T> T get(String path, Map<String, String> params, Class<T> responseType) {
    return execute("GET", buildUri(path, params), null, responseType, RetryPolicy.DEFAULT);
  }

  /**
   * Sends a GET request and deserializes the response using a TypeReference.
   *
   * @param path the API path
   * @param params OData query parameters
   * @param responseType the expected response type
   * @param <T> the response type
   * @return the deserialized response
   */
  public <T> T get(String path, Map<String, String> params, TypeReference<T> responseType) {
    return executeWithTypeRef(
        "GET", buildUri(path, params), null, responseType, RetryPolicy.DEFAULT);
  }

  /**
   * Sends a POST request with a JSON body.
   *
   * @param path the API path
   * @param body the request body (serialized to JSON)
   * @param responseType the expected response type
   * @param <T> the response type
   * @return the deserialized response
   */
  public <T> T post(String path, Object body, Class<T> responseType) {
    return execute("POST", buildUri(path, Map.of()), body, responseType, RetryPolicy.DEFAULT);
  }

  /**
   * Sends a POST request with optional retry policy override.
   *
   * @param path the API path
   * @param body the request body
   * @param responseType the expected response type
   * @param policy retry policy override
   * @param <T> the response type
   * @return the deserialized response
   */
  public <T> T post(String path, Object body, Class<T> responseType, RetryPolicy policy) {
    return execute("POST", buildUri(path, Map.of()), body, responseType, policy);
  }

  /**
   * Sends a PATCH request with a JSON body.
   *
   * @param path the API path
   * @param body the request body
   * @param responseType the expected response type
   * @param <T> the response type
   * @return the deserialized response
   */
  public <T> T patch(String path, Object body, Class<T> responseType) {
    return execute("PATCH", buildUri(path, Map.of()), body, responseType, RetryPolicy.DEFAULT);
  }

  /**
   * Sends a DELETE request.
   *
   * @param path the API path
   * @param params query parameters
   */
  public void delete(String path, Map<String, String> params) {
    execute("DELETE", buildUri(path, params), null, Void.class, RetryPolicy.DEFAULT);
  }

  public <T> T get(URI uri, Class<T> responseType) {
    return execute("GET", uri, null, responseType, RetryPolicy.DEFAULT);
  }

  public <T> T get(URI uri, TypeReference<T> responseType) {
    return executeWithTypeRef("GET", uri, null, responseType, RetryPolicy.DEFAULT);
  }

  public <T> T post(URI uri, Object body, Class<T> responseType) {
    return execute("POST", uri, body, responseType, RetryPolicy.DEFAULT);
  }

  public <T> T post(URI uri, Object body, Class<T> responseType, RetryPolicy policy) {
    return execute("POST", uri, body, responseType, policy);
  }

  public <T> T post(URI uri, Object body, TypeReference<T> responseType, RetryPolicy policy) {
    return executeWithTypeRef("POST", uri, body, responseType, policy);
  }

  public <T> T patch(URI uri, Object body, Class<T> responseType) {
    return execute("PATCH", uri, body, responseType, RetryPolicy.DEFAULT);
  }

  public <T> T patch(URI uri, Object body, Class<T> responseType, RetryPolicy policy) {
    return execute("PATCH", uri, body, responseType, policy);
  }

  public JsonNode postForJsonNode(URI uri, Object body, RetryPolicy policy) {
    return executeForJsonNode("POST", uri, body, policy);
  }

  public JsonNode patchForJsonNode(URI uri, Object body, RetryPolicy policy) {
    return executeForJsonNode("PATCH", uri, body, policy);
  }

  public void delete(URI uri) {
    execute("DELETE", uri, null, Void.class, RetryPolicy.DEFAULT);
  }

  public <T> T convertValue(JsonNode node, Class<T> targetType) {
    try {
      return objectMapper.treeToValue(node, targetType);
    } catch (JsonProcessingException e) {
      throw new ShareFileSerializationException("Failed to convert response body", e);
    }
  }

  /**
   * Core execution method. Orchestrates the full request pipeline.
   *
   * @param method HTTP method
   * @param path API path
   * @param body request body (null for bodyless requests)
   * @param params query parameters
   * @param responseType expected response type
   * @param policy retry policy override
   * @param <T> the response type
   * @return the deserialized response
   */
  public <T> T execute(
      String method,
      String path,
      Object body,
      Map<String, String> params,
      Class<T> responseType,
      RetryPolicy policy) {
    return execute(method, buildUri(path, params), body, responseType, policy);
  }

  private <T> T executeWithTypeRef(
      String method, URI uri, Object body, TypeReference<T> responseType, RetryPolicy policy) {
    String requestId = UUID.randomUUID().toString();
    byte[] jsonBody = serializeBody(body);

    MetricsProvider.Timer timer = metrics.startTimer();
    String entity = extractEntityName(uri.getPath());
    incrementActiveRequests(entity, method);

    try {
      HttpTransport.HttpResponse response =
          executeWithRetry(method, resolveUri(uri), jsonBody, requestId, policy);

      try (response) {
        int status = response.statusCode();
        metrics.recordRequest(timer, entity, method, status);

        if (status == 204) {
          return null;
        }

        byte[] responseBody = response.bodyBytes(MAX_JSON_RESPONSE_BYTES);
        return deserializeWithTypeRef(responseBody, responseType);
      }
    } catch (ShareFileApiException e) {
      timer.stop();
      recordErrorMetric(entity, method, e.getHttpStatus());
      throw e;
    } finally {
      decrementActiveRequests(entity, method);
    }
  }

  private <T> T execute(
      String method, URI uri, Object body, Class<T> responseType, RetryPolicy policy) {
    String requestId = UUID.randomUUID().toString();
    byte[] jsonBody = serializeBody(body);

    MetricsProvider.Timer timer = metrics.startTimer();
    String entity = extractEntityName(uri.getPath());
    incrementActiveRequests(entity, method);

    try {
      HttpTransport.HttpResponse response =
          executeWithRetry(method, resolveUri(uri), jsonBody, requestId, policy);

      try (response) {
        int status = response.statusCode();
        metrics.recordRequest(timer, entity, method, status);

        if (status == 204 || responseType == Void.class) {
          return null;
        }

        byte[] responseBody = response.bodyBytes(MAX_JSON_RESPONSE_BYTES);
        return deserialize(responseBody, responseType);
      }
    } catch (ShareFileApiException e) {
      timer.stop();
      recordErrorMetric(entity, method, e.getHttpStatus());
      throw e;
    } finally {
      decrementActiveRequests(entity, method);
    }
  }

  private JsonNode executeForJsonNode(String method, URI uri, Object body, RetryPolicy policy) {
    String requestId = UUID.randomUUID().toString();
    byte[] jsonBody = serializeBody(body);

    MetricsProvider.Timer timer = metrics.startTimer();
    String entity = extractEntityName(uri.getPath());
    incrementActiveRequests(entity, method);

    try {
      HttpTransport.HttpResponse response =
          executeWithRetry(method, resolveUri(uri), jsonBody, requestId, policy);

      try (response) {
        int status = response.statusCode();
        metrics.recordRequest(timer, entity, method, status);

        if (status == 204) {
          return null;
        }

        byte[] responseBody = response.bodyBytes(MAX_JSON_RESPONSE_BYTES);
        return deserializeTree(responseBody);
      }
    } catch (ShareFileApiException e) {
      timer.stop();
      recordErrorMetric(entity, method, e.getHttpStatus());
      throw e;
    } finally {
      decrementActiveRequests(entity, method);
    }
  }

  // ── Request execution with retry ──────────────────────────────────────

  private HttpTransport.HttpResponse executeWithRetry(
      String method, URI uri, byte[] jsonBody, String requestId, RetryPolicy policy) {
    return retryEngine.execute(
        () -> executeSingle(method, uri, jsonBody, requestId, true), method, policy);
  }

  /**
   * Executes a single request attempt. If a 401 is received, refreshes the token and retries once.
   */
  private HttpTransport.HttpResponse executeSingle(
      String method, URI uri, byte[] jsonBody, String requestId, boolean allowTokenRefresh) {
    String token = tokenManager.getAccessToken();
    HttpTransport.HttpRequest request = buildRequest(method, uri, jsonBody, requestId, token);
    logRequest(method, uri, request.headers(), jsonBody);

    HttpTransport.HttpResponse response = transport.execute(request);

    int status = response.statusCode();
    if (status >= 400) {
      byte[] errorBody;
      try {
        errorBody = response.bodyBytes(MAX_JSON_RESPONSE_BYTES);
      } catch (Exception e) {
        errorBody = new byte[0];
      } finally {
        response.close();
      }

      // 401: try token refresh once
      if (status == 401 && allowTokenRefresh) {
        log.debug("Received 401, refreshing token and retrying");
        tokenManager.refreshAccessToken();
        return executeSingle(method, uri, jsonBody, requestId, false);
      }

      ErrorBody parsed = parseErrorBody(errorBody);
      long retryAfter = parseRetryAfter(response, status);

      // For retryable statuses, throw a RetryableResponseException
      if (isRetryableStatus(status)) {
        throw new RetryEngine.RetryableResponseException(
            status, retryAfter, parsed.code, parsed.message, requestId, method, uri.toString());
      }

      // Non-retryable: throw the final exception
      throw ErrorResponseMapper.create(
          status, parsed.code, parsed.message, requestId, method, uri.toString(), retryAfter);
    }

    logResponse(method, uri, status, response.headers());

    return response;
  }

  private boolean isRetryableStatus(int status) {
    return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
  }

  // ── Request building ──────────────────────────────────────────────────

  private HttpTransport.HttpRequest buildRequest(
      String method, URI uri, byte[] jsonBody, String requestId, String bearerToken) {
    var headers = new LinkedHashMap<String, String>();
    headers.put("Authorization", "Bearer " + bearerToken);
    headers.put("X-Request-Id", requestId);
    headers.put("Accept", "application/json");
    if (jsonBody != null) {
      headers.put("Content-Type", "application/json");
    }
    Map<String, String> unmodifiableHeaders = Collections.unmodifiableMap(headers);

    boolean hasBody = jsonBody != null;
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
        return unmodifiableHeaders;
      }

      @Override
      public Optional<InputStream> bodyStream() {
        return hasBody ? Optional.of(new ByteArrayInputStream(jsonBody)) : Optional.empty();
      }

      @Override
      public OptionalLong contentLength() {
        return hasBody ? OptionalLong.of(jsonBody.length) : OptionalLong.empty();
      }

      @Override
      public Duration timeout() {
        return readTimeout;
      }
    };
  }

  // ── URI building ──────────────────────────────────────────────────────

  private URI buildUri(String path, Map<String, String> params) {
    StringBuilder sb = new StringBuilder(baseUrl);
    if (!path.startsWith("/")) {
      sb.append('/');
    }
    sb.append(path);

    if (params != null && !params.isEmpty()) {
      sb.append('?');
      boolean first = true;
      for (Map.Entry<String, String> entry : params.entrySet()) {
        if (!first) {
          sb.append('&');
        }
        sb.append(encodeUri(entry.getKey()));
        sb.append('=');
        sb.append(encodeUri(entry.getValue()));
        first = false;
      }
    }
    return URI.create(sb.toString());
  }

  private URI resolveUri(URI uri) {
    if (uri.isAbsolute()) {
      return uri;
    }
    String path = uri.toString();
    StringBuilder sb = new StringBuilder(baseUrl);
    if (!path.startsWith("/")) {
      sb.append('/');
    }
    sb.append(path);
    return URI.create(sb.toString());
  }

  private static String encodeUri(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private void incrementActiveRequests(String entity, String method) {
    metrics.setGauge(
        MetricNames.HTTP_REQUESTS_ACTIVE,
        activeRequests.incrementAndGet(),
        "entity",
        entity,
        "method",
        method);
  }

  private void decrementActiveRequests(String entity, String method) {
    metrics.setGauge(
        MetricNames.HTTP_REQUESTS_ACTIVE,
        Math.max(0, activeRequests.decrementAndGet()),
        "entity",
        entity,
        "method",
        method);
  }

  private void recordErrorMetric(String entity, String method, int status) {
    metrics.recordError(entity, method, status);
    metrics.incrementCounter(
        MetricNames.HTTP_ERRORS,
        "entity",
        entity,
        "method",
        method,
        "status",
        String.valueOf(status));
  }

  private void logRequest(String method, URI uri, Map<String, String> headers, byte[] jsonBody) {
    if (log.isDebugEnabled()) {
      log.debug("HTTP request {} {}", method, uri);
    }
    if (log.isTraceEnabled()) {
      String body =
          jsonBody == null
              ? ""
              : LogSanitizer.redactBody(new String(jsonBody, StandardCharsets.UTF_8));
      log.trace(
          "HTTP request trace method={} uri={} headers={} body={}",
          method,
          uri,
          LogSanitizer.redactHeaders(headers),
          body);
    }
  }

  private void logResponse(String method, URI uri, int status, Map<String, List<String>> headers) {
    if (log.isDebugEnabled()) {
      log.debug("HTTP response {} {} -> {}", method, uri, status);
    }
    if (log.isTraceEnabled()) {
      log.trace(
          "HTTP response trace method={} uri={} status={} headers={}",
          method,
          uri,
          status,
          headers);
    }
  }

  // ── Serialization ─────────────────────────────────────────────────────

  private byte[] serializeBody(Object body) {
    if (body == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsBytes(body);
    } catch (JsonProcessingException e) {
      throw new ShareFileSerializationException("Failed to serialize request body", e);
    }
  }

  private <T> T deserialize(byte[] responseBody, Class<T> type) {
    try {
      return objectMapper.readValue(responseBody, type);
    } catch (IOException e) {
      throw new ShareFileSerializationException("Failed to deserialize response body", e);
    }
  }

  private <T> T deserializeWithTypeRef(byte[] responseBody, TypeReference<T> type) {
    try {
      return objectMapper.readValue(responseBody, type);
    } catch (IOException e) {
      throw new ShareFileSerializationException("Failed to deserialize response body", e);
    }
  }

  private JsonNode deserializeTree(byte[] responseBody) {
    try {
      return objectMapper.readTree(responseBody);
    } catch (IOException e) {
      throw new ShareFileSerializationException("Failed to deserialize response body", e);
    }
  }

  // ── Error parsing ─────────────────────────────────────────────────────

  private ErrorBody parseErrorBody(byte[] body) {
    if (body == null || body.length == 0) {
      return new ErrorBody("Unknown", "No error body");
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      String code = root.has("code") ? root.get("code").asText() : "Unknown";
      String message;
      if (root.has("message")) {
        JsonNode msgNode = root.get("message");
        if (msgNode.isObject() && msgNode.has("value")) {
          message = msgNode.get("value").asText();
        } else {
          message = msgNode.asText();
        }
      } else {
        message = new String(body, StandardCharsets.UTF_8);
      }
      return new ErrorBody(code, message);
    } catch (IOException e) {
      return new ErrorBody("Unknown", new String(body, StandardCharsets.UTF_8));
    }
  }

  private static long parseRetryAfter(HttpTransport.HttpResponse response, int status) {
    if (status != 429) {
      return -1;
    }
    Map<String, List<String>> headers = response.headers();
    List<String> retryAfterValues =
        headers.getOrDefault("retry-after", headers.getOrDefault("Retry-After", List.of()));
    if (!retryAfterValues.isEmpty()) {
      try {
        return Long.parseLong(retryAfterValues.get(0));
      } catch (NumberFormatException e) {
        log.debug("Unparseable Retry-After header: {}", retryAfterValues.get(0));
      }
    }
    return -1;
  }

  /** Extracts the entity name from the API path (e.g., "/Items(abc)" → "Items"). */
  private static String extractEntityName(String path) {
    String cleaned = path.startsWith("/") ? path.substring(1) : path;
    int parenIdx = cleaned.indexOf('(');
    int slashIdx = cleaned.indexOf('/');
    int endIdx = cleaned.length();
    if (parenIdx > 0) {
      endIdx = Math.min(endIdx, parenIdx);
    }
    if (slashIdx > 0) {
      endIdx = Math.min(endIdx, slashIdx);
    }
    return endIdx > 0 ? cleaned.substring(0, endIdx) : cleaned;
  }

  private record ErrorBody(String code, String message) {}
}
