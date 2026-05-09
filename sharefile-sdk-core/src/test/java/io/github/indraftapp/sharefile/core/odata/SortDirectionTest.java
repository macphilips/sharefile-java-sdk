package io.github.indraftapp.sharefile.core.odata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SF-04: Tests for {@link SortDirection}. */
class SortDirectionTest {

  @Test
  void asc_toOData() {
    assertThat(SortDirection.ASC.toOData()).isEqualTo("asc");
  }

  @Test
  void desc_toOData() {
    assertThat(SortDirection.DESC.toOData()).isEqualTo("desc");
  }

  @Test
  void enumValues() {
    assertThat(SortDirection.values()).containsExactly(SortDirection.ASC, SortDirection.DESC);
  }
}
