package io.github.indraftapp.sharefile.core.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class ODataFeedDeserializer extends StdDeserializer<ODataFeed<?>>
    implements ContextualDeserializer {

  private final JavaType itemType;

  ODataFeedDeserializer() {
    this(null);
  }

  private ODataFeedDeserializer(JavaType itemType) {
    super(ODataFeed.class);
    this.itemType = itemType;
  }

  @Override
  public ODataFeed<?> deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    ObjectMapper mapper = (ObjectMapper) parser.getCodec();
    JsonNode node = mapper.readTree(parser);

    ODataFeed<Object> feed = new ODataFeed<>();
    JsonNode metadata = node.get("odata.metadata");
    if (metadata != null && !metadata.isNull()) {
      feed.setMetadata(metadata.asText());
    }

    JsonNode count = node.get("odata.count");
    if (count != null && count.isInt()) {
      feed.setCount(count.intValue());
    } else if (count != null && count.canConvertToInt()) {
      feed.setCount(count.asInt());
    }

    JsonNode nextLink = node.get("odata.nextLink");
    if (nextLink != null && !nextLink.isNull()) {
      feed.setNextLink(nextLink.asText());
    }

    JsonNode valueNode = node.get("value");
    if (valueNode != null && valueNode.isArray()) {
      List<Object> items = new ArrayList<>(valueNode.size());
      JavaType resolvedItemType = itemType != null ? itemType : context.constructType(Object.class);
      for (JsonNode itemNode : valueNode) {
        items.add(mapper.readerFor(resolvedItemType).readValue(itemNode));
      }
      feed.setValue(items);
    }

    return feed;
  }

  @Override
  public ODataFeedDeserializer createContextual(
      DeserializationContext context, BeanProperty property) {
    JavaType contextualType = property != null ? property.getType() : context.getContextualType();
    if (contextualType == null || contextualType.containedTypeCount() == 0) {
      return new ODataFeedDeserializer(context.constructType(Object.class));
    }
    return new ODataFeedDeserializer(contextualType.containedType(0));
  }
}
