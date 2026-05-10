package io.github.indraftapp.sharefile.client;

import io.github.indraftapp.sharefile.client.auth.TokenManager;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.core.model.HealthStatus;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Main public entry point for the ShareFile Java SDK. */
public final class ShareFileClient implements AutoCloseable {

  private final ItemsClient itemsClient;
  private final UsersClient usersClient;
  private final SharesClient sharesClient;
  private final AccessControlsClient accessControlsClient;
  private final AsyncOperationsClient asyncOperationsClient;
  private final TransferClient transferClient;
  private final GroupsClient groupsClient;
  private final AccountsClient accountsClient;
  private final ZonesClient zonesClient;
  private final WebhookSubscriptionsClient webhookSubscriptionsClient;
  private final SessionsClient sessionsClient;
  private final TokenManager tokenManager;
  private final ExecutorService executor;
  private final boolean ownsExecutor;
  private final HttpTransport transport;
  private final boolean ownsTransport;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  ShareFileClient(
      ItemsClient itemsClient,
      UsersClient usersClient,
      SharesClient sharesClient,
      AccessControlsClient accessControlsClient,
      AsyncOperationsClient asyncOperationsClient,
      TransferClient transferClient,
      GroupsClient groupsClient,
      AccountsClient accountsClient,
      ZonesClient zonesClient,
      WebhookSubscriptionsClient webhookSubscriptionsClient,
      SessionsClient sessionsClient,
      TokenManager tokenManager,
      ExecutorService executor,
      boolean ownsExecutor,
      HttpTransport transport,
      boolean ownsTransport) {
    this.itemsClient = Objects.requireNonNull(itemsClient, "itemsClient must not be null");
    this.usersClient = Objects.requireNonNull(usersClient, "usersClient must not be null");
    this.sharesClient = Objects.requireNonNull(sharesClient, "sharesClient must not be null");
    this.accessControlsClient =
        Objects.requireNonNull(accessControlsClient, "accessControlsClient must not be null");
    this.asyncOperationsClient =
        Objects.requireNonNull(asyncOperationsClient, "asyncOperationsClient must not be null");
    this.transferClient = Objects.requireNonNull(transferClient, "transferClient must not be null");
    this.groupsClient = Objects.requireNonNull(groupsClient, "groupsClient must not be null");
    this.accountsClient = Objects.requireNonNull(accountsClient, "accountsClient must not be null");
    this.zonesClient = Objects.requireNonNull(zonesClient, "zonesClient must not be null");
    this.webhookSubscriptionsClient =
        Objects.requireNonNull(
            webhookSubscriptionsClient, "webhookSubscriptionsClient must not be null");
    this.sessionsClient = Objects.requireNonNull(sessionsClient, "sessionsClient must not be null");
    this.tokenManager = Objects.requireNonNull(tokenManager, "tokenManager must not be null");
    this.executor = Objects.requireNonNull(executor, "executor must not be null");
    this.ownsExecutor = ownsExecutor;
    this.transport = Objects.requireNonNull(transport, "transport must not be null");
    this.ownsTransport = ownsTransport;
  }

  /** Creates a new fluent builder for {@link ShareFileClient}. */
  public static ShareFileClientBuilder builder() {
    return new ShareFileClientBuilder();
  }

  /**
   * Returns the Items resource client.
   *
   * <pre>{@code
   * Item item = client.items().getById("home");
   * }</pre>
   *
   * @return items client
   */
  public ItemsClient items() {
    return itemsClient;
  }

  /**
   * Returns the Users resource client.
   *
   * <pre>{@code
   * User current = client.users().getCurrentUser();
   * }</pre>
   *
   * @return users client
   */
  public UsersClient users() {
    return usersClient;
  }

  /**
   * Returns the Shares resource client.
   *
   * <pre>{@code
   * ODataFeed<Share> shares = client.shares().list();
   * }</pre>
   *
   * @return shares client
   */
  public SharesClient shares() {
    return sharesClient;
  }

  /**
   * Returns the Groups resource client.
   *
   * <pre>{@code
   * ODataFeed<Group> groups = client.groups().list();
   * }</pre>
   *
   * @return groups client
   */
  public GroupsClient groups() {
    return groupsClient;
  }

  /**
   * Returns the Accounts resource client.
   *
   * <pre>{@code
   * Account account = client.accounts().get();
   * }</pre>
   *
   * @return accounts client
   */
  public AccountsClient accounts() {
    return accountsClient;
  }

  /**
   * Returns the AccessControls resource client.
   *
   * <pre>{@code
   * ODataFeed<AccessControl> acls = client.accessControls().getByItem("fo123");
   * }</pre>
   *
   * @return access-controls client
   */
  public AccessControlsClient accessControls() {
    return accessControlsClient;
  }

  /**
   * Returns the Zones resource client placeholder.
   *
   * <pre>{@code
   * ZonesClient zones = client.zones();
   * }</pre>
   *
   * @return zones client
   */
  public ZonesClient zones() {
    return zonesClient;
  }

  /**
   * Returns the WebhookSubscriptions resource client.
   *
   * <pre>{@code
   * ODataFeed<WebhookSubscription> hooks = client.webhookSubscriptions().list();
   * }</pre>
   *
   * @return webhook subscriptions client
   */
  public WebhookSubscriptionsClient webhookSubscriptions() {
    return webhookSubscriptionsClient;
  }

  /**
   * Returns the Sessions resource client placeholder.
   *
   * <pre>{@code
   * SessionsClient sessions = client.sessions();
   * }</pre>
   *
   * @return sessions client
   */
  public SessionsClient sessions() {
    return sessionsClient;
  }

  /**
   * Returns the AsyncOperations resource client.
   *
   * <pre>{@code
   * AsyncOperation operation = client.asyncOperations().getById("op-123");
   * }</pre>
   *
   * @return async-operations client
   */
  public AsyncOperationsClient asyncOperations() {
    return asyncOperationsClient;
  }

  /**
   * Returns the Transfer resource client.
   *
   * <pre>{@code
   * DownloadSpecification spec = client.transfers().resolveDownloadUrl("fi123");
   * }</pre>
   *
   * @return transfer client
   */
  public TransferClient transfers() {
    return transferClient;
  }

  /**
   * Returns a lightweight health snapshot based on account lookup and token state.
   *
   * <pre>{@code
   * HealthStatus status = client.checkHealth();
   * }</pre>
   *
   * @return current SDK health status
   */
  public HealthStatus checkHealth() {
    try {
      return HealthStatus.up(
          accountsClient.get().getSubdomain(), tokenManager.getSecondsUntilExpiry());
    } catch (Exception e) {
      return HealthStatus.down(e);
    }
  }

  /**
   * Closes SDK-owned resources such as the token manager and, when applicable, the SDK-managed
   * executor and transport.
   *
   * <pre>{@code
   * try (ShareFileClient client = ShareFileClient.builder() ... .build()) {
   *   // use client
   * }
   * }</pre>
   */
  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) {
      return;
    }

    RuntimeException closeFailure = null;
    try {
      tokenManager.close();
    } catch (RuntimeException e) {
      closeFailure = e;
    }

    if (ownsExecutor) {
      try {
        shutdownExecutor();
      } catch (RuntimeException e) {
        if (closeFailure == null) {
          closeFailure = e;
        } else {
          closeFailure.addSuppressed(e);
        }
      }
    }

    if (ownsTransport && transport instanceof AutoCloseable closeable) {
      try {
        closeable.close();
      } catch (Exception e) {
        RuntimeException wrapped =
            new IllegalStateException("Failed to close SDK-owned HttpTransport", e);
        if (closeFailure == null) {
          closeFailure = wrapped;
        } else {
          closeFailure.addSuppressed(wrapped);
        }
      }
    }

    if (closeFailure != null) {
      throw closeFailure;
    }
  }

  private void shutdownExecutor() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while shutting down ShareFile executor", e);
    }
  }
}
