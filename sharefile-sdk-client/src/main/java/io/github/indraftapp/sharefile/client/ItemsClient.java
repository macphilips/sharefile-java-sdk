package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.model.Item;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.OperationResult;
import io.github.indraftapp.sharefile.core.model.request.AdvancedSearchRequest;
import io.github.indraftapp.sharefile.core.model.request.BulkDeleteRequest;
import io.github.indraftapp.sharefile.core.model.request.BulkRestoreRequest;
import io.github.indraftapp.sharefile.core.model.request.CheckInRequest;
import io.github.indraftapp.sharefile.core.model.request.FolderCreateRequest;
import io.github.indraftapp.sharefile.core.model.request.LinkCreateRequest;
import io.github.indraftapp.sharefile.core.model.request.NoteCreateRequest;
import io.github.indraftapp.sharefile.core.model.response.AdvancedSearchResults;
import io.github.indraftapp.sharefile.core.model.response.ItemInfo;
import io.github.indraftapp.sharefile.core.model.response.Redirection;
import io.github.indraftapp.sharefile.core.model.response.SearchResults;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Explicit ShareFile resource client for `/Items` endpoints. */
public final class ItemsClient {

  private static final TypeReference<ODataFeed<Item>> ITEM_FEED_TYPE = new TypeReference<>() {};

  private final ResourceRequestExecutor executor;

  ItemsClient(ResourceRequestExecutor executor) {
    this.executor = Objects.requireNonNull(executor, "executor must not be null");
  }

  ItemsClient(ShareFileHttpClient httpClient) {
    this(new ResourceRequestExecutor(httpClient, "/Items"));
  }

  public Item getById(String id) {
    return getById(id, ODataQuery.empty());
  }

  public Item getById(String id, ODataQuery query) {
    return executor.get(executor.entityUri(id), query, Item.class);
  }

  public Item getByPath(String path) {
    return executor.get(
        executor.uriWithParams(
            executor.collectionActionUri("ByPath"), Map.of("path", path == null ? "" : path)),
        ODataQuery.empty(),
        Item.class);
  }

  public Item getByPath(String rootId, String relativePath) {
    return executor.get(
        executor.uriWithParams(
            executor.entityActionUri(rootId, "ByPath"),
            Map.of("path", relativePath == null ? "" : relativePath)),
        ODataQuery.empty(),
        Item.class);
  }

  public Item getParent(String id) {
    return executor.get(executor.entityActionUri(id, "Parent"), ODataQuery.empty(), Item.class);
  }

  public ODataFeed<Item> getChildren(String id) {
    return getChildren(id, ODataQuery.empty());
  }

  public ODataFeed<Item> getChildren(String id, ODataQuery query) {
    return executor.getCollection(executor.entityActionUri(id, "Children"), query, ITEM_FEED_TYPE);
  }

  public ODataFeed<Item> getBreadcrumbs(String id) {
    return executor.getCollection(
        executor.entityActionUri(id, "Breadcrumbs"), ODataQuery.empty(), ITEM_FEED_TYPE);
  }

  public ODataFeed<Item> getDeletedChildren(String parentId) {
    return executor.getCollection(
        executor.entityActionUri(parentId, "DeletedChildren"), ODataQuery.empty(), ITEM_FEED_TYPE);
  }

  public ODataFeed<Item> getVersions(String id) {
    return executor.getCollection(
        executor.entityActionUri(id, "Versions"), ODataQuery.empty(), ITEM_FEED_TYPE);
  }

  public ItemInfo getFolderAccessInfo(String id) {
    return executor.get(
        executor.entityActionUri(id, "AccessInfo"), ODataQuery.empty(), ItemInfo.class);
  }

  public Redirection getThumbnail(String id, int size) {
    return executor.get(
        executor.uriWithParams(
            executor.entityActionUri(id, "Thumbnail"), Map.of("size", Integer.toString(size))),
        ODataQuery.empty(),
        Redirection.class);
  }

  public SearchResults search(String query, Integer maxResults, Integer skip) {
    return executor.get(
        executor.uriWithParams(
            executor.collectionActionUri("Search"), searchParams(query, maxResults, skip)),
        ODataQuery.empty(),
        SearchResults.class);
  }

  public SearchResults search(String folderId, String query, Integer maxResults, Integer skip) {
    return executor.get(
        executor.uriWithParams(
            executor.entityActionUri(folderId, "Search"), searchParams(query, maxResults, skip)),
        ODataQuery.empty(),
        SearchResults.class);
  }

  public AdvancedSearchResults advancedSearch(AdvancedSearchRequest request) {
    return executor.post(
        executor.collectionActionUri("AdvancedSearch"), request, AdvancedSearchResults.class);
  }

  public Item createFolder(String parentId, FolderCreateRequest request) {
    return createFolder(parentId, request, RetryPolicy.DEFAULT);
  }

  public Item createFolder(String parentId, FolderCreateRequest request, RetryPolicy policy) {
    return executor.post(executor.entityActionUri(parentId, "Folder"), request, Item.class, policy);
  }

  public Item createNote(String parentId, NoteCreateRequest request) {
    return createNote(parentId, request, RetryPolicy.DEFAULT);
  }

  public Item createNote(String parentId, NoteCreateRequest request, RetryPolicy policy) {
    return executor.post(executor.entityActionUri(parentId, "Note"), request, Item.class, policy);
  }

  public Item createLink(String parentId, LinkCreateRequest request) {
    return createLink(parentId, request, RetryPolicy.DEFAULT);
  }

  public Item createLink(String parentId, LinkCreateRequest request, RetryPolicy policy) {
    return executor.post(executor.entityActionUri(parentId, "Link"), request, Item.class, policy);
  }

  public OperationResult<Item> update(String id, Item item) {
    return update(id, item, RetryPolicy.DEFAULT);
  }

  public OperationResult<Item> update(String id, Item item, RetryPolicy policy) {
    return executor.patchOperationResult(executor.entityUri(id), item, Item.class, policy);
  }

  public OperationResult<Item> copy(String id, String targetFolderId, boolean overwrite) {
    return copy(id, targetFolderId, overwrite, RetryPolicy.DEFAULT);
  }

  public OperationResult<Item> copy(
      String id, String targetFolderId, boolean overwrite, RetryPolicy policy) {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("targetid", targetFolderId);
    params.put("overwrite", Boolean.toString(overwrite));
    URI uri = executor.uriWithParams(executor.entityActionUri(id, "Copy"), params);
    return executor.postOperationResult(uri, null, Item.class, policy);
  }

  public void delete(String id) {
    executor.delete(executor.entityUri(id));
  }

  public void bulkDelete(String parentId, List<String> itemIds, boolean permanently) {
    bulkDelete(parentId, itemIds, permanently, RetryPolicy.DEFAULT);
  }

  public void bulkDelete(
      String parentId, List<String> itemIds, boolean permanently, RetryPolicy policy) {
    BulkDeleteRequest request = new BulkDeleteRequest();
    request.setItemIds(itemIds);
    URI uri = executor.entityActionUri(parentId, "BulkDelete");
    if (permanently) {
      uri = executor.uriWithParams(uri, Map.of("deletePermanently", Boolean.TRUE.toString()));
    }
    executor.post(uri, request, Void.class, policy);
  }

  public void bulkRestore(List<String> itemIds) {
    bulkRestore(itemIds, RetryPolicy.DEFAULT);
  }

  public void bulkRestore(List<String> itemIds, RetryPolicy policy) {
    BulkRestoreRequest request = new BulkRestoreRequest();
    request.setItemIds(itemIds);
    executor.post(executor.collectionActionUri("BulkRestore"), request, Void.class, policy);
  }

  public Item checkOut(String id) {
    return checkOut(id, RetryPolicy.DEFAULT);
  }

  public Item checkOut(String id, RetryPolicy policy) {
    return executor.post(executor.entityActionUri(id, "CheckOut"), null, Item.class, policy);
  }

  public Item checkIn(String id, String message) {
    return checkIn(id, message, RetryPolicy.DEFAULT);
  }

  public Item checkIn(String id, String message, RetryPolicy policy) {
    CheckInRequest request = new CheckInRequest();
    request.setComment(message);
    return executor.post(executor.entityActionUri(id, "CheckIn"), request, Item.class, policy);
  }

  public void discardCheckOut(String id) {
    discardCheckOut(id, RetryPolicy.DEFAULT);
  }

  public void discardCheckOut(String id, RetryPolicy policy) {
    executor.post(executor.entityActionUri(id, "DiscardCheckOut"), null, Void.class, policy);
  }

  private static Map<String, String> searchParams(String query, Integer maxResults, Integer skip) {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("q", query);
    if (maxResults != null) {
      params.put("$top", maxResults.toString());
    }
    if (skip != null) {
      params.put("$skip", skip.toString());
    }
    return params;
  }
}
