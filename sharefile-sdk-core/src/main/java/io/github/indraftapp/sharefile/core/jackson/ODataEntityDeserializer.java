package io.github.indraftapp.sharefile.core.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import io.github.indraftapp.sharefile.core.model.ODataEntity;

import java.io.IOException;

final class ODataEntityDeserializer extends StdDeserializer<ODataEntity> {

    ODataEntityDeserializer() {
        super(ODataEntity.class);
    }

    @Override
    public ODataEntity deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode node = mapper.readTree(parser);
        JsonNode typeNode = node.get("odata.type");

        if (typeNode == null || typeNode.isNull() || typeNode.asText().isBlank()) {
            throw InvalidTypeIdException.from(
                    parser,
                    "Missing ShareFile odata.type for ODataEntity deserialization",
                    context.constructType(ODataEntity.class),
                    null
            );
        }

        Class<? extends ODataEntity> targetType = ODataTypeResolver.resolveEntityType(typeNode.asText())
                .orElseThrow(() -> InvalidTypeIdException.from(
                        parser,
                        "Unknown ShareFile odata.type: " + typeNode.asText(),
                        context.constructType(ODataEntity.class),
                        typeNode.asText()
                ));

        return mapper.readerFor(targetType).readValue(node);
    }
}
