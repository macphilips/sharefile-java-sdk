package io.github.indraftapp.sharefile.core.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.indraftapp.sharefile.core.model.ODataEntity;
import io.github.indraftapp.sharefile.core.model.ODataFeed;

/**
 * Jackson module that registers custom deserializers for ShareFile OData types.
 *
 * <p>Registers:
 *
 * <ul>
 *   <li>{@link ODataEntity} deserializer — resolves polymorphic types via {@code odata.type}
 *   <li>{@link ODataFeed} deserializer — handles OData collection responses with {@code
 *       odata.count}, {@code odata.nextLink}, and generic item type resolution
 * </ul>
 *
 * <p>Automatically registered by {@link ShareFileObjectMapper#create()}.
 */
public final class ShareFileModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public ShareFileModule() {
    addDeserializer(ODataEntity.class, new ODataEntityDeserializer());
    addDeserializer((Class) ODataFeed.class, new ODataFeedDeserializer());
  }
}
