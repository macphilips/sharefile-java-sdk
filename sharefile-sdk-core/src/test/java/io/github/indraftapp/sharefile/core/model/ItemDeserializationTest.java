package io.github.indraftapp.sharefile.core.model;

import io.github.indraftapp.sharefile.core.jackson.ShareFileObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ItemDeserializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = ShareFileObjectMapper.create();
    }

    @Test
    void shouldDeserializeItemWithODataMetadata() throws Exception {
        String json = """
                {
                    "odata.metadata": "https://example.sf-api.com/sf/v3/$metadata#Items/@Element",
                    "odata.type": "ShareFile.Api.Models.File",
                    "Id": "item-abc-123",
                    "url": "https://example.sf-api.com/sf/v3/Items(item-abc-123)",
                    "Name": "report.pdf",
                    "FileName": "report.pdf",
                    "Description": "Monthly report",
                    "FileSizeBytes": 204800,
                    "FileSizeInKB": 200,
                    "IsHidden": false
                }
                """;

        Item item = mapper.readValue(json, Item.class);

        assertThat(item).isInstanceOf(File.class);
        assertThat(item.getMetadata()).isEqualTo("https://example.sf-api.com/sf/v3/$metadata#Items/@Element");
        assertThat(item.getType()).isEqualTo("ShareFile.Api.Models.File");
        assertThat(item.getId()).isEqualTo("item-abc-123");
        assertThat(item.getUrl()).isEqualTo("https://example.sf-api.com/sf/v3/Items(item-abc-123)");
        assertThat(item.getName()).isEqualTo("report.pdf");
        assertThat(item.getFileName()).isEqualTo("report.pdf");
        assertThat(item.getDescription()).isEqualTo("Monthly report");
        assertThat(item.getFileSizeBytes()).isEqualTo(204800L);
        assertThat(item.getFileSizeInKB()).isEqualTo(200);
        assertThat(item.getIsHidden()).isFalse();
    }

    @Test
    void shouldDeserializeItemWithNestedCreator() throws Exception {
        String json = """
                {
                    "Id": "item-456",
                    "Name": "presentation.pptx",
                    "Creator": {
                        "Id": "user-789",
                        "Email": "jane@example.com",
                        "FirstName": "Jane",
                        "LastName": "Smith"
                    }
                }
                """;

        Item item = mapper.readValue(json, Item.class);

        assertThat(item.getId()).isEqualTo("item-456");
        assertThat(item.getName()).isEqualTo("presentation.pptx");
        assertThat(item.getCreator()).isNotNull();
        assertThat(item.getCreator().getId()).isEqualTo("user-789");
        assertThat(item.getCreator().getEmail()).isEqualTo("jane@example.com");
        assertThat(item.getCreator().getFirstName()).isEqualTo("Jane");
        assertThat(item.getCreator().getLastName()).isEqualTo("Smith");
    }

    @Test
    void shouldDeserializeItemWithDateFields() throws Exception {
        String json = """
                {
                    "Id": "item-date-test",
                    "Name": "dated-file.txt",
                    "CreationDate": "2024-01-15T10:30:00Z",
                    "ExpirationDate": "2025-01-15T10:30:00Z"
                }
                """;

        Item item = mapper.readValue(json, Item.class);

        assertThat(item.getCreationDate()).isEqualTo(Instant.parse("2024-01-15T10:30:00Z"));
        assertThat(item.getExpirationDate()).isEqualTo(Instant.parse("2025-01-15T10:30:00Z"));
    }

    @Test
    void shouldIgnoreUnknownProperties() throws Exception {
        String json = """
                {
                    "Id": "item-unknown",
                    "Name": "test.txt",
                    "SomeUnknownField": "should be ignored",
                    "AnotherUnknown": 42
                }
                """;

        Item item = mapper.readValue(json, Item.class);

        assertThat(item.getId()).isEqualTo("item-unknown");
        assertThat(item.getName()).isEqualTo("test.txt");
    }

    @Test
    void shouldDeserializeItemWithNestedZone() throws Exception {
        String json = """
                {
                    "Id": "item-zone",
                    "Name": "zoned-file.txt",
                    "Zone": {
                        "Id": "zone-1",
                        "Name": "US East Storage"
                    }
                }
                """;

        Item item = mapper.readValue(json, Item.class);

        assertThat(item.getZone()).isNotNull();
        assertThat(item.getZone().getId()).isEqualTo("zone-1");
        assertThat(item.getZone().getName()).isEqualTo("US East Storage");
    }

    @Test
    void shouldDeserializeNestedItemSubtypes() throws Exception {
        String json = """
                {
                    "Id": "folder-1",
                    "odata.type": "ShareFile.Api.Models.Folder",
                    "Children": [
                        {
                            "Id": "file-1",
                            "odata.type": "ShareFile.Api.Models.File",
                            "Name": "report.pdf"
                        },
                        {
                            "Id": "note-1",
                            "odata.type": "ShareFile.Api.Models.Note",
                            "Name": "Read me"
                        }
                    ]
                }
                """;

        Folder folder = mapper.readValue(json, Folder.class);

        assertThat(folder.getChildren()).hasSize(2);
        assertThat(folder.getChildren().get(0)).isInstanceOf(File.class);
        assertThat(folder.getChildren().get(1)).isInstanceOf(Note.class);
    }

    @Test
    void shouldDeserializeNestedUserSubtype() throws Exception {
        String json = """
                {
                    "Id": "item-1",
                    "Name": "shared-folder",
                    "Creator": {
                        "Id": "account-user-1",
                        "odata.type": "ShareFile.Api.Models.AccountUser",
                        "Email": "owner@example.com",
                        "IsAdministrator": true
                    }
                }
                """;

        Item item = mapper.readValue(json, Item.class);

        assertThat(item.getCreator()).isInstanceOf(AccountUser.class);
        assertThat(((AccountUser) item.getCreator()).getIsAdministrator()).isTrue();
    }
}
