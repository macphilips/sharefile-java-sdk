package io.github.indraftapp.sharefile.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.core.jackson.ShareFileObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Utility for loading classpath fixtures from {@code sharefile-sdk-test}. */
public final class ShareFileFixtures {

  private static final String ROOT = "fixtures/";
  private static final ObjectMapper MAPPER = ShareFileObjectMapper.create();

  private ShareFileFixtures() {}

  public static String readString(String fixturePath) {
    return new String(readBytes(fixturePath), StandardCharsets.UTF_8);
  }

  public static byte[] readBytes(String fixturePath) {
    try (InputStream stream = openFixture(fixturePath)) {
      return stream.readAllBytes();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read fixture " + fixturePath, e);
    }
  }

  public static JsonNode readJsonNode(String fixturePath) {
    try (InputStream stream = openFixture(fixturePath)) {
      return MAPPER.readTree(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse fixture " + fixturePath, e);
    }
  }

  public static <T> T readValue(String fixturePath, Class<T> type) {
    try (InputStream stream = openFixture(fixturePath)) {
      return MAPPER.readValue(stream, type);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to deserialize fixture " + fixturePath, e);
    }
  }

  public static <T> T readValue(String fixturePath, TypeReference<T> type) {
    try (InputStream stream = openFixture(fixturePath)) {
      return MAPPER.readValue(stream, type);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to deserialize fixture " + fixturePath, e);
    }
  }

  private static InputStream openFixture(String fixturePath) {
    String normalizedPath = normalizePath(fixturePath);
    ClassLoader classLoader = ShareFileFixtures.class.getClassLoader();
    InputStream stream = classLoader.getResourceAsStream(ROOT + normalizedPath);
    if (stream == null) {
      throw new IllegalArgumentException("Fixture not found: " + normalizedPath);
    }
    return stream;
  }

  private static String normalizePath(String fixturePath) {
    String normalized = Objects.requireNonNull(fixturePath, "fixturePath must not be null").trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("fixturePath must not be blank");
    }
    return normalized.startsWith("/") ? normalized.substring(1) : normalized;
  }
}
