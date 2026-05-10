package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.model.Item;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.OperationResult;
import io.github.indraftapp.sharefile.core.model.enums.DlpStatus;
import io.github.indraftapp.sharefile.core.model.enums.TreeMode;
import io.github.indraftapp.sharefile.core.model.request.AdvancedSearchRequest;
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

  /**
   * Retrieves a single item by ID with no extra query parameters.
   *
   * <p>Use this for the common case:
   *
   * <pre>{@code
   * Item item = client.items().getById("fo123");
   * }</pre>
   *
   * @param id item identifier or special ShareFile ID such as {@code home}
   * @return the resolved item
   */
  public Item getById(String id) {
    return getById(id, ItemQuery.empty());
  }

  /**
   * Retrieves a single item by ID with OData query options.
   *
   * <p>Use this when you need OData fields such as {@code $expand} or {@code $select}:
   *
   * <pre>{@code
   * Item item = client.items().getById("fo123", ODataQuery.builder().expand("Parent").build());
   * }</pre>
   *
   * @param id item identifier or special ShareFile ID such as {@code home}
   * @param query OData query options to append to the request
   * @return the resolved item
   */
  public Item getById(String id, ODataQuery query) {
    return getById(id, ItemQuery.builder().odata(query).build());
  }

  /**
   * Retrieves a single item by ID with merged OData and ShareFile-specific query parameters.
   *
   * <p>Use this overload when the endpoint needs non-OData parameters such as {@code
   * includeDeleted}:
   *
   * <pre>{@code
   * Item item = client.items().getById(
   *     "fo123",
   *     ItemQuery.builder().includeDeleted(true).build());
   * }</pre>
   *
   * @param id item identifier or special ShareFile ID such as {@code home}
   * @param query merged item query parameters
   * @return the resolved item
   */
  public Item getById(String id, ItemQuery query) {
    return executor.get(executor.entityUri(id), query, Item.class);
  }

  /**
   * Resolves an item by absolute ShareFile path.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Item item = client.items().getByPath("/folder1/folder2/file.txt");
   * }</pre>
   *
   * @param path absolute ShareFile path
   * @return the resolved item
   */
  public Item getByPath(String path) {
    return getByPath(ItemQuery.builder().path(path == null ? "" : path).build());
  }

  /**
   * Resolves an item by path relative to a root item.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Item item = client.items().getByPath("fo-root", "child-folder/file.txt");
   * }</pre>
   *
   * @param rootId root item identifier
   * @param relativePath relative path from {@code rootId}
   * @return the resolved item
   */
  public Item getByPath(String rootId, String relativePath) {
    return getByPath(
        rootId, ItemQuery.builder().path(relativePath == null ? "" : relativePath).build());
  }

  /**
   * Resolves an item by absolute path using an {@link ItemQuery}.
   *
   * <p>The query must include {@code path}. Use this overload when the endpoint also needs
   * ShareFile-specific parameters such as {@code includeDeleted}.
   *
   * <pre>{@code
   * Item item = client.items().getByPath(
   *     ItemQuery.builder()
   *         .path("/folder1/archive.pdf")
   *         .includeDeleted(true)
   *         .build());
   * }</pre>
   *
   * @param query item query containing at least {@code path}
   * @return the resolved item
   * @throws IllegalArgumentException if {@code path} is missing or blank
   */
  public Item getByPath(ItemQuery query) {
    requireQueryParam(query, "path", "ByPath");
    return executor.get(executor.collectionActionUri("ByPath"), query, Item.class);
  }

  /**
   * Resolves an item by path relative to a root item using an {@link ItemQuery}.
   *
   * <p>The query must include {@code path}.
   *
   * <pre>{@code
   * Item item = client.items().getByPath(
   *     "fo-root",
   *     ItemQuery.builder().path("child/report.pdf").build());
   * }</pre>
   *
   * @param rootId root item identifier
   * @param query item query containing at least {@code path}
   * @return the resolved item
   * @throws IllegalArgumentException if {@code path} is missing or blank
   */
  public Item getByPath(String rootId, ItemQuery query) {
    requireQueryParam(query, "path", "ByPath");
    return executor.get(executor.entityActionUri(rootId, "ByPath"), query, Item.class);
  }

  /**
   * Retrieves the parent of an item.
   *
   * <pre>{@code
   * Item parent = client.items().getParent("fo123");
   * }</pre>
   *
   * @param id target item identifier
   * @return the parent item
   */
  public Item getParent(String id) {
    return getParent(id, ItemQuery.empty());
  }

  /**
   * Retrieves the parent of an item with additional item query parameters.
   *
   * <pre>{@code
   * Item parent = client.items().getParent(
   *     "fo123",
   *     ItemQuery.builder().includeDeleted(true).build());
   * }</pre>
   *
   * @param id target item identifier
   * @param query merged item query parameters
   * @return the parent item
   */
  public Item getParent(String id, ItemQuery query) {
    return executor.get(executor.entityActionUri(id, "Parent"), query, Item.class);
  }

  /**
   * Lists the children of a folder with default query behavior.
   *
   * <pre>{@code
   * ODataFeed<Item> children = client.items().getChildren("fo-folder");
   * }</pre>
   *
   * @param id folder identifier
   * @return feed of child items
   */
  public ODataFeed<Item> getChildren(String id) {
    return getChildren(id, ItemQuery.empty());
  }

  /**
   * Lists the children of a folder using OData query options.
   *
   * <pre>{@code
   * ODataFeed<Item> children = client.items().getChildren(
   *     "fo-folder",
   *     ODataQuery.builder().top(25).build());
   * }</pre>
   *
   * @param id folder identifier
   * @param query OData query options such as {@code $top} or {@code $filter}
   * @return feed of child items
   */
  public ODataFeed<Item> getChildren(String id, ODataQuery query) {
    return getChildren(id, ItemQuery.builder().odata(query).build());
  }

  /**
   * Lists the children of a folder using merged OData and ShareFile-specific query parameters.
   *
   * <p>Use this overload for documented Item parameters such as {@code includeDeleted} and {@code
   * orderingMode}.
   *
   * <pre>{@code
   * ODataFeed<Item> children = client.items().getChildren(
   *     "fo-folder",
   *     ItemQuery.builder().includeDeleted(true).build());
   * }</pre>
   *
   * @param id folder identifier
   * @param query merged item query parameters
   * @return feed of child items
   */
  public ODataFeed<Item> getChildren(String id, ItemQuery query) {
    return executor.getCollection(executor.entityActionUri(id, "Children"), query, ITEM_FEED_TYPE);
  }

  /**
   * Retrieves the breadcrumb path from the root to a target item.
   *
   * <pre>{@code
   * ODataFeed<Item> breadcrumbs = client.items().getBreadcrumbs("fi123");
   * }</pre>
   *
   * @param id target item identifier
   * @return feed of breadcrumb items
   */
  public ODataFeed<Item> getBreadcrumbs(String id) {
    return getBreadcrumbs(id, ItemQuery.empty());
  }

  /**
   * Retrieves the breadcrumb path from the root to a target item with additional query parameters.
   *
   * <pre>{@code
   * ODataFeed<Item> breadcrumbs = client.items().getBreadcrumbs(
   *     "fi123",
   *     ItemQuery.builder().includeDeleted(true).build());
   * }</pre>
   *
   * @param id target item identifier
   * @param query merged item query parameters
   * @return feed of breadcrumb items
   */
  public ODataFeed<Item> getBreadcrumbs(String id, ItemQuery query) {
    return executor.getCollection(
        executor.entityActionUri(id, "Breadcrumbs"), query, ITEM_FEED_TYPE);
  }

  /**
   * Lists deleted children for a parent folder.
   *
   * <pre>{@code
   * ODataFeed<Item> deleted = client.items().getDeletedChildren("fo-parent");
   * }</pre>
   *
   * @param parentId parent folder identifier
   * @return feed of deleted child items
   */
  public ODataFeed<Item> getDeletedChildren(String parentId) {
    return getDeletedChildren(parentId, ItemQuery.empty());
  }

  /**
   * Lists deleted children for a parent folder with additional query parameters.
   *
   * <pre>{@code
   * ODataFeed<Item> deleted = client.items().getDeletedChildren(
   *     "fo-parent",
   *     ItemQuery.builder().includeDeleted(true).build());
   * }</pre>
   *
   * @param parentId parent folder identifier
   * @param query merged item query parameters
   * @return feed of deleted child items
   */
  public ODataFeed<Item> getDeletedChildren(String parentId, ItemQuery query) {
    return executor.getCollection(
        executor.entityActionUri(parentId, "DeletedChildren"), query, ITEM_FEED_TYPE);
  }

  /**
   * Lists the versions in a file stream.
   *
   * <pre>{@code
   * ODataFeed<Item> versions = client.items().getVersions("st-stream");
   * }</pre>
   *
   * @param id stream or file identifier
   * @return feed of file versions
   */
  public ODataFeed<Item> getVersions(String id) {
    return getVersions(id, ItemQuery.empty());
  }

  /**
   * Lists the versions in a file stream with additional query parameters.
   *
   * <p>Use this overload for flags such as {@code includeDeleted}.
   *
   * <pre>{@code
   * ODataFeed<Item> versions = client.items().getVersions(
   *     "st-stream",
   *     ItemQuery.builder().includeDeleted(true).build());
   * }</pre>
   *
   * @param id stream or file identifier
   * @param query merged item query parameters
   * @return feed of file versions
   */
  public ODataFeed<Item> getVersions(String id, ItemQuery query) {
    return executor.getCollection(executor.entityActionUri(id, "Stream"), query, ITEM_FEED_TYPE);
  }

  /**
   * Retrieves folder access information.
   *
   * <pre>{@code
   * ItemInfo info = client.items().getFolderAccessInfo("fo-folder");
   * }</pre>
   *
   * @param id folder identifier
   * @return folder access information
   */
  public ItemInfo getFolderAccessInfo(String id) {
    return getFolderAccessInfo(id, ItemQuery.empty());
  }

  /**
   * Retrieves folder access information with additional query parameters.
   *
   * <pre>{@code
   * ItemInfo info = client.items().getFolderAccessInfo(
   *     "fo-folder",
   *     ItemQuery.empty());
   * }</pre>
   *
   * @param id folder identifier
   * @param query merged item query parameters
   * @return folder access information
   */
  public ItemInfo getFolderAccessInfo(String id, ItemQuery query) {
    return executor.get(executor.entityActionUri(id, "Info"), query, ItemInfo.class);
  }

  /**
   * Retrieves a thumbnail for an item using the given size.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Redirection thumbnail = client.items().getThumbnail("fi123", 600);
   * }</pre>
   *
   * @param id item identifier
   * @param size thumbnail size, typically {@code 75} or {@code 600}
   * @return thumbnail redirection payload
   */
  public Redirection getThumbnail(String id, int size) {
    return getThumbnail(id, ItemQuery.builder().size(size).build());
  }

  /**
   * Retrieves a thumbnail for an item using an {@link ItemQuery}.
   *
   * <p>Use this overload for the documented thumbnail query parameters such as {@code size} and
   * {@code redirect}.
   *
   * <pre>{@code
   * Redirection thumbnail = client.items().getThumbnail(
   *     "fi123",
   *     ItemQuery.builder().size(600).redirect(false).build());
   * }</pre>
   *
   * @param id item identifier
   * @param query merged item query parameters
   * @return thumbnail redirection payload
   */
  public Redirection getThumbnail(String id, ItemQuery query) {
    return executor.get(executor.entityActionUri(id, "Thumbnail"), query, Redirection.class);
  }

  /**
   * Runs a global item search.
   *
   * <pre>{@code
   * SearchResults results = client.items().search("quarterly", 25, 0);
   * }</pre>
   *
   * @param query search text
   * @param maxResults maximum number of results to return, or {@code null} for the API default
   * @param skip number of results to skip, or {@code null} for the API default
   * @return search results
   */
  public SearchResults search(String query, Integer maxResults, Integer skip) {
    return search(ItemQuery.builder().query(query).maxResults(maxResults).skip(skip).build());
  }

  /**
   * Runs a folder-scoped item search.
   *
   * <pre>{@code
   * SearchResults results = client.items().search("fo-folder", "quarterly", 25, 0);
   * }</pre>
   *
   * @param folderId parent folder identifier
   * @param query search text
   * @param maxResults maximum number of results to return, or {@code null} for the API default
   * @param skip number of results to skip, or {@code null} for the API default
   * @return search results
   */
  public SearchResults search(String folderId, String query, Integer maxResults, Integer skip) {
    return search(
        folderId, ItemQuery.builder().query(query).maxResults(maxResults).skip(skip).build());
  }

  /**
   * Runs a global item search using an {@link ItemQuery}.
   *
   * <p>The query must include {@code query}. Use this overload for additional parameters such as
   * {@code homeFolderOnly}.
   *
   * <pre>{@code
   * SearchResults results = client.items().search(
   *     ItemQuery.builder().query("quarterly").homeFolderOnly(true).build());
   * }</pre>
   *
   * @param query merged item query parameters containing at least {@code query}
   * @return search results
   * @throws IllegalArgumentException if {@code query} is missing or blank
   */
  public SearchResults search(ItemQuery query) {
    requireQueryParam(query, "query", "Search");
    return executor.get(executor.collectionActionUri("Search"), query, SearchResults.class);
  }

  /**
   * Runs a folder-scoped item search using an {@link ItemQuery}.
   *
   * <p>The query must include {@code query}.
   *
   * <pre>{@code
   * SearchResults results = client.items().search(
   *     "fo-folder",
   *     ItemQuery.builder().query("quarterly").maxResults(10).build());
   * }</pre>
   *
   * @param folderId parent folder identifier
   * @param query merged item query parameters containing at least {@code query}
   * @return search results
   * @throws IllegalArgumentException if {@code query} is missing or blank
   */
  public SearchResults search(String folderId, ItemQuery query) {
    requireQueryParam(query, "query", "Search");
    return executor.get(executor.entityActionUri(folderId, "Search"), query, SearchResults.class);
  }

  /**
   * Retrieves a TreeView root item for copy, move, or manage workflows.
   *
   * <pre>{@code
   * Item root = client.items().treeView("fo-folder", TreeMode.COPY);
   * }</pre>
   *
   * @param id folder identifier
   * @param treeMode required tree mode
   * @return tree root item
   */
  public Item treeView(String id, TreeMode treeMode) {
    return treeView(id, ItemQuery.builder().treeMode(treeMode).build());
  }

  /**
   * Retrieves a TreeView root item using an {@link ItemQuery}.
   *
   * <p>The query must include {@code treemode}. Optional parameters such as {@code sourceId},
   * {@code canCreateRootFolder}, and {@code fileBox} may also be supplied.
   *
   * <pre>{@code
   * Item root = client.items().treeView(
   *     "fo-folder",
   *     ItemQuery.builder().treeMode(TreeMode.MOVE).sourceId("fi123").build());
   * }</pre>
   *
   * @param id folder identifier
   * @param query merged item query parameters containing at least {@code treemode}
   * @return tree root item
   * @throws IllegalArgumentException if {@code treemode} is missing or blank
   */
  public Item treeView(String id, ItemQuery query) {
    requireQueryParam(query, "treemode", "TreeView");
    return executor.get(executor.entityActionUri(id, "TreeView"), query, Item.class);
  }

  /**
   * Lists deleted items for a user.
   *
   * <pre>{@code
   * ODataFeed<Item> deleted = client.items().getUserDeletedItems("user-1");
   * }</pre>
   *
   * @param userId user identifier
   * @return feed of deleted items for that user
   */
  public ODataFeed<Item> getUserDeletedItems(String userId) {
    return getUserDeletedItems(ItemQuery.builder().userId(userId).build());
  }

  /**
   * Lists deleted items for a user using an {@link ItemQuery}.
   *
   * <p>The query must include {@code userid}. Use {@code zone} to further constrain the result.
   *
   * <pre>{@code
   * ODataFeed<Item> deleted = client.items().getUserDeletedItems(
   *     ItemQuery.builder().userId("user-1").zone("zone-1").build());
   * }</pre>
   *
   * @param query merged item query parameters containing at least {@code userid}
   * @return feed of deleted items for the user
   * @throws IllegalArgumentException if {@code userid} is missing or blank
   */
  public ODataFeed<Item> getUserDeletedItems(ItemQuery query) {
    requireQueryParam(query, "userid", "UserDeletedItems");
    return executor.getCollection(
        executor.collectionActionUri("UserDeletedItems"), query, ITEM_FEED_TYPE);
  }

  /**
   * Lists items by DLP status.
   *
   * <pre>{@code
   * ODataFeed<Item> items = client.items().getByDlpStatus(DlpStatus.SCANNED_OK);
   * }</pre>
   *
   * @param status required DLP status filter
   * @return feed of matching items
   */
  public ODataFeed<Item> getByDlpStatus(DlpStatus status) {
    return getByDlpStatus(ItemQuery.builder().status(status).build());
  }

  /**
   * Lists items by DLP status using an {@link ItemQuery}.
   *
   * <p>The query must include {@code status}. Optional parameters such as {@code zone} and {@code
   * endDate} may also be supplied.
   *
   * <pre>{@code
   * ODataFeed<Item> items = client.items().getByDlpStatus(
   *     ItemQuery.builder().status(DlpStatus.SCANNED_OK).zone("zone-1").build());
   * }</pre>
   *
   * @param query merged item query parameters containing at least {@code status}
   * @return feed of matching items
   * @throws IllegalArgumentException if {@code status} is missing or blank
   */
  public ODataFeed<Item> getByDlpStatus(ItemQuery query) {
    requireQueryParam(query, "status", "ByDlpStatus");
    return executor.getCollection(
        executor.collectionActionUri("ByDlpStatus"), query, ITEM_FEED_TYPE);
  }

  /**
   * Executes the advanced search endpoint.
   *
   * <p>Use this when the simple search endpoints do not provide enough filtering or paging control.
   *
   * <pre>{@code
   * AdvancedSearchResults results = client.items().advancedSearch(request);
   * }</pre>
   *
   * @param request advanced search request payload
   * @return advanced search results
   */
  public AdvancedSearchResults advancedSearch(AdvancedSearchRequest request) {
    return executor.post(
        executor.collectionActionUri("AdvancedSearch"), request, AdvancedSearchResults.class);
  }

  /**
   * Creates a folder under the given parent item.
   *
   * <pre>{@code
   * Item folder = client.items().createFolder("fo-parent", request);
   * }</pre>
   *
   * @param parentId parent folder identifier
   * @param request folder creation payload
   * @return the created folder item
   */
  public Item createFolder(String parentId, FolderCreateRequest request) {
    return createFolder(parentId, request, RetryPolicy.DEFAULT);
  }

  /**
   * Creates a folder under the given parent item with an explicit retry policy.
   *
   * <pre>{@code
   * Item folder = client.items().createFolder(
   *     "fo-parent",
   *     request,
   *     RetryPolicy.retryOnServerError(1));
   * }</pre>
   *
   * @param parentId parent folder identifier
   * @param request folder creation payload
   * @param policy retry policy to use for the request
   * @return the created folder item
   */
  public Item createFolder(String parentId, FolderCreateRequest request, RetryPolicy policy) {
    return executor.post(executor.entityActionUri(parentId, "Folder"), request, Item.class, policy);
  }

  /**
   * Creates a folder under the given parent item with documented ShareFile query parameters.
   *
   * <pre>{@code
   * Item folder = client.items().createFolder("fo-parent", request, true, false);
   * }</pre>
   *
   * @param parentId parent folder identifier
   * @param request folder creation payload
   * @param overwrite whether an existing folder may be overwritten
   * @param passthrough passthrough flag, or {@code null} to omit it
   * @return the created folder item
   */
  public Item createFolder(
      String parentId, FolderCreateRequest request, Boolean overwrite, Boolean passthrough) {
    return createFolder(parentId, request, overwrite, passthrough, RetryPolicy.DEFAULT);
  }

  /**
   * Creates a folder under the given parent item with documented query parameters and a custom
   * retry policy.
   *
   * <pre>{@code
   * Item folder = client.items().createFolder(
   *     "fo-parent",
   *     request,
   *     true,
   *     false,
   *     RetryPolicy.retryOnServerError(1));
   * }</pre>
   *
   * @param parentId parent folder identifier
   * @param request folder creation payload
   * @param overwrite whether an existing folder may be overwritten
   * @param passthrough passthrough flag, or {@code null} to omit it
   * @param policy retry policy to use for the request
   * @return the created folder item
   */
  public Item createFolder(
      String parentId,
      FolderCreateRequest request,
      Boolean overwrite,
      Boolean passthrough,
      RetryPolicy policy) {
    Map<String, String> params = new LinkedHashMap<>();
    if (overwrite != null) {
      params.put("overwrite", overwrite.toString());
    }
    if (passthrough != null) {
      params.put("passthrough", passthrough.toString());
    }
    return executor.post(
        executor.uriWithParams(executor.entityActionUri(parentId, "Folder"), params),
        request,
        Item.class,
        policy);
  }

  /**
   * Creates a note under the given parent item.
   *
   * <pre>{@code
   * Item note = client.items().createNote("fo-parent", request);
   * }</pre>
   *
   * @param parentId parent folder identifier
   * @param request note creation payload
   * @return the created note item
   */
  public Item createNote(String parentId, NoteCreateRequest request) {
    return createNote(parentId, request, RetryPolicy.DEFAULT);
  }

  /**
   * Creates a note under the given parent item with an explicit retry policy.
   *
   * <pre>{@code
   * Item note = client.items().createNote("fo-parent", request, RetryPolicy.DEFAULT);
   * }</pre>
   *
   * @param parentId parent folder identifier
   * @param request note creation payload
   * @param policy retry policy to use for the request
   * @return the created note item
   */
  public Item createNote(String parentId, NoteCreateRequest request, RetryPolicy policy) {
    return executor.post(executor.entityActionUri(parentId, "Note"), request, Item.class, policy);
  }

  /**
   * Creates a link under the given parent item.
   *
   * <pre>{@code
   * Item link = client.items().createLink("fo-parent", request);
   * }</pre>
   *
   * @param parentId parent folder identifier
   * @param request link creation payload
   * @return the created link item
   */
  public Item createLink(String parentId, LinkCreateRequest request) {
    return createLink(parentId, request, RetryPolicy.DEFAULT);
  }

  /**
   * Creates a link under the given parent item with an explicit retry policy.
   *
   * <pre>{@code
   * Item link = client.items().createLink("fo-parent", request, RetryPolicy.DEFAULT);
   * }</pre>
   *
   * @param parentId parent folder identifier
   * @param request link creation payload
   * @param policy retry policy to use for the request
   * @return the created link item
   */
  public Item createLink(String parentId, LinkCreateRequest request, RetryPolicy policy) {
    return executor.post(executor.entityActionUri(parentId, "Link"), request, Item.class, policy);
  }

  /**
   * Updates an item and returns either the completed entity or an async-operation wrapper.
   *
   * <pre>{@code
   * OperationResult<Item> result = client.items().update("fi123", itemPatch);
   * }</pre>
   *
   * @param id item identifier
   * @param item partial item payload to send
   * @return operation result wrapping either a completed item or a pending async operation
   */
  public OperationResult<Item> update(String id, Item item) {
    return update(id, item, RetryPolicy.DEFAULT);
  }

  /**
   * Updates an item with an explicit retry policy.
   *
   * <pre>{@code
   * OperationResult<Item> result = client.items().update(
   *     "fi123",
   *     itemPatch,
   *     RetryPolicy.retryOnServerError(1));
   * }</pre>
   *
   * @param id item identifier
   * @param item partial item payload to send
   * @param policy retry policy to use for the request
   * @return operation result wrapping either a completed item or a pending async operation
   */
  public OperationResult<Item> update(String id, Item item, RetryPolicy policy) {
    return executor.patchOperationResult(executor.entityUri(id), item, Item.class, policy);
  }

  /**
   * Copies an item into a target folder.
   *
   * <pre>{@code
   * OperationResult<Item> result = client.items().copy("fi123", "fo-target", true);
   * }</pre>
   *
   * @param id source item identifier
   * @param targetFolderId target folder identifier
   * @param overwrite whether an existing target item may be overwritten
   * @return operation result wrapping either a completed item or a pending async operation
   */
  public OperationResult<Item> copy(String id, String targetFolderId, boolean overwrite) {
    return copy(id, targetFolderId, overwrite, RetryPolicy.DEFAULT);
  }

  /**
   * Copies an item into a target folder with an explicit retry policy.
   *
   * <pre>{@code
   * OperationResult<Item> result = client.items().copy(
   *     "fi123",
   *     "fo-target",
   *     true,
   *     RetryPolicy.retryOnServerError(1));
   * }</pre>
   *
   * @param id source item identifier
   * @param targetFolderId target folder identifier
   * @param overwrite whether an existing target item may be overwritten
   * @param policy retry policy to use for the request
   * @return operation result wrapping either a completed item or a pending async operation
   */
  public OperationResult<Item> copy(
      String id, String targetFolderId, boolean overwrite, RetryPolicy policy) {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("targetid", targetFolderId);
    params.put("overwrite", Boolean.toString(overwrite));
    URI uri = executor.uriWithParams(executor.entityActionUri(id, "Copy"), params);
    return executor.postOperationResult(uri, null, Item.class, policy);
  }

  /**
   * Deletes an item by ID.
   *
   * <pre>{@code
   * client.items().delete("fi123");
   * }</pre>
   *
   * @param id item identifier
   */
  public void delete(String id) {
    executor.delete(executor.entityUri(id));
  }

  /**
   * Bulk-deletes items under a parent item.
   *
   * <pre>{@code
   * client.items().bulkDelete("fo-parent", List.of("fi1", "fi2"), true);
   * }</pre>
   *
   * @param parentId parent item identifier
   * @param itemIds item identifiers to delete
   * @param permanently whether the delete should be permanent
   */
  public void bulkDelete(String parentId, List<String> itemIds, boolean permanently) {
    bulkDelete(parentId, itemIds, permanently, RetryPolicy.DEFAULT);
  }

  /**
   * Bulk-deletes items under a parent item with an explicit retry policy.
   *
   * <pre>{@code
   * client.items().bulkDelete(
   *     "fo-parent",
   *     List.of("fi1", "fi2"),
   *     true,
   *     RetryPolicy.retryOnServerError(1));
   * }</pre>
   *
   * @param parentId parent item identifier
   * @param itemIds item identifiers to delete
   * @param permanently whether the delete should be permanent
   * @param policy retry policy to use for the request
   */
  public void bulkDelete(
      String parentId, List<String> itemIds, boolean permanently, RetryPolicy policy) {
    URI uri = executor.entityActionUri(parentId, "BulkDelete");
    if (permanently) {
      uri = executor.uriWithParams(uri, Map.of("deletePermanently", Boolean.TRUE.toString()));
    }
    executor.post(uri, itemIds, Void.class, policy);
  }

  /**
   * Restores previously deleted items.
   *
   * <pre>{@code
   * client.items().bulkRestore(List.of("fi1", "fi2"));
   * }</pre>
   *
   * @param itemIds item identifiers to restore
   */
  public void bulkRestore(List<String> itemIds) {
    bulkRestore(itemIds, RetryPolicy.DEFAULT);
  }

  /**
   * Restores previously deleted items with an explicit retry policy.
   *
   * <pre>{@code
   * client.items().bulkRestore(List.of("fi1", "fi2"), RetryPolicy.retryOnServerError(1));
   * }</pre>
   *
   * @param itemIds item identifiers to restore
   * @param policy retry policy to use for the request
   */
  public void bulkRestore(List<String> itemIds, RetryPolicy policy) {
    BulkRestoreRequest request = new BulkRestoreRequest();
    request.setItemIds(itemIds);
    executor.post(executor.collectionActionUri("BulkRestore"), request, Void.class, policy);
  }

  /**
   * Checks out a file.
   *
   * <pre>{@code
   * Item checkedOut = client.items().checkOut("fi123");
   * }</pre>
   *
   * @param id file identifier
   * @return the checked-out item
   */
  public Item checkOut(String id) {
    return checkOut(id, RetryPolicy.DEFAULT);
  }

  /**
   * Checks out a file with an explicit retry policy.
   *
   * <pre>{@code
   * Item checkedOut = client.items().checkOut("fi123", RetryPolicy.DEFAULT);
   * }</pre>
   *
   * @param id file identifier
   * @param policy retry policy to use for the request
   * @return the checked-out item
   */
  public Item checkOut(String id, RetryPolicy policy) {
    return executor.post(executor.entityActionUri(id, "CheckOut"), null, Item.class, policy);
  }

  /**
   * Checks in a previously checked-out file.
   *
   * <pre>{@code
   * Item checkedIn = client.items().checkIn("fi123", "Done editing");
   * }</pre>
   *
   * @param id file identifier
   * @param message optional check-in comment
   * @return the checked-in item
   */
  public Item checkIn(String id, String message) {
    return checkIn(id, message, RetryPolicy.DEFAULT);
  }

  /**
   * Checks in a previously checked-out file with an explicit retry policy.
   *
   * <pre>{@code
   * Item checkedIn = client.items().checkIn(
   *     "fi123",
   *     "Done editing",
   *     RetryPolicy.DEFAULT);
   * }</pre>
   *
   * @param id file identifier
   * @param message optional check-in comment
   * @param policy retry policy to use for the request
   * @return the checked-in item
   */
  public Item checkIn(String id, String message, RetryPolicy policy) {
    CheckInRequest request = new CheckInRequest();
    request.setComment(message);
    return executor.post(executor.entityActionUri(id, "CheckIn"), request, Item.class, policy);
  }

  /**
   * Discards a file check-out.
   *
   * <pre>{@code
   * client.items().discardCheckOut("fi123");
   * }</pre>
   *
   * @param id file identifier
   */
  public void discardCheckOut(String id) {
    discardCheckOut(id, RetryPolicy.DEFAULT);
  }

  /**
   * Discards a file check-out with an explicit retry policy.
   *
   * <pre>{@code
   * client.items().discardCheckOut("fi123", RetryPolicy.DEFAULT);
   * }</pre>
   *
   * @param id file identifier
   * @param policy retry policy to use for the request
   */
  public void discardCheckOut(String id, RetryPolicy policy) {
    executor.post(executor.entityActionUri(id, "DiscardCheckOut"), null, Void.class, policy);
  }

  private static void requireQueryParam(ItemQuery query, String name, String endpoint) {
    Objects.requireNonNull(query, "query must not be null");
    String value = query.getParam(name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(endpoint + " requires query parameter: " + name);
    }
  }
}
