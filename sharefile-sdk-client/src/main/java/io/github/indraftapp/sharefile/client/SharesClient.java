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

  public Share getById(String id) {
    return getById(id, ODataQuery.empty());
  }

  public Share getById(String id, ODataQuery query) {
    return executor.get(executor.entityUri(id), query, Share.class);
  }

  public ODataFeed<Share> list() {
    return list(ODataQuery.empty());
  }

  public ODataFeed<Share> list(ODataQuery query) {
    return executor.getCollection(executor.collectionUri(), query, SHARE_FEED_TYPE);
  }

  public Share createSendShare(SendShareRequest request) {
    return executor.post(executor.collectionUri(), request, Share.class);
  }

  public Share createRequestShare(RequestShareRequest request) {
    return executor.post(executor.collectionUri(), request, Share.class);
  }

  public Share update(String id, Share share) {
    return executor.patch(executor.entityUri(id), share, Share.class);
  }

  public void delete(String id) {
    executor.delete(executor.entityUri(id));
  }

  public ODataFeed<Share> getByUser(String userId) {
    return executor.getCollection(
        executor.relativeUri("/Users(" + userId + ")/Shares"), ODataQuery.empty(), SHARE_FEED_TYPE);
  }

  public ODataFeed<Contact> getRecipients(String shareId) {
    return executor.getCollection(
        executor.entityActionUri(shareId, "Recipients"), ODataQuery.empty(), CONTACT_FEED_TYPE);
  }

  public void sendNotification(String shareId, ShareNotificationRequest request) {
    executor.post(executor.entityActionUri(shareId, "Notify"), request, Void.class);
  }

  public ODataFeed<Item> getItems(String shareId) {
    return executor.getCollection(
        executor.entityActionUri(shareId, "Items"), ODataQuery.empty(), ITEM_FEED_TYPE);
  }

  public DownloadSpecification downloadItems(String shareId) {
    return executor.get(
        executor.uriWithParams(
            executor.entityActionUri(shareId, "Download"), Map.of("redirect", "false")),
        ODataQuery.empty(),
        DownloadSpecification.class);
  }
}
