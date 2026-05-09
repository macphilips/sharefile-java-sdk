package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.model.AsyncOperation;
import io.github.indraftapp.sharefile.core.model.ODataEntity;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.OperationResult;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Shared request helper for explicit ShareFile resource clients. */
final class ResourceRequestExecutor {

  private final ShareFileHttpClient httpClient;
  private final String basePath;

  ResourceRequestExecutor(ShareFileHttpClient httpClient, String basePath) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    this.basePath = normalizeBasePath(basePath);
  }

  URI collectionUri() {
    return URI.create(basePath);
  }

  URI collectionActionUri(String action) {
    Objects.requireNonNull(action, "action must not be null");
    return URI.create(basePath + "/" + action);
  }

  URI entityUri(String id) {
    Objects.requireNonNull(id, "id must not be null");
    return URI.create(basePath + "(" + id + ")");
  }

  URI entityActionUri(String id, String action) {
    Objects.requireNonNull(action, "action must not be null");
    return URI.create(entityUri(id).toString() + "/" + action);
  }

  URI compositeKeyUri(String... keyFragments) {
    Objects.requireNonNull(keyFragments, "keyFragments must not be null");
    if (keyFragments.length == 0) {
      throw new IllegalArgumentException("keyFragments must not be empty");
    }
    return URI.create(basePath + "(" + String.join(",", keyFragments) + ")");
  }

  URI uriWithParams(URI uri, Map<String, String> params) {
    return appendParams(uri, params);
  }

  <T> T get(URI uri, ODataQuery query, Class<T> type) {
    return httpClient.get(appendQuery(uri, query), type);
  }

  <T> T get(URI uri, ODataQuery query, TypeReference<T> type) {
    return httpClient.get(appendQuery(uri, query), type);
  }

  <T> T post(URI uri, Object body, Class<T> type) {
    return post(uri, body, type, RetryPolicy.DEFAULT);
  }

  <T> T post(URI uri, Object body, Class<T> type, RetryPolicy policy) {
    return httpClient.post(uri, body, type, policy);
  }

  <T> T patch(URI uri, Object body, Class<T> type) {
    return patch(uri, body, type, RetryPolicy.DEFAULT);
  }

  <T> T patch(URI uri, Object body, Class<T> type, RetryPolicy policy) {
    return httpClient.patch(uri, body, type, policy);
  }

  void delete(URI uri) {
    httpClient.delete(uri);
  }

  <T> ODataFeed<T> getCollection(URI uri, ODataQuery query, TypeReference<ODataFeed<T>> type) {
    return httpClient.get(appendQuery(uri, query), type);
  }

  <T> ODataFeed<T> getNextPage(ODataFeed<T> current, TypeReference<ODataFeed<T>> type) {
    Objects.requireNonNull(current, "current feed must not be null");
    if (!current.hasNextPage()) {
      throw new IllegalArgumentException("Current feed does not have an OData next page link");
    }
    return httpClient.get(URI.create(current.getNextLink()), type);
  }

  <T extends ODataEntity> OperationResult<T> postOperationResult(
      URI uri, Object body, Class<T> entityType, RetryPolicy policy) {
    return toOperationResult(httpClient.postForJsonNode(uri, body, policy), entityType);
  }

  <T extends ODataEntity> OperationResult<T> patchOperationResult(
      URI uri, Object body, Class<T> entityType, RetryPolicy policy) {
    return toOperationResult(httpClient.patchForJsonNode(uri, body, policy), entityType);
  }

  private <T extends ODataEntity> OperationResult<T> toOperationResult(
      JsonNode node, Class<T> entityType) {
    ODataEntity entity = httpClient.convertValue(node, ODataEntity.class);
    if (entity instanceof AsyncOperation operation) {
      return new OperationResult.Pending<>(operation);
    }
    if (entityType.isInstance(entity)) {
      return new OperationResult.Completed<>(entityType.cast(entity));
    }
    return new OperationResult.Completed<>(httpClient.convertValue(node, entityType));
  }

  private URI appendQuery(URI uri, ODataQuery query) {
    if (query == null || query.isEmpty()) {
      return uri;
    }
    return appendParams(uri, query.toQueryParams());
  }

  private URI appendParams(URI uri, Map<String, String> params) {
    if (params == null || params.isEmpty()) {
      return uri;
    }

    Map<String, String> orderedParams = new LinkedHashMap<>(params);
    StringBuilder sb = new StringBuilder(uri.toString());
    sb.append(uri.getQuery() == null || uri.getQuery().isEmpty() ? '?' : '&');

    boolean first = true;
    for (Map.Entry<String, String> entry : orderedParams.entrySet()) {
      if (!first) {
        sb.append('&');
      }
      sb.append(encode(entry.getKey()));
      sb.append('=');
      sb.append(encode(entry.getValue()));
      first = false;
    }
    return URI.create(sb.toString());
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private static String normalizeBasePath(String basePath) {
    Objects.requireNonNull(basePath, "basePath must not be null");
    if (basePath.isBlank()) {
      throw new IllegalArgumentException("basePath must not be blank");
    }
    return basePath.startsWith("/") ? basePath : "/" + basePath;
  }
}
