package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.core.exception.ShareFileNotFoundException;
import io.github.indraftapp.sharefile.core.model.Group;
import io.github.indraftapp.sharefile.core.model.Item;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.User;
import io.github.indraftapp.sharefile.core.model.response.UserPreferences;
import io.github.indraftapp.sharefile.core.model.response.UserSecurity;
import io.github.indraftapp.sharefile.core.odata.Filter;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.util.Objects;

/** Explicit ShareFile resource client for `/Users` endpoints. */
public final class UsersClient {

  private static final TypeReference<ODataFeed<User>> USER_FEED_TYPE = new TypeReference<>() {};
  private static final TypeReference<ODataFeed<Group>> GROUP_FEED_TYPE = new TypeReference<>() {};
  private static final TypeReference<ODataFeed<Item>> ITEM_FEED_TYPE = new TypeReference<>() {};

  private final ResourceRequestExecutor executor;

  UsersClient(ResourceRequestExecutor executor) {
    this.executor = Objects.requireNonNull(executor, "executor must not be null");
  }

  UsersClient(ShareFileHttpClient httpClient) {
    this(new ResourceRequestExecutor(httpClient, "/Users"));
  }

  /**
   * Retrieves a user by identifier with no additional query options.
   *
   * <pre>{@code
   * User user = client.users().getById("user-1");
   * }</pre>
   *
   * @param id user identifier
   * @return resolved user
   */
  public User getById(String id) {
    return getById(id, ODataQuery.empty());
  }

  /**
   * Retrieves a user by identifier using OData query options.
   *
   * <pre>{@code
   * User user = client.users().getById("user-1", ODataQuery.builder().expand("Preferences").build());
   * }</pre>
   *
   * @param id user identifier
   * @param query OData query options
   * @return resolved user
   */
  public User getById(String id, ODataQuery query) {
    return executor.get(executor.entityUri(id), query, User.class);
  }

  /**
   * Lists users with no additional query options.
   *
   * <pre>{@code
   * ODataFeed<User> users = client.users().list();
   * }</pre>
   *
   * @return feed of users
   */
  public ODataFeed<User> list() {
    return list(ODataQuery.empty());
  }

  /**
   * Lists users using OData query options.
   *
   * @param query OData query options
   * @return feed of users
   */
  public ODataFeed<User> list(ODataQuery query) {
    return executor.getCollection(executor.collectionUri(), query, USER_FEED_TYPE);
  }

  /**
   * Retrieves the currently authenticated user.
   *
   * <pre>{@code
   * User me = client.users().getCurrentUser();
   * }</pre>
   *
   * @return current authenticated user
   */
  public User getCurrentUser() {
    return executor.get(executor.collectionUri(), ODataQuery.empty(), User.class);
  }

  /**
   * Looks up a user by email address using a filtered users feed.
   *
   * <pre>{@code
   * User user = client.users().getByEmail("user@example.com");
   * }</pre>
   *
   * @param email user email address
   * @return first matching user
   * @throws ShareFileNotFoundException if no user matches the supplied email
   */
  public User getByEmail(String email) {
    ODataFeed<User> feed =
        list(ODataQuery.builder().filter(Filter.eq("Email", email)).top(1).build());
    if (feed.getItems().isEmpty()) {
      throw new ShareFileNotFoundException(
          "NotFound", "No ShareFile user found for email: " + email, null, "GET", "/Users");
    }
    return feed.getItems().get(0);
  }

  /**
   * Creates a new user.
   *
   * <pre>{@code
   * User created = client.users().create(user);
   * }</pre>
   *
   * @param user user payload
   * @return created user
   */
  public User create(User user) {
    return executor.post(executor.collectionUri(), user, User.class);
  }

  /**
   * Updates an existing user.
   *
   * <pre>{@code
   * User updated = client.users().update("user-1", user);
   * }</pre>
   *
   * @param id user identifier
   * @param user partial or full user payload
   * @return updated user
   */
  public User update(String id, User user) {
    return executor.patch(executor.entityUri(id), user, User.class);
  }

  /**
   * Deletes a user.
   *
   * <pre>{@code
   * client.users().delete("user-1");
   * }</pre>
   *
   * @param id user identifier
   */
  public void delete(String id) {
    executor.delete(executor.entityUri(id));
  }

  /**
   * Retrieves user preferences.
   *
   * <pre>{@code
   * UserPreferences prefs = client.users().getPreferences("user-1");
   * }</pre>
   *
   * @param id user identifier
   * @return user preferences
   */
  public UserPreferences getPreferences(String id) {
    return executor.get(
        executor.entityActionUri(id, "Preferences"), ODataQuery.empty(), UserPreferences.class);
  }

  /**
   * Updates user preferences.
   *
   * <pre>{@code
   * UserPreferences updated = client.users().updatePreferences("user-1", prefs);
   * }</pre>
   *
   * @param id user identifier
   * @param preferences updated preferences payload
   * @return updated user preferences
   */
  public UserPreferences updatePreferences(String id, UserPreferences preferences) {
    return executor.patch(
        executor.entityActionUri(id, "Preferences"), preferences, UserPreferences.class);
  }

  /**
   * Retrieves user security settings.
   *
   * <pre>{@code
   * UserSecurity security = client.users().getSecurity("user-1");
   * }</pre>
   *
   * @param id user identifier
   * @return user security settings
   */
  public UserSecurity getSecurity(String id) {
    return executor.get(
        executor.entityActionUri(id, "Security"), ODataQuery.empty(), UserSecurity.class);
  }

  /**
   * Updates user security settings.
   *
   * <pre>{@code
   * UserSecurity updated = client.users().updateSecurity("user-1", security);
   * }</pre>
   *
   * @param id user identifier
   * @param security updated security payload
   * @return updated user security settings
   */
  public UserSecurity updateSecurity(String id, UserSecurity security) {
    return executor.patch(executor.entityActionUri(id, "Security"), security, UserSecurity.class);
  }

  /**
   * Triggers a password reset for a user.
   *
   * <pre>{@code
   * client.users().resetPassword("user-1");
   * }</pre>
   *
   * @param id user identifier
   */
  public void resetPassword(String id) {
    executor.post(executor.entityActionUri(id, "ResetPassword"), null, Void.class);
  }

  /**
   * Resends the welcome email for a user.
   *
   * <pre>{@code
   * client.users().sendWelcomeEmail("user-1");
   * }</pre>
   *
   * @param id user identifier
   */
  public void sendWelcomeEmail(String id) {
    executor.post(executor.entityActionUri(id, "ResendWelcome"), null, Void.class);
  }

  /**
   * Lists the groups associated with a user.
   *
   * <pre>{@code
   * ODataFeed<Group> groups = client.users().getGroups("user-1");
   * }</pre>
   *
   * @param userId user identifier
   * @return feed of groups for the user
   */
  public ODataFeed<Group> getGroups(String userId) {
    return executor.getCollection(
        executor.entityActionUri(userId, "Groups"), ODataQuery.empty(), GROUP_FEED_TYPE);
  }

  /**
   * Lists all shared folders visible to the current user.
   *
   * <pre>{@code
   * ODataFeed<Item> folders = client.users().getAllSharedFolders();
   * }</pre>
   *
   * @return feed of all shared folders
   */
  public ODataFeed<Item> getAllSharedFolders() {
    return executor.getCollection(
        executor.entityActionUri("AllSharedFolders"), ODataQuery.empty(), ITEM_FEED_TYPE);
  }
}
