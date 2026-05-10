package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.core.model.enums.ItemOrderingMode;
import io.github.indraftapp.sharefile.core.model.enums.TreeMode;
import io.github.indraftapp.sharefile.core.odata.Filter;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItemQueryTest {

  @Test
  void mergesODataAndItemSpecificParameters() {
    ItemQuery query =
        ItemQuery.builder()
            .odata(ODataQuery.builder().top(25).filter(Filter.eq("Name", "Reports")).build())
            .includeDeleted(true)
            .orderingMode(ItemOrderingMode.NAME_ASC)
            .treeMode(TreeMode.COPY)
            .sourceId("source-1")
            .build();

    Map<String, String> params = query.toQueryParams();

    assertEquals("25", params.get("$top"));
    assertEquals("Name eq 'Reports'", params.get("$filter"));
    assertEquals("true", params.get("includeDeleted"));
    assertEquals("NameAsc", params.get("orderingMode"));
    assertEquals("Copy", params.get("treemode"));
    assertEquals("source-1", params.get("sourceId"));
  }

  @Test
  void customParametersRejectBlankAndODataPrefixedNames() {
    IllegalArgumentException blankName =
        assertThrows(
            IllegalArgumentException.class, () -> ItemQuery.builder().param("  ", "value").build());
    IllegalArgumentException odataStyleName =
        assertThrows(
            IllegalArgumentException.class,
            () -> ItemQuery.builder().param("$expand", "Parent").build());

    assertTrue(blankName.getMessage().contains("blank"));
    assertTrue(odataStyleName.getMessage().contains("$expand"));
  }
}
