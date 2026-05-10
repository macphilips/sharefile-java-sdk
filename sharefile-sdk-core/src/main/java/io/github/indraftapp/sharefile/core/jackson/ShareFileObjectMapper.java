package io.github.indraftapp.sharefile.core.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Factory for pre-configured Jackson {@link ObjectMapper} instances tuned for the ShareFile REST
 * API.
 *
 * <p>The returned mapper is configured with:
 *
 * <ul>
 *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES = false}
 *   <li>{@code WRITE_DATES_AS_TIMESTAMPS = false} (ISO-8601 strings)
 *   <li>{@code UPPER_CAMEL_CASE} property naming (PascalCase)
 *   <li>Java Time module for {@code java.time.*} support
 *   <li>{@link ShareFileModule} for OData polymorphic deserialization
 * </ul>
 */
public final class ShareFileObjectMapper {

  private ShareFileObjectMapper() {}

  /**
   * Creates a new {@link ObjectMapper} configured for the ShareFile API.
   *
   * <p>Each call returns a new instance. Callers should cache the result if they need to reuse it.
   *
   * @return a pre-configured ObjectMapper
   */
  public static ObjectMapper create() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new ShareFileModule());
    return mapper;
  }
}
