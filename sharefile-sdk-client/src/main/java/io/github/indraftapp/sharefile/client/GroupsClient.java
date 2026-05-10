package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.model.Contact;
import io.github.indraftapp.sharefile.core.model.Group;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.net.URI;
import java.util.Objects;

/** Explicit ShareFile resource client for `/Groups` endpoints. */
public final class GroupsClient {

  private static final TypeReference<ODataFeed<Group>> GROUP_FEED_TYPE = new TypeReference<>() {};
  private static final TypeReference<ODataFeed<Contact>> CONTACT_FEED_TYPE =
      new TypeReference<>() {};

  private final ResourceRequestExecutor executor;

  GroupsClient(ResourceRequestExecutor executor) {
    this.executor = Objects.requireNonNull(executor, "executor must not be null");
  }

  GroupsClient(ShareFileHttpClient httpClient) {
    this(new ResourceRequestExecutor(httpClient, "/Groups"));
  }

  /**
   * Retrieves a group by identifier.
   *
   * <pre>{@code
   * Group group = client.groups().getById("group-1");
   * }</pre>
   *
   * @param id group identifier
   * @return the resolved group
   */
  public Group getById(String id) {
    return executor.get(executor.entityUri(id), ODataQuery.empty(), Group.class);
  }

  /**
   * Lists groups with no additional query options.
   *
   * <pre>{@code
   * ODataFeed<Group> groups = client.groups().list();
   * }</pre>
   *
   * @return feed of groups
   */
  public ODataFeed<Group> list() {
    return list(ODataQuery.empty());
  }

  /**
   * Lists groups using OData query options.
   *
   * <pre>{@code
   * ODataFeed<Group> groups = client.groups().list(ODataQuery.builder().top(25).build());
   * }</pre>
   *
   * @param query OData query options
   * @return feed of groups
   */
  public ODataFeed<Group> list(ODataQuery query) {
    return executor.getCollection(executor.collectionUri(), query, GROUP_FEED_TYPE);
  }

  /**
   * Creates a group with the default retry policy.
   *
   * <pre>{@code
   * Group created = client.groups().create(group);
   * }</pre>
   *
   * @param group group payload
   * @return created group
   */
  public Group create(Group group) {
    return create(group, RetryPolicy.DEFAULT);
  }

  /**
   * Creates a group with an explicit retry policy.
   *
   * @param group group payload
   * @param policy retry policy override
   * @return created group
   */
  public Group create(Group group, RetryPolicy policy) {
    return executor.post(executor.collectionUri(), group, Group.class, policy);
  }

  /**
   * Updates an existing group with the default retry policy.
   *
   * <pre>{@code
   * Group updated = client.groups().update("group-1", group);
   * }</pre>
   *
   * @param id group identifier
   * @param group partial or full group payload
   * @return updated group
   */
  public Group update(String id, Group group) {
    return update(id, group, RetryPolicy.DEFAULT);
  }

  /**
   * Updates an existing group with an explicit retry policy.
   *
   * @param id group identifier
   * @param group partial or full group payload
   * @param policy retry policy override
   * @return updated group
   */
  public Group update(String id, Group group, RetryPolicy policy) {
    return executor.patch(executor.entityUri(id), group, Group.class, policy);
  }

  /**
   * Deletes a group.
   *
   * <pre>{@code
   * client.groups().delete("group-1");
   * }</pre>
   *
   * @param id group identifier
   */
  public void delete(String id) {
    executor.delete(executor.entityUri(id));
  }

  /**
   * Lists the members of a group.
   *
   * <pre>{@code
   * ODataFeed<Contact> members = client.groups().getMembers("group-1");
   * }</pre>
   *
   * @param id group identifier
   * @return feed of group members
   */
  public ODataFeed<Contact> getMembers(String id) {
    return getMembers(id, ODataQuery.empty());
  }

  /**
   * Lists the members of a group using OData query options.
   *
   * <pre>{@code
   * ODataFeed<Contact> members =
   *     client.groups().getMembers("group-1", ODataQuery.builder().top(50).build());
   * }</pre>
   *
   * @param id group identifier
   * @param query OData query options
   * @return feed of group members
   */
  public ODataFeed<Contact> getMembers(String id, ODataQuery query) {
    return executor.getCollection(
        executor.entityActionUri(id, "Contacts"), query, CONTACT_FEED_TYPE);
  }

  /**
   * Adds a contact or user to a group with the default retry policy.
   *
   * <pre>{@code
   * Contact contact = client.groups().addMember("group-1", "user-1");
   * }</pre>
   *
   * @param groupId group identifier
   * @param userId contact or user identifier to add
   * @return created contact membership
   */
  public Contact addMember(String groupId, String userId) {
    return addMember(groupId, userId, RetryPolicy.DEFAULT);
  }

  /**
   * Adds a contact or user to a group with an explicit retry policy.
   *
   * @param groupId group identifier
   * @param userId contact or user identifier to add
   * @param policy retry policy override
   * @return created contact membership
   */
  public Contact addMember(String groupId, String userId, RetryPolicy policy) {
    Contact contact = new Contact();
    contact.setId(userId);
    return executor.post(
        executor.entityActionUri(groupId, "Contacts"), contact, Contact.class, policy);
  }

  /**
   * Removes a contact or user from a group.
   *
   * <pre>{@code
   * client.groups().removeMember("group-1", "user-1");
   * }</pre>
   *
   * @param groupId group identifier
   * @param userId contact or user identifier to remove
   */
  public void removeMember(String groupId, String userId) {
    URI memberUri = executor.relativeUri("/Groups(%s)/Contacts(%s)".formatted(groupId, userId));
    executor.delete(memberUri);
  }
}
