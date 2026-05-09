package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.model.AccessControl;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.OperationResult;
import io.github.indraftapp.sharefile.core.model.request.BulkAccessControlRequest;
import io.github.indraftapp.sharefile.core.model.request.CloneRequest;
import io.github.indraftapp.sharefile.core.model.request.NotifyRequest;
import io.github.indraftapp.sharefile.core.model.response.AccessControlBulkResult;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Explicit ShareFile resource client for `/AccessControls` endpoints. */
public final class AccessControlsClient {

  private static final TypeReference<ODataFeed<AccessControl>> ACCESS_CONTROL_FEED_TYPE =
      new TypeReference<>() {};

  private final ResourceRequestExecutor executor;

  AccessControlsClient(ResourceRequestExecutor executor) {
    this.executor = Objects.requireNonNull(executor, "executor must not be null");
  }

  AccessControlsClient(ShareFileHttpClient httpClient) {
    this(new ResourceRequestExecutor(httpClient, "/AccessControls"));
  }

  public AccessControl getById(String principalId, String itemId) {
    return executor.get(
        compositeKeyUri(principalId, itemId), ODataQuery.empty(), AccessControl.class);
  }

  public ODataFeed<AccessControl> getByItem(String itemId) {
    return executor.getCollection(
        itemScopedUri(itemId), ODataQuery.empty(), ACCESS_CONTROL_FEED_TYPE);
  }

  public OperationResult<AccessControl> create(
      String itemId, AccessControl acl, boolean recursive) {
    return create(itemId, acl, recursive, RetryPolicy.DEFAULT);
  }

  public OperationResult<AccessControl> create(
      String itemId, AccessControl acl, boolean recursive, RetryPolicy policy) {
    return executor.postOperationResult(
        executor.uriWithParams(itemScopedUri(itemId), recursiveParam(recursive)),
        acl,
        AccessControl.class,
        policy);
  }

  public OperationResult<AccessControl> update(
      String itemId, AccessControl acl, boolean recursive) {
    return update(itemId, acl, recursive, RetryPolicy.DEFAULT);
  }

  public OperationResult<AccessControl> update(
      String itemId, AccessControl acl, boolean recursive, RetryPolicy policy) {
    return executor.patchOperationResult(
        executor.uriWithParams(itemScopedUri(itemId), recursiveParam(recursive)),
        acl,
        AccessControl.class,
        policy);
  }

  public void delete(String principalId, String itemId) {
    executor.delete(compositeKeyUri(principalId, itemId));
  }

  public AccessControlBulkResult bulkSet(String itemId, BulkAccessControlRequest request) {
    return bulkSet(itemId, request, RetryPolicy.DEFAULT);
  }

  public AccessControlBulkResult bulkSet(
      String itemId, BulkAccessControlRequest request, RetryPolicy policy) {
    return executor.post(
        itemScopedActionUri(itemId, "BulkSet"), request, AccessControlBulkResult.class, policy);
  }

  public AccessControlBulkResult bulkSetForPrincipal(
      String principalId, BulkAccessControlRequest request) {
    return bulkSetForPrincipal(principalId, request, RetryPolicy.DEFAULT);
  }

  public AccessControlBulkResult bulkSetForPrincipal(
      String principalId, BulkAccessControlRequest request, RetryPolicy policy) {
    return executor.post(
        executor.uriWithParams(
            executor.collectionActionUri("BulkSetForPrincipal"),
            Map.of("principalId", principalId)),
        request,
        AccessControlBulkResult.class,
        policy);
  }

  public void clone(String folderId, String principalId, List<String> clonePrincipalIds) {
    clone(folderId, principalId, clonePrincipalIds, RetryPolicy.DEFAULT);
  }

  public void clone(
      String folderId, String principalId, List<String> clonePrincipalIds, RetryPolicy policy) {
    CloneRequest request = new CloneRequest();
    request.setFolderId(folderId);
    request.setPrincipalId(principalId);
    request.setClonePrincipalIds(clonePrincipalIds);
    executor.post(executor.collectionActionUri("Clone"), request, Void.class, policy);
  }

  public void bulkDelete(String itemId, List<String> principalIds) {
    bulkDelete(itemId, principalIds, RetryPolicy.DEFAULT);
  }

  public void bulkDelete(String itemId, List<String> principalIds, RetryPolicy policy) {
    executor.post(itemScopedActionUri(itemId, "BulkDelete"), principalIds, Void.class, policy);
  }

  public void notifyUsers(String itemId, List<String> userIds, String message) {
    notifyUsers(itemId, userIds, message, RetryPolicy.DEFAULT);
  }

  public void notifyUsers(String itemId, List<String> userIds, String message, RetryPolicy policy) {
    NotifyRequest request = new NotifyRequest();
    request.setUserIds(userIds);
    request.setMessage(message);
    executor.post(itemScopedActionUri(itemId, "NotifyUsers"), request, Void.class, policy);
  }

  private URI compositeKeyUri(String principalId, String itemId) {
    return executor.compositeKeyUri("principalid=" + principalId, "itemid=" + itemId);
  }

  private URI itemScopedUri(String itemId) {
    return executor.relativeUri("/Items(" + itemId + ")/AccessControls");
  }

  private URI itemScopedActionUri(String itemId, String action) {
    return executor.relativeUri(itemScopedUri(itemId).toString() + "/" + action);
  }

  private static Map<String, String> recursiveParam(boolean recursive) {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("recursive", Boolean.toString(recursive));
    return params;
  }
}
