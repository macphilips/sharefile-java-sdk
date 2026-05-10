package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.core.model.Contact;
import io.github.indraftapp.sharefile.core.model.Item;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.Share;
import io.github.indraftapp.sharefile.core.model.request.RequestShareRequest;
import io.github.indraftapp.sharefile.core.model.request.SendShareRequest;
import io.github.indraftapp.sharefile.core.model.request.ShareNotificationRequest;
import io.github.indraftapp.sharefile.core.model.response.DownloadSpecification;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.util.Map;
import java.util.Objects;

/** Explicit ShareFile resource client for `/Shares` endpoints. */
public final class SharesClient {

  private static final TypeReference<ODataFeed<Share>> SHARE_FEED_TYPE = new TypeReference<>() {};
  private static final TypeReference<ODataFeed<Contact>> CONTACT_FEED_TYPE =
      new TypeReference<>() {};
  private static final TypeReference<ODataFeed<Item>> ITEM_FEED_TYPE = new TypeReference<>() {};

  private final ResourceRequestExecutor executor;

  SharesClient(ResourceRequestExecutor executor) {
    this.executor = Objects.requireNonNull(executor, "executor must not be null");
  }

  SharesClient(ShareFileHttpClient httpClient) {
    this(new ResourceRequestExecutor(httpClient, "/Shares"));
  }

  /**
   * Retrieves a share by identifier with no additional query options.
   *
   * <pre>{@code
   * Share share = client.shares().getById("share-1");
   * }</pre>
   *
   * @param id share identifier
   * @return resolved share
   */
  public Share getById(String id) {
    return getById(id, ODataQuery.empty());
  }

  /**
   * Retrieves a share by identifier using OData query options.
   *
   * <pre>{@code
   * Share share =
   *     client.shares().getById("share-1", ODataQuery.builder().expand("Items").build());
   * }</pre>
   *
   * @param id share identifier
   * @param query OData query options
   * @return resolved share
   */
  public Share getById(String id, ODataQuery query) {
    return executor.get(executor.entityUri(id), query, Share.class);
  }

  /**
   * Lists shares with no additional query options.
   *
   * <pre>{@code
   * ODataFeed<Share> shares = client.shares().list();
   * }</pre>
   *
   * @return feed of shares
   */
  public ODataFeed<Share> list() {
    return list(ODataQuery.empty());
  }

  /**
   * Lists shares using OData query options.
   *
   * @param query OData query options
   * @return feed of shares
   */
  public ODataFeed<Share> list(ODataQuery query) {
    return executor.getCollection(executor.collectionUri(), query, SHARE_FEED_TYPE);
  }

  /**
   * Creates a send-style share.
   *
   * <pre>{@code
   * Share share = client.shares().createSendShare(request);
   * }</pre>
   *
   * @param request send share request payload
   * @return created share
   */
  public Share createSendShare(SendShareRequest request) {
    return executor.post(executor.collectionUri(), request, Share.class);
  }

  /**
   * Creates a request-style share.
   *
   * <pre>{@code
   * Share share = client.shares().createRequestShare(request);
   * }</pre>
   *
   * @param request request share payload
   * @return created share
   */
  public Share createRequestShare(RequestShareRequest request) {
    return executor.post(executor.collectionUri(), request, Share.class);
  }

  /**
   * Updates an existing share.
   *
   * <pre>{@code
   * Share updated = client.shares().update("share-1", share);
   * }</pre>
   *
   * @param id share identifier
   * @param share partial or full share payload
   * @return updated share
   */
  public Share update(String id, Share share) {
    return executor.patch(executor.entityUri(id), share, Share.class);
  }

  /**
   * Deletes a share.
   *
   * <pre>{@code
   * client.shares().delete("share-1");
   * }</pre>
   *
   * @param id share identifier
   */
  public void delete(String id) {
    executor.delete(executor.entityUri(id));
  }

  /**
   * Lists shares owned by or associated with a user.
   *
   * <pre>{@code
   * ODataFeed<Share> shares = client.shares().getByUser("user-1");
   * }</pre>
   *
   * @param userId user identifier
   * @return feed of shares for the user
   */
  public ODataFeed<Share> getByUser(String userId) {
    return executor.getCollection(
        executor.relativeUri("/Users(" + userId + ")/Shares"), ODataQuery.empty(), SHARE_FEED_TYPE);
  }

  /**
   * Lists the recipients attached to a share.
   *
   * <pre>{@code
   * ODataFeed<Contact> recipients = client.shares().getRecipients("share-1");
   * }</pre>
   *
   * @param shareId share identifier
   * @return feed of recipients
   */
  public ODataFeed<Contact> getRecipients(String shareId) {
    return executor.getCollection(
        executor.entityActionUri(shareId, "Recipients"), ODataQuery.empty(), CONTACT_FEED_TYPE);
  }

  /**
   * Sends a notification for an existing share.
   *
   * <pre>{@code
   * client.shares().sendNotification("share-1", request);
   * }</pre>
   *
   * @param shareId share identifier
   * @param request notification request payload
   */
  public void sendNotification(String shareId, ShareNotificationRequest request) {
    executor.post(executor.entityActionUri(shareId, "Notify"), request, Void.class);
  }

  /**
   * Lists the items attached to a share.
   *
   * <pre>{@code
   * ODataFeed<Item> items = client.shares().getItems("share-1");
   * }</pre>
   *
   * @param shareId share identifier
   * @return feed of items in the share
   */
  public ODataFeed<Item> getItems(String shareId) {
    return executor.getCollection(
        executor.entityActionUri(shareId, "Items"), ODataQuery.empty(), ITEM_FEED_TYPE);
  }

  /**
   * Resolves the download specification for all items in a share.
   *
   * <pre>{@code
   * DownloadSpecification spec = client.shares().downloadItems("share-1");
   * }</pre>
   *
   * @param shareId share identifier
   * @return download specification with resolved URL information
   */
  public DownloadSpecification downloadItems(String shareId) {
    return executor.get(
        executor.uriWithParams(
            executor.entityActionUri(shareId, "Download"), Map.of("redirect", "false")),
        ODataQuery.empty(),
        DownloadSpecification.class);
  }
}
