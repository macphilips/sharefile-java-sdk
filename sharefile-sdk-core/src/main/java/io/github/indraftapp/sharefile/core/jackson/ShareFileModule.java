package io.github.indraftapp.sharefile.core.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.indraftapp.sharefile.core.model.ODataEntity;
import io.github.indraftapp.sharefile.core.model.ODataFeed;

public final class ShareFileModule extends SimpleModule {

    public ShareFileModule() {
        addDeserializer(ODataEntity.class, new ODataEntityDeserializer());
        addDeserializer(ODataFeed.class, new ODataFeedDeserializer());
    }
}
