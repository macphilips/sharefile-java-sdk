package io.github.indraftapp.sharefile.core.odata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-04: Tests for the type-safe OData {@link Filter} DSL.
 */
class FilterTest {

    // ── Comparison operators ─────────────────────────────────────────────

    @Test
    void eq_stringValue_singleQuoted() {
        var filter = Filter.eq("Name", "Reports");
        assertThat(filter.toExpression()).isEqualTo("Name eq 'Reports'");
    }

    @Test
    void eq_integerValue() {
        var filter = Filter.eq("FileCount", 10);
        assertThat(filter.toExpression()).isEqualTo("FileCount eq 10");
    }

    @Test
    void eq_booleanValue() {
        var filter = Filter.eq("IsHidden", false);
        assertThat(filter.toExpression()).isEqualTo("IsHidden eq false");
    }

    @Test
    void eq_booleanTrue() {
        var filter = Filter.eq("IsHidden", true);
        assertThat(filter.toExpression()).isEqualTo("IsHidden eq true");
    }

    @Test
    void ne_stringValue() {
        var filter = Filter.ne("Name", "Trash");
        assertThat(filter.toExpression()).isEqualTo("Name ne 'Trash'");
    }

    @Test
    void gt_numericValue() {
        var filter = Filter.gt("FileCount", 100);
        assertThat(filter.toExpression()).isEqualTo("FileCount gt 100");
    }

    @Test
    void lt_numericValue() {
        var filter = Filter.lt("FileSizeInKB", 1024);
        assertThat(filter.toExpression()).isEqualTo("FileSizeInKB lt 1024");
    }

    // ── String functions ─────────────────────────────────────────────────

    @Test
    void substringOf_producesCorrectODataSyntax() {
        var filter = Filter.substringOf("Name", "report");
        assertThat(filter.toExpression()).isEqualTo("substringof('report', Name)");
    }

    @Test
    void startsWith_producesCorrectODataSyntax() {
        var filter = Filter.startsWith("Name", "Project");
        assertThat(filter.toExpression()).isEqualTo("startswith(Name, 'Project')");
    }

    @Test
    void endsWith_producesCorrectODataSyntax() {
        var filter = Filter.endsWith("FileName", ".pdf");
        assertThat(filter.toExpression()).isEqualTo("endswith(FileName, '.pdf')");
    }

    // ── Type function ────────────────────────────────────────────────────

    @Test
    void isOf_producesCorrectODataSyntax() {
        var filter = Filter.isOf("ShareFile.Api.Models.Folder");
        assertThat(filter.toExpression()).isEqualTo("isof('ShareFile.Api.Models.Folder')");
    }

    // ── Logical combinators ──────────────────────────────────────────────

    @Test
    void and_twoFilters() {
        var filter = Filter.and(
                Filter.eq("IsHidden", false),
                Filter.substringOf("Name", "report")
        );
        assertThat(filter.toExpression())
                .isEqualTo("(IsHidden eq false) and (substringof('report', Name))");
    }

    @Test
    void or_twoFilters() {
        var filter = Filter.or(
                Filter.eq("Name", "Reports"),
                Filter.eq("Name", "Documents")
        );
        assertThat(filter.toExpression())
                .isEqualTo("(Name eq 'Reports') or (Name eq 'Documents')");
    }

    @Test
    void and_threeFilters() {
        var filter = Filter.and(
                Filter.eq("IsHidden", false),
                Filter.gt("FileCount", 0),
                Filter.startsWith("Name", "Project")
        );
        assertThat(filter.toExpression())
                .isEqualTo("(IsHidden eq false) and (FileCount gt 0) and (startswith(Name, 'Project'))");
    }

    @Test
    void nested_andInsideOr() {
        var filter = Filter.or(
                Filter.and(
                        Filter.eq("IsHidden", false),
                        Filter.gt("FileCount", 0)
                ),
                Filter.eq("Name", "Special")
        );
        assertThat(filter.toExpression())
                .isEqualTo("((IsHidden eq false) and (FileCount gt 0)) or (Name eq 'Special')");
    }

    // ── String escaping ──────────────────────────────────────────────────

    @Test
    void eq_stringWithSingleQuote_escapedByDoubling() {
        var filter = Filter.eq("Name", "O'Brien");
        assertThat(filter.toExpression()).isEqualTo("Name eq 'O''Brien'");
    }

    @Test
    void substringOf_stringWithSingleQuote_escapedByDoubling() {
        var filter = Filter.substringOf("Name", "it's");
        assertThat(filter.toExpression()).isEqualTo("substringof('it''s', Name)");
    }

    // ── Validation ───────────────────────────────────────────────────────

    @Test
    void and_fewerThanTwoFilters_throws() {
        assertThatThrownBy(() -> Filter.and(Filter.eq("Name", "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least two filters");
    }

    @Test
    void or_fewerThanTwoFilters_throws() {
        assertThatThrownBy(() -> Filter.or(Filter.eq("Name", "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least two filters");
    }

    // ── equals / hashCode / toString ─────────────────────────────────────

    @Test
    void equalsAndHashCode() {
        var a = Filter.eq("Name", "Reports");
        var b = Filter.eq("Name", "Reports");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toString_returnsExpression() {
        var filter = Filter.eq("Name", "Reports");
        assertThat(filter.toString()).isEqualTo("Name eq 'Reports'");
    }
}
