package io.github.indraftapp.sharefile.core.odata;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Type-safe OData {@code $filter} expression builder.
 *
 * <p>Usage:
 * <pre>{@code
 * Filter.eq("Name", "Reports")           // Name eq 'Reports'
 * Filter.gt("FileCount", 10)             // FileCount gt 10
 * Filter.substringOf("Name", "report")   // substringof('report', Name)
 * Filter.and(
 *     Filter.eq("IsHidden", false),
 *     Filter.substringOf("Name", "report")
 * )
 * }</pre>
 */
public final class Filter {

    private final String expression;

    private Filter(String expression) {
        this.expression = Objects.requireNonNull(expression, "expression must not be null");
    }

    /**
     * Produces {@code field eq value}.
     */
    public static Filter eq(String field, Object value) {
        return comparison(field, "eq", value);
    }

    /**
     * Produces {@code field ne value}.
     */
    public static Filter ne(String field, Object value) {
        return comparison(field, "ne", value);
    }

    /**
     * Produces {@code field gt value}.
     */
    public static Filter gt(String field, Object value) {
        return comparison(field, "gt", value);
    }

    /**
     * Produces {@code field lt value}.
     */
    public static Filter lt(String field, Object value) {
        return comparison(field, "lt", value);
    }

    /**
     * Produces {@code substringof('value', field)}.
     *
     * <p>Note: OData v3 uses {@code substringof(needle, haystack)} — the search
     * value comes first, the field name second.
     */
    public static Filter substringOf(String field, String value) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(value, "value must not be null");
        return new Filter("substringof('%s', %s)".formatted(escapeString(value), field));
    }

    /**
     * Produces {@code startswith(field, 'value')}.
     */
    public static Filter startsWith(String field, String value) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(value, "value must not be null");
        return new Filter("startswith(%s, '%s')".formatted(field, escapeString(value)));
    }

    /**
     * Produces {@code endswith(field, 'value')}.
     */
    public static Filter endsWith(String field, String value) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(value, "value must not be null");
        return new Filter("endswith(%s, '%s')".formatted(field, escapeString(value)));
    }

    /**
     * Produces {@code isof('type')}.
     */
    public static Filter isOf(String type) {
        Objects.requireNonNull(type, "type must not be null");
        return new Filter("isof('%s')".formatted(escapeString(type)));
    }

    /**
     * Combines filters with {@code and}.
     *
     * @throws IllegalArgumentException if fewer than two filters are provided
     */
    public static Filter and(Filter... filters) {
        return combine("and", filters);
    }

    /**
     * Combines filters with {@code or}.
     *
     * @throws IllegalArgumentException if fewer than two filters are provided
     */
    public static Filter or(Filter... filters) {
        return combine("or", filters);
    }

    /**
     * Returns the OData filter expression string.
     */
    public String toExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Filter other)) return false;
        return expression.equals(other.expression);
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }

    // ── Internal ─────────────────────────────────────────────────────────

    private static Filter comparison(String field, String operator, Object value) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(value, "value must not be null");
        return new Filter("%s %s %s".formatted(field, operator, formatValue(value)));
    }

    private static Filter combine(String operator, Filter... filters) {
        Objects.requireNonNull(filters, "filters must not be null");
        if (filters.length < 2) {
            throw new IllegalArgumentException(
                    "'%s' requires at least two filters".formatted(operator));
        }
        var joiner = new StringJoiner(" %s ".formatted(operator));
        for (Filter f : filters) {
            Objects.requireNonNull(f, "filter must not be null");
            joiner.add("(%s)".formatted(f.expression));
        }
        return new Filter(joiner.toString());
    }

    /**
     * Formats a value for OData filter syntax:
     * <ul>
     *   <li>Strings → single-quoted with internal quotes escaped</li>
     *   <li>Booleans → {@code true}/{@code false}</li>
     *   <li>Numbers → numeric literal</li>
     *   <li>Other → {@code toString()} without quoting</li>
     * </ul>
     */
    private static String formatValue(Object value) {
        if (value instanceof String s) {
            return "'%s'".formatted(escapeString(s));
        } else if (value instanceof Boolean b) {
            return b.toString();
        } else {
            return value.toString();
        }
    }

    /**
     * Escapes single quotes in OData string literals by doubling them.
     */
    private static String escapeString(String s) {
        return s.replace("'", "''");
    }
}
