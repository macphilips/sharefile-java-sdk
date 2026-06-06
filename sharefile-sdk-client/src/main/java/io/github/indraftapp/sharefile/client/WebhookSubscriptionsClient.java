package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.WebhookSubscription;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.util.Objects;

/** Explicit ShareFile resource client for `/WebhookSubscriptions` endpoints. */
public final class WebhookSubscriptionsClient {

  private static final TypeReference<ODataFeed<WebhookSubscription>> WEBHOOK_FEED_TYPE =
      new TypeReference<>() {};

  private final ResourceRequestExecutor executor;

  WebhookSubscriptionsClient(ResourceRequestExecutor executor) {
    this.executor = Objects.requireNonNull(executor, "executor must not be null");
  }

  WebhookSubscriptionsClient(ShareFileHttpClient httpClient) {
    this(new ResourceRequestExecutor(httpClient, "/WebhookSubscriptions"));
  }

  /**
   * Retrieves a webhook subscription by identifier.
   *
   * <pre>{@code
   * WebhookSubscription subscription = client.webhookSubscriptions().getById("sub-1");
   * }</pre>
   *
   * @param id subscription identifier
   * @return resolved subscription
   */
  public WebhookSubscription getById(String id) {
    return executor.get(executor.entityUri(id), ODataQuery.empty(), WebhookSubscription.class);
  }

  /**
   * Lists webhook subscriptions with no additional query options.
   *
   * <pre>{@code
   * ODataFeed<WebhookSubscription> feed = client.webhookSubscriptions().list();
   * }</pre>
   *
   * @return feed of webhook subscriptions
   */
  public ODataFeed<WebhookSubscription> list() {
    return list(ODataQuery.empty());
  }

  /**
   * Lists webhook subscriptions using OData query options.
   *
   * @param query OData query options
   * @return feed of webhook subscriptions
   */
  public ODataFeed<WebhookSubscription> list(ODataQuery query) {
    return executor.getCollection(executor.collectionUri(), query, WEBHOOK_FEED_TYPE);
  }

  /**
   * Creates a webhook subscription with the default retry policy.
   *
   * <pre>{@code
   * WebhookSubscription created = client.webhookSubscriptions().create(subscription);
   * }</pre>
   *
   * @param subscription subscription payload
   * @return created subscription
   */
  public WebhookSubscription create(WebhookSubscription subscription) {
    return create(subscription, RetryPolicy.DEFAULT);
  }

  /**
   * Creates a webhook subscription with an explicit retry policy.
   *
   * @param subscription subscription payload
   * @param policy retry policy override
   * @return created subscription
   */
  public WebhookSubscription create(WebhookSubscription subscription, RetryPolicy policy) {
    return executor.post(executor.collectionUri(), subscription, WebhookSubscription.class, policy);
  }

  /**
   * Deletes a webhook subscription.
   *
   * <pre>{@code
   * client.webhookSubscriptions().delete("sub-1");
   * }</pre>
   *
   * @param id subscription identifier
   */
  public void delete(String id) {
    executor.delete(executor.entityUri(id));
  }
}
