package io.github.indraftapp.sharefile.client.auth;

import io.github.indraftapp.sharefile.core.exception.HmacValidationException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Validates the HMAC-SHA256 signature ({@code h} parameter) on authorization code redirects.
 *
 * <p>The validation process per &sect;5.5 of the tech spec:
 *
 * <ol>
 *   <li>Strip the {@code h} query parameter from the redirect URI.
 *   <li>Extract path + remaining query string.
 *   <li>Compute HMAC-SHA256 using the client secret.
 *   <li>Base64-encode, then URL-encode the digest.
 *   <li>Compare against the received {@code h} value.
 * </ol>
 */
final class HmacValidator {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private HmacValidator() {}

  /**
   * Validates the HMAC-SHA256 signature on an authorization code redirect URI.
   *
   * @param redirectUri the full redirect URI including the {@code h} parameter
   * @param clientSecret the client secret used as the HMAC key
   * @throws HmacValidationException if validation fails or the {@code h} parameter is missing
   */
  static void validate(URI redirectUri, String clientSecret) {
    String query = redirectUri.getRawQuery();
    if (query == null || query.isEmpty()) {
      throw new HmacValidationException("Redirect URI has no query parameters");
    }

    Map<String, String> params = parseQueryString(query);
    String receivedHmac = params.get("h");
    if (receivedHmac == null || receivedHmac.isEmpty()) {
      throw new HmacValidationException("Missing 'h' parameter in redirect URI");
    }

    // Build path + query string without the 'h' parameter
    String pathAndQuery = buildPathWithoutH(redirectUri.getRawPath(), query);

    // Compute expected HMAC
    String expectedHmac = computeUrlEncodedHmac(pathAndQuery, clientSecret);

    // URL-decode both sides for comparison (the received value may be URL-encoded)
    String decodedReceived = URLDecoder.decode(receivedHmac, StandardCharsets.UTF_8);
    String decodedExpected = URLDecoder.decode(expectedHmac, StandardCharsets.UTF_8);

    if (!decodedReceived.equals(decodedExpected)) {
      throw new HmacValidationException(
          "HMAC validation failed: redirect URI signature does not match");
    }
  }

  /**
   * Computes the HMAC-SHA256 digest, Base64-encodes it, then URL-encodes the result.
   *
   * @param data the data to sign
   * @param key the HMAC key (client secret)
   * @return the URL-encoded, Base64-encoded HMAC digest
   */
  static String computeUrlEncodedHmac(String data, String key) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      String base64 = Base64.getEncoder().encodeToString(digest);
      return URLEncoder.encode(base64, StandardCharsets.UTF_8);
    } catch (NoSuchAlgorithmException e) {
      throw new HmacValidationException("HMAC-SHA256 algorithm not available", e);
    } catch (InvalidKeyException e) {
      throw new HmacValidationException("Invalid HMAC key", e);
    }
  }

  private static String buildPathWithoutH(String path, String rawQuery) {
    StringBuilder sb = new StringBuilder();
    if (path != null) {
      sb.append(path);
    }

    // Rebuild query string without 'h' parameter
    String[] pairs = rawQuery.split("&");
    boolean first = true;
    for (String pair : pairs) {
      String paramName = pair.contains("=") ? pair.substring(0, pair.indexOf('=')) : pair;
      if ("h".equals(paramName)) {
        continue;
      }
      sb.append(first ? '?' : '&');
      sb.append(pair);
      first = false;
    }
    return sb.toString();
  }

  private static Map<String, String> parseQueryString(String query) {
    Map<String, String> params = new LinkedHashMap<>();
    for (String pair : query.split("&")) {
      int eqIdx = pair.indexOf('=');
      if (eqIdx > 0) {
        String key = pair.substring(0, eqIdx);
        String value = pair.substring(eqIdx + 1);
        params.put(key, value);
      }
    }
    return params;
  }
}
