package io.github.indraftapp.sharefile.client;

import io.github.indraftapp.sharefile.core.model.enums.DlpStatus;
import io.github.indraftapp.sharefile.core.model.enums.ItemOrderingMode;
import io.github.indraftapp.sharefile.core.model.enums.TreeMode;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Item-specific query model that merges OData and ShareFile non-OData parameters. */
public final class ItemQuery {

  private final ODataQuery odataQuery;
  private final Map<String, String> params;

  private ItemQuery(Builder builder) {
    this.odataQuery = builder.odataQuery;
    this.params = Collections.unmodifiableMap(new LinkedHashMap<>(builder.params));
  }

  /** Creates a new item-query builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Returns an empty item query. */
  public static ItemQuery empty() {
    return builder().build();
  }

  /** Returns all query parameters, with OData parameters first. */
  public Map<String, String> toQueryParams() {
    LinkedHashMap<String, String> merged = new LinkedHashMap<>();
    if (odataQuery != null && !odataQuery.isEmpty()) {
      merged.putAll(odataQuery.toQueryParams());
    }
    merged.putAll(params);
    return Collections.unmodifiableMap(merged);
  }

  /** Returns whether the query is empty. */
  public boolean isEmpty() {
    return (odataQuery == null || odataQuery.isEmpty()) && params.isEmpty();
  }

  String getParam(String name) {
    return params.get(name);
  }

  /** Mutable builder for {@link ItemQuery}. */
  public static final class Builder {

    private ODataQuery odataQuery = ODataQuery.empty();
    private final LinkedHashMap<String, String> params = new LinkedHashMap<>();

    private Builder() {}

    public Builder odata(ODataQuery odataQuery) {
      this.odataQuery = Objects.requireNonNull(odataQuery, "odataQuery must not be null");
      return this;
    }

    public Builder includeDeleted(Boolean includeDeleted) {
      return booleanParam("includeDeleted", includeDeleted);
    }

    public Builder orderingMode(ItemOrderingMode orderingMode) {
      return enumParam("orderingMode", orderingMode == null ? null : orderingMode.getValue());
    }

    public Builder treeMode(TreeMode treeMode) {
      return enumParam("treemode", treeMode == null ? null : treeMode.getValue());
    }

    public Builder sourceId(String sourceId) {
      return stringParam("sourceId", sourceId);
    }

    public Builder canCreateRootFolder(Boolean canCreateRootFolder) {
      return booleanParam("canCreateRootFolder", canCreateRootFolder);
    }

    public Builder fileBox(Boolean fileBox) {
      return booleanParam("fileBox", fileBox);
    }

    public Builder redirect(Boolean redirect) {
      return booleanParam("redirect", redirect);
    }

    public Builder includeAllVersions(Boolean includeAllVersions) {
      return booleanParam("includeAllVersions", includeAllVersions);
    }

    public Builder size(Integer size) {
      return intParam("size", size);
    }

    public Builder path(String path) {
      return stringParam("path", path);
    }

    public Builder query(String query) {
      return stringParam("query", query);
    }

    public Builder maxResults(Integer maxResults) {
      return intParam("maxResults", maxResults);
    }

    public Builder skip(Integer skip) {
      return intParam("skip", skip);
    }

    public Builder homeFolderOnly(Boolean homeFolderOnly) {
      return booleanParam("homeFolderOnly", homeFolderOnly);
    }

    public Builder userId(String userId) {
      return stringParam("userid", userId);
    }

    public Builder zone(String zone) {
      return stringParam("zone", zone);
    }

    public Builder status(DlpStatus status) {
      return enumParam("status", status == null ? null : status.getValue());
    }

    public Builder endDate(Instant endDate) {
      return stringParam("enddate", endDate == null ? null : endDate.toString());
    }

    public Builder param(String name, String value) {
      validateParamName(name);
      if (value == null) {
        params.remove(name);
      } else {
        params.put(name, value);
      }
      return this;
    }

    public ItemQuery build() {
      return new ItemQuery(this);
    }

    private Builder booleanParam(String name, Boolean value) {
      return param(name, value == null ? null : value.toString());
    }

    private Builder intParam(String name, Integer value) {
      return param(name, value == null ? null : value.toString());
    }

    private Builder stringParam(String name, String value) {
      return param(name, value);
    }

    private Builder enumParam(String name, String value) {
      return param(name, value);
    }

    private static void validateParamName(String name) {
      Objects.requireNonNull(name, "name must not be null");
      if (name.isBlank()) {
        throw new IllegalArgumentException("name must not be blank");
      }
      if (name.startsWith("$")) {
        throw new IllegalArgumentException(
            "Custom Item query params must not start with '$': " + name);
      }
    }
  }
}
