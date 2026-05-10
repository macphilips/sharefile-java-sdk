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

  public User getById(String id) {
    return getById(id, ODataQuery.empty());
  }

  public User getById(String id, ODataQuery query) {
    return executor.get(executor.entityUri(id), query, User.class);
  }

  public ODataFeed<User> list() {
    return list(ODataQuery.empty());
  }

  public ODataFeed<User> list(ODataQuery query) {
    return executor.getCollection(executor.collectionUri(), query, USER_FEED_TYPE);
  }

  public User getCurrentUser() {
    return executor.get(executor.collectionUri(), ODataQuery.empty(), User.class);
  }

  public User getByEmail(String email) {
    ODataFeed<User> feed =
        list(ODataQuery.builder().filter(Filter.eq("Email", email)).top(1).build());
    if (feed.getItems().isEmpty()) {
      throw new ShareFileNotFoundException(
          "NotFound", "No ShareFile user found for email: " + email, null, "GET", "/Users");
    }
    return feed.getItems().get(0);
  }

  public User create(User user) {
    return executor.post(executor.collectionUri(), user, User.class);
  }

  public User update(String id, User user) {
    return executor.patch(executor.entityUri(id), user, User.class);
  }

  public void delete(String id) {
    executor.delete(executor.entityUri(id));
  }

  public UserPreferences getPreferences(String id) {
    return executor.get(
        executor.entityActionUri(id, "Preferences"), ODataQuery.empty(), UserPreferences.class);
  }

  public UserPreferences updatePreferences(String id, UserPreferences preferences) {
    return executor.patch(
        executor.entityActionUri(id, "Preferences"), preferences, UserPreferences.class);
  }

  public UserSecurity getSecurity(String id) {
    return executor.get(
        executor.entityActionUri(id, "Security"), ODataQuery.empty(), UserSecurity.class);
  }

  public UserSecurity updateSecurity(String id, UserSecurity security) {
    return executor.patch(executor.entityActionUri(id, "Security"), security, UserSecurity.class);
  }

  public void resetPassword(String id) {
    executor.post(executor.entityActionUri(id, "ResetPassword"), null, Void.class);
  }

  public void sendWelcomeEmail(String id) {
    executor.post(executor.entityActionUri(id, "ResendWelcome"), null, Void.class);
  }

  public ODataFeed<Group> getGroups(String userId) {
    return executor.getCollection(
        executor.entityActionUri(userId, "Groups"), ODataQuery.empty(), GROUP_FEED_TYPE);
  }

  public ODataFeed<Item> getAllSharedFolders() {
    return executor.getCollection(
        executor.entityActionUri("AllSharedFolders"), ODataQuery.empty(), ITEM_FEED_TYPE);
  }
}
