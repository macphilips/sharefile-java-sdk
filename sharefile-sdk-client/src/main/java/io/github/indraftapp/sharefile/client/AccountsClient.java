package io.github.indraftapp.sharefile.client;

import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.core.model.Account;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.util.Objects;

/**
 * Minimal ShareFile resource client for `/Accounts`.
 *
 * <p>SF-14 only exposes the current-account lookup needed for {@link
 * ShareFileClient#checkHealth()}. Broader Accounts behavior is deferred to later tickets.
 */
public final class AccountsClient {

  private final ResourceRequestExecutor executor;

  AccountsClient(ResourceRequestExecutor executor) {
    this.executor = Objects.requireNonNull(executor, "executor must not be null");
  }

  AccountsClient(ShareFileHttpClient httpClient) {
    this(new ResourceRequestExecutor(httpClient, "/Accounts"));
  }

  /** Returns the current ShareFile account. */
  public Account get() {
    return executor.get(executor.collectionUri(), ODataQuery.empty(), Account.class);
  }
}
