package io.github.indraftapp.sharefile.client;

import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.core.model.Session;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.util.Objects;

/** Explicit ShareFile resource client for `/Sessions` endpoints. */
public final class SessionsClient {

  private final ResourceRequestExecutor executor;

  SessionsClient(ResourceRequestExecutor executor) {
    this.executor = Objects.requireNonNull(executor, "executor must not be null");
  }

  SessionsClient(ShareFileHttpClient httpClient) {
    this(new ResourceRequestExecutor(httpClient, "/Sessions"));
  }

  /**
   * Retrieves the current session.
   *
   * <pre>{@code
   * Session session = client.sessions().get();
   * }</pre>
   *
   * @return current session
   */
  public Session get() {
    return executor.get(executor.collectionUri(), ODataQuery.empty(), Session.class);
  }

  /**
   * Creates or logs in a session.
   *
   * <pre>{@code
   * Session session = client.sessions().login(sessionPayload);
   * }</pre>
   *
   * @param session session payload
   * @return created session
   */
  public Session login(Session session) {
    return executor.post(executor.collectionUri(), session, Session.class);
  }

  /**
   * Logs out the current session.
   *
   * <pre>{@code
   * client.sessions().logout();
   * }</pre>
   */
  public void logout() {
    executor.delete(executor.collectionUri());
  }
}
