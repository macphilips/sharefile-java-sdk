package io.github.indraftapp.sharefile.core.odata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** SF-04: Tests for the {@link ODataQuery} fluent builder. */
class ODataQueryTest {

  // ── Acceptance criteria from ticket ──────────────────────────────────

  @Test
  void selectAndTop_producesExpectedParams() {
    var params = ODataQuery.builder().select("Name").top(10).build().toQueryParams();

    assertThat(params)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "$select", "Name",
                "$top", "10"));
  }

  // ── Select ───────────────────────────────────────────────────────────

  @Test
  void select_multipleFields_commaSeparated() {
    var params =
        ODataQuery.builder().select("Name", "Email", "CreationDate").build().toQueryParams();

    assertThat(params.get("$select")).isEqualTo("Name,Email,CreationDate");
  }

  @Test
  void select_calledMultipleTimes_accumulates() {
    var params = ODataQuery.builder().select("Name").select("Email").build().toQueryParams();

    assertThat(params.get("$select")).isEqualTo("Name,Email");
  }

  // ── Expand ───────────────────────────────────────────────────────────

  @Test
  void expand_multipleFields() {
    var params = ODataQuery.builder().expand("Parent", "Zone").build().toQueryParams();

    assertThat(params.get("$expand")).isEqualTo("Parent,Zone");
  }

  @Test
  void expandAll_producesStar() {
    var params = ODataQuery.builder().expandAll().build().toQueryParams();

    assertThat(params.get("$expand")).isEqualTo("*");
  }

  @Test
  void expandAll_overridesIndividualExpands() {
    var params = ODataQuery.builder().expand("Parent").expandAll().build().toQueryParams();

    assertThat(params.get("$expand")).isEqualTo("*");
  }

  // ── Filter (raw) ─────────────────────────────────────────────────────

  @Test
  void filter_rawString() {
    var params = ODataQuery.builder().filter("IsHidden eq false").build().toQueryParams();

    assertThat(params.get("$filter")).isEqualTo("IsHidden eq false");
  }

  // ── Filter (typed) ───────────────────────────────────────────────────

  @Test
  void filter_typedFilter() {
    var params = ODataQuery.builder().filter(Filter.eq("Name", "Reports")).build().toQueryParams();

    assertThat(params.get("$filter")).isEqualTo("Name eq 'Reports'");
  }

  @Test
  void filter_typedOverwritesRaw() {
    var params =
        ODataQuery.builder()
            .filter("old raw")
            .filter(Filter.eq("Name", "New"))
            .build()
            .toQueryParams();

    assertThat(params.get("$filter")).isEqualTo("Name eq 'New'");
  }

  @Test
  void filter_rawOverwritesTyped() {
    var params =
        ODataQuery.builder()
            .filter(Filter.eq("Name", "Old"))
            .filter("Name eq 'New'")
            .build()
            .toQueryParams();

    assertThat(params.get("$filter")).isEqualTo("Name eq 'New'");
  }

  @Test
  void filter_complexAndOr() {
    var filter =
        Filter.and(
            Filter.eq("IsHidden", false),
            Filter.or(Filter.substringOf("Name", "report"), Filter.startsWith("Name", "Project")));
    var params = ODataQuery.builder().filter(filter).build().toQueryParams();

    assertThat(params.get("$filter"))
        .isEqualTo(
            "(IsHidden eq false) and ((substringof('report', Name)) or (startswith(Name, 'Project')))");
  }

  // ── OrderBy ──────────────────────────────────────────────────────────

  @Test
  void orderBy_singleField() {
    var params =
        ODataQuery.builder().orderBy("CreationDate", SortDirection.DESC).build().toQueryParams();

    assertThat(params.get("$orderby")).isEqualTo("CreationDate desc");
  }

  @Test
  void orderBy_multipleFields() {
    var params =
        ODataQuery.builder()
            .orderBy("CreationDate", SortDirection.DESC)
            .orderBy("Name", SortDirection.ASC)
            .build()
            .toQueryParams();

    assertThat(params.get("$orderby")).isEqualTo("CreationDate desc,Name asc");
  }

  // ── Top / Skip ───────────────────────────────────────────────────────

  @Test
  void top_and_skip() {
    var params = ODataQuery.builder().top(100).skip(50).build().toQueryParams();

    assertThat(params.get("$top")).isEqualTo("100");
    assertThat(params.get("$skip")).isEqualTo("50");
  }

  @Test
  void top_negative_throws() {
    assertThatThrownBy(() -> ODataQuery.builder().top(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("negative");
  }

  @Test
  void skip_negative_throws() {
    assertThatThrownBy(() -> ODataQuery.builder().skip(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("negative");
  }

  // ── Empty query ──────────────────────────────────────────────────────

  @Test
  void emptyQuery_producesEmptyMap() {
    var params = ODataQuery.empty().toQueryParams();
    assertThat(params).isEmpty();
  }

  @Test
  void emptyQuery_isEmpty() {
    assertThat(ODataQuery.empty().isEmpty()).isTrue();
  }

  @Test
  void nonEmptyQuery_isNotEmpty() {
    assertThat(ODataQuery.builder().top(10).build().isEmpty()).isFalse();
  }

  // ── Immutability ─────────────────────────────────────────────────────

  @Test
  void toQueryParams_returnsUnmodifiableMap() {
    var params = ODataQuery.builder().top(10).build().toQueryParams();
    assertThatThrownBy(() -> params.put("$select", "Name"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  // ── Full query ───────────────────────────────────────────────────────

  @Test
  void fullQuery_allOptionsSet() {
    var params =
        ODataQuery.builder()
            .select("Name", "Email", "CreationDate")
            .expand("Parent", "Zone")
            .filter(Filter.eq("IsHidden", false))
            .orderBy("CreationDate", SortDirection.DESC)
            .top(100)
            .skip(0)
            .build()
            .toQueryParams();

    assertThat(params).containsEntry("$select", "Name,Email,CreationDate");
    assertThat(params).containsEntry("$expand", "Parent,Zone");
    assertThat(params).containsEntry("$filter", "IsHidden eq false");
    assertThat(params).containsEntry("$orderby", "CreationDate desc");
    assertThat(params).containsEntry("$top", "100");
    assertThat(params).containsEntry("$skip", "0");
    assertThat(params).hasSize(6);
  }

  // ── Only set params appear ───────────────────────────────────────────

  @Test
  void onlySetParamsAppear() {
    var params = ODataQuery.builder().select("Name").build().toQueryParams();

    assertThat(params).containsOnlyKeys("$select");
  }
}
