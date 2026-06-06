package io.github.indraftapp.sharefile.core.odata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of OData query options ({@code $select}, {@code $expand}, {@code
 * $filter}, {@code $orderby}, {@code $top}, {@code $skip}).
 *
 * <p>Create instances via the {@link #builder()} method:
 *
 * <pre>{@code
 * ODataQuery query = ODataQuery.builder()
 *     .select("Name", "Email")
 *     .expand("Parent")
 *     .filter(Filter.eq("Name", "Reports"))
 *     .orderBy("CreationDate", SortDirection.DESC)
 *     .top(100)
 *     .skip(0)
 *     .build();
 *
 * Map<String, String> params = query.toQueryParams();
 * }</pre>
 */
public final class ODataQuery {

  private final String select;
  private final String expand;
  private final String filter;
  private final String orderBy;
  private final Integer top;
  private final Integer skip;

  private ODataQuery(Builder builder) {
    this.select = builder.selectFields.isEmpty() ? null : String.join(",", builder.selectFields);
    this.expand = buildExpand(builder);
    this.filter = buildFilter(builder);
    this.orderBy =
        builder.orderByClauses.isEmpty() ? null : String.join(",", builder.orderByClauses);
    this.top = builder.top;
    this.skip = builder.skip;
  }

  /** Creates a new builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Returns an empty query with no parameters set. */
  public static ODataQuery empty() {
    return new Builder().build();
  }

  /**
   * Serializes this query to an unmodifiable map of OData query parameters.
   *
   * <p>Only parameters that have been set are included. Keys use the OData {@code $}-prefixed names
   * ({@code $select}, {@code $expand}, etc.).
   *
   * @return an unmodifiable map of query parameter name to value
   */
  public Map<String, String> toQueryParams() {
    var params = new LinkedHashMap<String, String>();
    if (select != null) {
      params.put("$select", select);
    }
    if (expand != null) {
      params.put("$expand", expand);
    }
    if (filter != null) {
      params.put("$filter", filter);
    }
    if (orderBy != null) {
      params.put("$orderby", orderBy);
    }
    if (top != null) {
      params.put("$top", top.toString());
    }
    if (skip != null) {
      params.put("$skip", skip.toString());
    }
    return Collections.unmodifiableMap(params);
  }

  /** Returns {@code true} if no query parameters are set. */
  public boolean isEmpty() {
    return select == null
        && expand == null
        && filter == null
        && orderBy == null
        && top == null
        && skip == null;
  }

  @Override
  public String toString() {
    return toQueryParams().toString();
  }

  // ── Internal ─────────────────────────────────────────────────────────

  private static String buildExpand(Builder builder) {
    if (builder.expandAll) {
      return "*";
    }
    return builder.expandFields.isEmpty() ? null : String.join(",", builder.expandFields);
  }

  private static String buildFilter(Builder builder) {
    if (builder.rawFilter != null) {
      return builder.rawFilter;
    }
    if (builder.typedFilter != null) {
      return builder.typedFilter.toExpression();
    }
    return null;
  }

  // ── Builder ──────────────────────────────────────────────────────────

  /** Mutable builder for {@link ODataQuery}. */
  public static final class Builder {

    private final List<String> selectFields = new ArrayList<>();
    private final List<String> expandFields = new ArrayList<>();
    private boolean expandAll;
    private String rawFilter;
    private Filter typedFilter;
    private final List<String> orderByClauses = new ArrayList<>();
    private Integer top;
    private Integer skip;

    private Builder() {}

    /**
     * Adds property names to the {@code $select} clause.
     *
     * @param fields one or more PascalCase property names
     * @return this builder
     */
    public Builder select(String... fields) {
      Objects.requireNonNull(fields, "fields must not be null");
      for (String f : fields) {
        Objects.requireNonNull(f, "field must not be null");
        selectFields.add(f);
      }
      return this;
    }

    /**
     * Adds navigation property names to the {@code $expand} clause.
     *
     * <p>Ignored if {@link #expandAll()} has been called.
     *
     * @param fields one or more PascalCase navigation property names
     * @return this builder
     */
    public Builder expand(String... fields) {
      Objects.requireNonNull(fields, "fields must not be null");
      for (String f : fields) {
        Objects.requireNonNull(f, "field must not be null");
        expandFields.add(f);
      }
      return this;
    }

    /**
     * Sets {@code $expand=*} to expand all navigation properties.
     *
     * <p>When set, any individually added expand fields are ignored.
     *
     * @return this builder
     */
    public Builder expandAll() {
      this.expandAll = true;
      return this;
    }

    /**
     * Sets a raw {@code $filter} expression string.
     *
     * <p>Overwrites any previously set raw or typed filter.
     *
     * @param expression the raw OData filter expression
     * @return this builder
     */
    public Builder filter(String expression) {
      Objects.requireNonNull(expression, "expression must not be null");
      this.rawFilter = expression;
      this.typedFilter = null;
      return this;
    }

    /**
     * Sets a type-safe {@code $filter} expression built with {@link Filter}.
     *
     * <p>Overwrites any previously set raw or typed filter.
     *
     * @param filter the typed filter
     * @return this builder
     */
    public Builder filter(Filter filter) {
      Objects.requireNonNull(filter, "filter must not be null");
      this.typedFilter = filter;
      this.rawFilter = null;
      return this;
    }

    /**
     * Adds an {@code $orderby} clause.
     *
     * <p>Multiple calls append additional sort criteria.
     *
     * @param field the PascalCase property name
     * @param direction the sort direction
     * @return this builder
     */
    public Builder orderBy(String field, SortDirection direction) {
      Objects.requireNonNull(field, "field must not be null");
      Objects.requireNonNull(direction, "direction must not be null");
      orderByClauses.add("%s %s".formatted(field, direction.toOData()));
      return this;
    }

    /**
     * Sets the {@code $top} query option (page size).
     *
     * @param top maximum number of results to return
     * @return this builder
     * @throws IllegalArgumentException if top is negative
     */
    public Builder top(int top) {
      if (top < 0) {
        throw new IllegalArgumentException("top must not be negative: " + top);
      }
      this.top = top;
      return this;
    }

    /**
     * Sets the {@code $skip} query option (page offset).
     *
     * @param skip number of results to skip
     * @return this builder
     * @throws IllegalArgumentException if skip is negative
     */
    public Builder skip(int skip) {
      if (skip < 0) {
        throw new IllegalArgumentException("skip must not be negative: " + skip);
      }
      this.skip = skip;
      return this;
    }

    /** Builds an immutable {@link ODataQuery}. */
    public ODataQuery build() {
      return new ODataQuery(this);
    }
  }
}
