package io.github.indraftapp.sharefile.core.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.core.jackson.ShareFileObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemSerializationTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = ShareFileObjectMapper.create();
  }

  @Test
  void shouldSerializeItemWithPascalCaseFieldNames() throws Exception {
    Item item = new Item();
    item.setName("Test Document");
    item.setDescription("A test file");
    item.setFileSizeBytes(1024L);
    item.setIsHidden(false);

    String json = mapper.writeValueAsString(item);

    assertThat(json).contains("\"Name\":\"Test Document\"");
    assertThat(json).contains("\"Description\":\"A test file\"");
    assertThat(json).contains("\"FileSizeBytes\":1024");
    assertThat(json).contains("\"IsHidden\":false");
  }

  @Test
  void shouldSerializeODataMetadataFields() throws Exception {
    Item item = new Item();
    item.setMetadata("https://example.sf-api.com/sf/v3/$metadata#Items/@Element");
    item.setType("ShareFile.Api.Models.Item");
    item.setId("abc-123");
    item.setUrl("https://example.sf-api.com/sf/v3/Items(abc-123)");

    String json = mapper.writeValueAsString(item);

    assertThat(json).contains("\"odata.metadata\":");
    assertThat(json).contains("\"odata.type\":");
    assertThat(json).contains("\"Id\":\"abc-123\"");
    assertThat(json).contains("\"url\":");
  }

  @Test
  void shouldOmitUnsetNullFields() throws Exception {
    Item item = new Item();
    item.setName("Only Name Set");

    String json = mapper.writeValueAsString(item);

    assertThat(json).contains("\"Name\":\"Only Name Set\"");
    assertThat(json).doesNotContain("\"Description\":null");
    assertThat(json).doesNotContain("\"Creator\":null");
  }

  @Test
  void shouldSerializeNestedCreator() throws Exception {
    Item item = new Item();
    item.setName("Doc");

    User creator = new User();
    creator.setFirstName("John");
    creator.setLastName("Doe");
    creator.setEmail("john@example.com");
    item.setCreator(creator);

    String json = mapper.writeValueAsString(item);

    assertThat(json).contains("\"Creator\":{");
    assertThat(json).contains("\"FirstName\":\"John\"");
    assertThat(json).contains("\"LastName\":\"Doe\"");
    assertThat(json).contains("\"Email\":\"john@example.com\"");
  }
}
