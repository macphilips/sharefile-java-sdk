package io.github.indraftapp.sharefile.core.odata;

/** Sort direction for OData {@code $orderby} clauses. */
public enum SortDirection {
  ASC("asc"),
  DESC("desc");

  private final String odataValue;

  SortDirection(String odataValue) {
    this.odataValue = odataValue;
  }

  /** Returns the OData representation ({@code "asc"} or {@code "desc"}). */
  public String toOData() {
    return odataValue;
  }
}
