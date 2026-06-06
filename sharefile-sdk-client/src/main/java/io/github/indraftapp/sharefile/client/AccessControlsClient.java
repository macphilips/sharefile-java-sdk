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

  /**
   * Retrieves a single access-control entry by its composite principal/item key.
   *
   * <pre>{@code
   * AccessControl acl = client.accessControls().getById("user-1", "fo123");
   * }</pre>
   *
   * @param principalId principal identifier
   * @param itemId item identifier
   * @return resolved access control entry
   */
  public AccessControl getById(String principalId, String itemId) {
    return executor.get(
        compositeKeyUri(principalId, itemId), ODataQuery.empty(), AccessControl.class);
  }

  /**
   * Lists all access controls applied directly to an item.
   *
   * <pre>{@code
   * ODataFeed<AccessControl> acls = client.accessControls().getByItem("fo123");
   * }</pre>
   *
   * @param itemId item identifier
   * @return feed of access controls for the item
   */
  public ODataFeed<AccessControl> getByItem(String itemId) {
    return executor.getCollection(
        itemScopedUri(itemId), ODataQuery.empty(), ACCESS_CONTROL_FEED_TYPE);
  }

  /**
   * Creates an item-scoped access control using the default retry policy.
   *
   * <pre>{@code
   * OperationResult<AccessControl> result =
   *     client.accessControls().create("fo123", acl, false);
   * }</pre>
   *
   * @param itemId item identifier
   * @param acl access-control payload
   * @param recursive whether the change should be applied recursively
   * @return completed access control or pending async operation result
   */
  public OperationResult<AccessControl> create(
      String itemId, AccessControl acl, boolean recursive) {
    return create(itemId, acl, recursive, RetryPolicy.DEFAULT);
  }

  /**
   * Creates an item-scoped access control with an explicit retry policy.
   *
   * @param itemId item identifier
   * @param acl access-control payload
   * @param recursive whether the change should be applied recursively
   * @param policy retry policy override
   * @return completed access control or pending async operation result
   */
  public OperationResult<AccessControl> create(
      String itemId, AccessControl acl, boolean recursive, RetryPolicy policy) {
    return executor.postOperationResult(
        executor.uriWithParams(itemScopedUri(itemId), recursiveParam(recursive)),
        acl,
        AccessControl.class,
        policy);
  }

  /**
   * Updates an item-scoped access control using the default retry policy.
   *
   * <pre>{@code
   * OperationResult<AccessControl> result =
   *     client.accessControls().update("fo123", acl, true);
   * }</pre>
   *
   * @param itemId item identifier
   * @param acl updated access-control payload
   * @param recursive whether the change should be applied recursively
   * @return completed access control or pending async operation result
   */
  public OperationResult<AccessControl> update(
      String itemId, AccessControl acl, boolean recursive) {
    return update(itemId, acl, recursive, RetryPolicy.DEFAULT);
  }

  /**
   * Updates an item-scoped access control with an explicit retry policy.
   *
   * @param itemId item identifier
   * @param acl updated access-control payload
   * @param recursive whether the change should be applied recursively
   * @param policy retry policy override
   * @return completed access control or pending async operation result
   */
  public OperationResult<AccessControl> update(
      String itemId, AccessControl acl, boolean recursive, RetryPolicy policy) {
    return executor.patchOperationResult(
        executor.uriWithParams(itemScopedUri(itemId), recursiveParam(recursive)),
        acl,
        AccessControl.class,
        policy);
  }

  /**
   * Deletes an access-control entry for a principal on an item.
   *
   * <pre>{@code
   * client.accessControls().delete("user-1", "fo123");
   * }</pre>
   *
   * @param principalId principal identifier
   * @param itemId item identifier
   */
  public void delete(String principalId, String itemId) {
    executor.delete(compositeKeyUri(principalId, itemId));
  }

  /**
   * Applies a bulk access-control request to an item using the default retry policy.
   *
   * <pre>{@code
   * AccessControlBulkResult result = client.accessControls().bulkSet("fo123", request);
   * }</pre>
   *
   * @param itemId item identifier
   * @param request bulk access-control request payload
   * @return bulk operation result
   */
  public AccessControlBulkResult bulkSet(String itemId, BulkAccessControlRequest request) {
    return bulkSet(itemId, request, RetryPolicy.DEFAULT);
  }

  /**
   * Applies a bulk access-control request to an item with an explicit retry policy.
   *
   * @param itemId item identifier
   * @param request bulk access-control request payload
   * @param policy retry policy override
   * @return bulk operation result
   */
  public AccessControlBulkResult bulkSet(
      String itemId, BulkAccessControlRequest request, RetryPolicy policy) {
    return executor.post(
        itemScopedActionUri(itemId, "BulkSet"), request, AccessControlBulkResult.class, policy);
  }

  /**
   * Applies a bulk access-control request for a principal across multiple items using the default
   * retry policy.
   *
   * <pre>{@code
   * AccessControlBulkResult result =
   *     client.accessControls().bulkSetForPrincipal("user-1", request);
   * }</pre>
   *
   * @param principalId principal identifier
   * @param request bulk access-control request payload
   * @return bulk operation result
   */
  public AccessControlBulkResult bulkSetForPrincipal(
      String principalId, BulkAccessControlRequest request) {
    return bulkSetForPrincipal(principalId, request, RetryPolicy.DEFAULT);
  }

  /**
   * Applies a bulk access-control request for a principal with an explicit retry policy.
   *
   * @param principalId principal identifier
   * @param request bulk access-control request payload
   * @param policy retry policy override
   * @return bulk operation result
   */
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

  /**
   * Clones access controls from one principal to other principals in a folder using the default
   * retry policy.
   *
   * <pre>{@code
   * client.accessControls().clone("fo123", "group-source", List.of("group-a", "group-b"));
   * }</pre>
   *
   * @param folderId folder identifier
   * @param principalId source principal identifier
   * @param clonePrincipalIds target principal identifiers
   */
  public void clone(String folderId, String principalId, List<String> clonePrincipalIds) {
    clone(folderId, principalId, clonePrincipalIds, RetryPolicy.DEFAULT);
  }

  /**
   * Clones access controls from one principal to other principals with an explicit retry policy.
   *
   * @param folderId folder identifier
   * @param principalId source principal identifier
   * @param clonePrincipalIds target principal identifiers
   * @param policy retry policy override
   */
  public void clone(
      String folderId, String principalId, List<String> clonePrincipalIds, RetryPolicy policy) {
    CloneRequest request = new CloneRequest();
    request.setFolderId(folderId);
    request.setPrincipalId(principalId);
    request.setClonePrincipalIds(clonePrincipalIds);
    executor.post(executor.collectionActionUri("Clone"), request, Void.class, policy);
  }

  /**
   * Bulk-deletes principals from an item's access controls using the default retry policy.
   *
   * <pre>{@code
   * client.accessControls().bulkDelete("fo123", List.of("user-1", "group-2"));
   * }</pre>
   *
   * @param itemId item identifier
   * @param principalIds principal identifiers to remove
   */
  public void bulkDelete(String itemId, List<String> principalIds) {
    bulkDelete(itemId, principalIds, RetryPolicy.DEFAULT);
  }

  /**
   * Bulk-deletes principals from an item's access controls with an explicit retry policy.
   *
   * @param itemId item identifier
   * @param principalIds principal identifiers to remove
   * @param policy retry policy override
   */
  public void bulkDelete(String itemId, List<String> principalIds, RetryPolicy policy) {
    executor.post(itemScopedActionUri(itemId, "BulkDelete"), principalIds, Void.class, policy);
  }

  /**
   * Sends ACL notification messages to users using the default retry policy.
   *
   * <pre>{@code
   * client.accessControls().notifyUsers("fo123", List.of("user-1"), "Access granted");
   * }</pre>
   *
   * @param itemId item identifier
   * @param userIds user identifiers to notify
   * @param message custom notification message
   */
  public void notifyUsers(String itemId, List<String> userIds, String message) {
    notifyUsers(itemId, userIds, message, RetryPolicy.DEFAULT);
  }

  /**
   * Sends ACL notification messages to users with an explicit retry policy.
   *
   * @param itemId item identifier
   * @param userIds user identifiers to notify
   * @param message custom notification message
   * @param policy retry policy override
   */
  public void notifyUsers(String itemId, List<String> userIds, String message, RetryPolicy policy) {
    NotifyRequest request = new NotifyRequest();
    request.setUserIds(userIds);
    request.setMessage(message);
    executor.post(itemScopedActionUri(itemId, "NotifyUsers"), request, Void.class, policy);
  }

  private URI compositeKeyUri(String principalId, String itemId) {
    LinkedHashMap<String, String> keys = new LinkedHashMap<>();
    keys.put("principalid", principalId);
    keys.put("itemid", itemId);
    return executor.compositeKeyUri(keys);
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
