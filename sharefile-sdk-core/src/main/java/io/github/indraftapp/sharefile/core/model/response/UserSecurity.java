package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Dynamic response payload containing user security settings.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSecurity {

    private final Map<String, Object> properties = new HashMap<>();

    /**
     * Returns all dynamically captured user security properties.
     *
     * @return the mutable map of dynamically captured properties
     */
    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Adds a dynamically named user security property during deserialization.
     *
     * @param key the property name
     * @param value the property value
     */
    @JsonAnySetter
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * Returns a dynamically named user security property by key.
     *
     * @param key the property name
     * @return the property value, or {@code null} if absent
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
}
