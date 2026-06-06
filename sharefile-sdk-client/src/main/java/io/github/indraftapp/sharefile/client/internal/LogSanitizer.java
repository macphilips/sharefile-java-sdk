package io.github.indraftapp.sharefile.client.internal;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Redacts sensitive request and response values before TRACE logging. */
public final class LogSanitizer {

  private static final String REDACTED = "***";
  private static final Pattern JSON_SECRET_PATTERN =
      Pattern.compile(
          "(?i)(\"(?:authorization|password|client_secret|access_token|refresh_token)\"\\s*:\\s*\")([^\"]*)(\")");
  private static final Pattern FORM_SECRET_PATTERN =
      Pattern.compile(
          "(?i)(^|&)(authorization|password|client_secret|access_token|refresh_token)=([^&]*)");

  private LogSanitizer() {}

  public static Map<String, String> redactHeaders(Map<String, String> headers) {
    Objects.requireNonNull(headers, "headers must not be null");
    Map<String, String> sanitized = new LinkedHashMap<>();
    headers.forEach(
        (key, value) -> {
          if (isSensitiveKey(key)) {
            sanitized.put(key, REDACTED);
          } else {
            sanitized.put(key, value);
          }
        });
    return sanitized;
  }

  public static String redactBody(String body) {
    if (body == null || body.isEmpty()) {
      return body;
    }
    String redacted = JSON_SECRET_PATTERN.matcher(body).replaceAll("$1" + REDACTED + "$3");
    return FORM_SECRET_PATTERN.matcher(redacted).replaceAll("$1$2=" + REDACTED);
  }

  private static boolean isSensitiveKey(String key) {
    String lower = key.toLowerCase(Locale.ROOT);
    return lower.equals("authorization")
        || lower.contains("password")
        || lower.contains("client_secret")
        || lower.contains("refresh_token")
        || lower.contains("access_token");
  }
}
