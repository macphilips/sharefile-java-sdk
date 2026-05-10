package io.github.indraftapp.sharefile.core.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.core.model.AccessControl;
import io.github.indraftapp.sharefile.core.model.AsyncOperation;
import io.github.indraftapp.sharefile.core.model.File;
import io.github.indraftapp.sharefile.core.model.Folder;
import io.github.indraftapp.sharefile.core.model.Group;
import io.github.indraftapp.sharefile.core.model.Item;
import io.github.indraftapp.sharefile.core.model.ODataEntity;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.Share;
import io.github.indraftapp.sharefile.core.model.enums.PreviewStatus;
import io.github.indraftapp.sharefile.core.model.request.BulkAccessControlRequest;
import io.github.indraftapp.sharefile.core.model.request.RequestShareRequest;
import io.github.indraftapp.sharefile.core.model.request.SendShareRequest;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShareFileObjectMapperTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = ShareFileObjectMapper.create();
  }

  @Test
  void shouldIgnoreUnknownPropertiesByDefault() throws Exception {
    String json =
        """
                {
                    "odata.type": "ShareFile.Api.Models.File",
                    "Id": "file-1",
                    "Name": "report.pdf",
                    "UnexpectedField": "ignored"
                }
                """;

    Item item = mapper.readValue(json, Item.class);

    assertThat(item).isInstanceOf(File.class);
    assertThat(item.getId()).isEqualTo("file-1");
  }

  @Test
  void shouldSerializeJavaTimeTypesAsIso8601Strings() throws Exception {
    TimePayload payload = new TimePayload();
    payload.setInstant(Instant.parse("2026-05-09T10:15:30Z"));
    payload.setZonedDateTime(ZonedDateTime.parse("2026-05-09T11:15:30+01:00[Africa/Lagos]"));

    JsonNode json = mapper.readTree(mapper.writeValueAsBytes(payload));

    assertThat(json.get("Instant").isTextual()).isTrue();
    assertThat(json.get("Instant").asText()).isEqualTo("2026-05-09T10:15:30Z");
    assertThat(json.get("ZonedDateTime").isTextual()).isTrue();
  }

  @Test
  void shouldApplyPascalCaseNamingStrategyToPlainBeans() throws Exception {
    PascalCasePayload payload = new PascalCasePayload();
    payload.setFileName("quarterly-report.pdf");

    JsonNode json = mapper.readTree(mapper.writeValueAsBytes(payload));

    assertThat(json.has("FileName")).isTrue();
    assertThat(json.has("fileName")).isFalse();
  }

  @Test
  void shouldDeserializeODataEntityUsingTypeResolver() throws Exception {
    String json =
        """
                {
                    "odata.type": "ShareFile.Api.Models.AsyncOperation",
                    "Id": "op1",
                    "State": "Queued"
                }
                """;

    ODataEntity entity = mapper.readValue(json, ODataEntity.class);

    assertThat(entity).isInstanceOf(AsyncOperation.class);
    assertThat(entity.getId()).isEqualTo("op1");
  }

  @Test
  void shouldDeserializeFeedWithConcreteItemSubtypes() throws Exception {
    String json =
        """
                {
                    "odata.metadata": "https://example.sf-api.com/sf/v3/$metadata#Items",
                    "odata.count": 2,
                    "odata.nextLink": "https://example.sf-api.com/sf/v3/Items?$skip=2",
                    "value": [
                        {
                            "odata.type": "ShareFile.Api.Models.File",
                            "Id": "file-1",
                            "Name": "report.pdf"
                        },
                        {
                            "odata.type": "ShareFile.Api.Models.Folder",
                            "Id": "folder-1",
                            "Name": "Finance"
                        }
                    ]
                }
                """;

    ODataFeed<Item> feed = mapper.readValue(json, new TypeReference<>() {});

    assertThat(feed.getMetadata()).isEqualTo("https://example.sf-api.com/sf/v3/$metadata#Items");
    assertThat(feed.getCount()).isEqualTo(2);
    assertThat(feed.getNextLink()).isEqualTo("https://example.sf-api.com/sf/v3/Items?$skip=2");
    assertThat(feed.getItems()).hasSize(2);
    assertThat(feed.getItems().get(0)).isInstanceOf(File.class);
    assertThat(feed.getItems().get(1)).isInstanceOf(Folder.class);
  }

  @Test
  void shouldDeserializeShareNestedItemsUsingTypeResolver() throws Exception {
    String json =
        """
                {
                    "Id": "share-1",
                    "Items": [
                        {
                            "odata.type": "ShareFile.Api.Models.File",
                            "Id": "file-1",
                            "Name": "report.pdf"
                        }
                    ],
                    "Parent": {
                        "odata.type": "ShareFile.Api.Models.Folder",
                        "Id": "folder-1",
                        "Name": "Finance"
                    }
                }
                """;

    Share share = mapper.readValue(json, Share.class);

    assertThat(share.getItems()).singleElement().isInstanceOf(File.class);
    assertThat(share.getParent()).isInstanceOf(Folder.class);
  }

  @Test
  void shouldDeserializeAccessControlPrincipalUsingTypeResolver() throws Exception {
    String json =
        """
                {
                    "Id": "acl-1",
                    "Principal": {
                        "odata.type": "ShareFile.Api.Models.Group",
                        "Id": "group-1",
                        "Name": "Finance Team"
                    },
                    "CanDownload": true
                }
                """;

    AccessControl accessControl = mapper.readValue(json, AccessControl.class);

    assertThat(accessControl.getPrincipal()).isInstanceOf(Group.class);
    assertThat(((Group) accessControl.getPrincipal()).getName()).isEqualTo("Finance Team");
    assertThat(accessControl.getCanDownload()).isTrue();
  }

  @Test
  void shouldDeserializeExtendedPreviewStatusValues() throws Exception {
    String canDocThumbJson =
        """
                {
                    "odata.type": "ShareFile.Api.Models.File",
                    "Id": "file-1",
                    "PreviewStatus": "CanDocThumb"
                }
                """;
    String blankPreviewStatusJson =
        """
                {
                    "odata.type": "ShareFile.Api.Models.File",
                    "Id": "file-2",
                    "PreviewStatus": ""
                }
                """;
    String unknownPreviewStatusJson =
        """
                {
                    "odata.type": "ShareFile.Api.Models.File",
                    "Id": "file-3",
                    "PreviewStatus": "FuturePreviewMode"
                }
                """;

    Item withDocThumbnailPreview = mapper.readValue(canDocThumbJson, Item.class);
    Item withBlankPreviewStatus = mapper.readValue(blankPreviewStatusJson, Item.class);
    Item withUnknownPreviewStatus = mapper.readValue(unknownPreviewStatusJson, Item.class);

    assertThat(withDocThumbnailPreview.getPreviewStatus()).isEqualTo(PreviewStatus.CAN_DOC_THUMB);
    assertThat(withBlankPreviewStatus.getPreviewStatus()).isEqualTo(PreviewStatus.NONE);
    assertThat(withUnknownPreviewStatus.getPreviewStatus()).isEqualTo(PreviewStatus.UNKNOWN);
  }

  @Test
  void shouldOmitNullFieldsDuringSerialization() throws Exception {
    Item item = new Item();
    item.setName("Only Name Set");

    JsonNode json = mapper.readTree(mapper.writeValueAsBytes(item));

    assertThat(json.get("Name").asText()).isEqualTo("Only Name Set");
    assertThat(json.has("Description")).isFalse();
    assertThat(json.has("Creator")).isFalse();
  }

  @Test
  void shouldSerializeShareRecipientsAsRecipientObjects() throws Exception {
    SendShareRequest sendShareRequest = new SendShareRequest();
    sendShareRequest.setItems(java.util.List.of("item-1"));
    sendShareRequest.setRecipients(java.util.List.of("send@example.com"));

    RequestShareRequest requestShareRequest = new RequestShareRequest();
    requestShareRequest.setFolderID("folder-1");
    requestShareRequest.setRecipients(java.util.List.of("request@example.com"));

    JsonNode sendJson = mapper.readTree(mapper.writeValueAsBytes(sendShareRequest));
    JsonNode requestJson = mapper.readTree(mapper.writeValueAsBytes(requestShareRequest));

    assertThat(sendJson.at("/Items/0/Id").asText()).isEqualTo("item-1");
    assertThat(sendJson.at("/Recipients/0/User/Email").asText()).isEqualTo("send@example.com");
    assertThat(requestJson.at("/Parent/Id").asText()).isEqualTo("folder-1");
    assertThat(requestJson.at("/Recipients/0/User/Email").asText())
        .isEqualTo("request@example.com");
  }

  @Test
  void shouldSerializeBulkAccessControlParamsWithPerEntryFlags() throws Exception {
    AccessControl accessControl = new AccessControl();
    accessControl.setCanDownload(Boolean.TRUE);

    BulkAccessControlRequest request = new BulkAccessControlRequest();
    request.setAccessControls(java.util.List.of(accessControl));
    request.setNotifyUser(Boolean.TRUE);
    request.setNotifyMessage("granted");
    request.setRecursive(Boolean.FALSE);

    JsonNode json = mapper.readTree(mapper.writeValueAsBytes(request));

    assertThat(json.at("/NotifyMessage").asText()).isEqualTo("granted");
    assertThat(json.at("/AccessControlParams/0/NotifyUser").asBoolean()).isTrue();
    assertThat(json.at("/AccessControlParams/0/Recursive").asBoolean()).isFalse();
    assertThat(json.at("/AccessControlParams/0/AccessControl/CanDownload").asBoolean()).isTrue();
  }

  private static final class PascalCasePayload {
    private String fileName;

    public String getFileName() {
      return fileName;
    }

    public void setFileName(String fileName) {
      this.fileName = fileName;
    }
  }

  private static final class TimePayload {
    private Instant instant;
    private ZonedDateTime zonedDateTime;

    public Instant getInstant() {
      return instant;
    }

    public void setInstant(Instant instant) {
      this.instant = instant;
    }

    public ZonedDateTime getZonedDateTime() {
      return zonedDateTime;
    }

    public void setZonedDateTime(ZonedDateTime zonedDateTime) {
      this.zonedDateTime = zonedDateTime;
    }
  }
}
