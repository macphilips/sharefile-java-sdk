package io.github.indraftapp.sharefile.client.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.core.exception.HmacValidationException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/** Tests for {@link HmacValidator}. */
class HmacValidatorTest {

  private static final String CLIENT_SECRET = "test-client-secret";

  @Test
  void validHmacPassesValidation() {
    // Build a redirect URI with a valid HMAC
    String path = "/oauth/callback";
    String queryWithoutH = "code=abc123&state=xyz";
    String dataToSign = path + "?" + queryWithoutH;
    String hmac = computeHmac(dataToSign, CLIENT_SECRET);

    URI redirectUri = URI.create("https://example.com" + path + "?" + queryWithoutH + "&h=" + hmac);

    assertDoesNotThrow(() -> HmacValidator.validate(redirectUri, CLIENT_SECRET));
  }

  @Test
  void invalidHmacThrows() {
    URI redirectUri = URI.create("https://example.com/oauth/callback?code=abc123&h=invalidhmac");

    assertThrows(
        HmacValidationException.class, () -> HmacValidator.validate(redirectUri, CLIENT_SECRET));
  }

  @Test
  void missingHParameterThrows() {
    URI redirectUri = URI.create("https://example.com/oauth/callback?code=abc123&state=xyz");

    HmacValidationException ex =
        assertThrows(
            HmacValidationException.class,
            () -> HmacValidator.validate(redirectUri, CLIENT_SECRET));
    assertTrue(ex.getMessage().contains("Missing 'h' parameter"));
  }

  @Test
  void noQueryParametersThrows() {
    URI redirectUri = URI.create("https://example.com/oauth/callback");

    HmacValidationException ex =
        assertThrows(
            HmacValidationException.class,
            () -> HmacValidator.validate(redirectUri, CLIENT_SECRET));
    assertTrue(ex.getMessage().contains("no query parameters"));
  }

  @Test
  void hParameterStrippedFromSignedData() {
    // Verify that 'h' is excluded from the data being signed
    String path = "/callback";
    String queryWithoutH = "code=test&state=s1";
    String dataToSign = path + "?" + queryWithoutH;
    String hmac = computeHmac(dataToSign, CLIENT_SECRET);

    // Put 'h' in the middle of query params
    URI redirectUri =
        URI.create("https://example.com" + path + "?code=test&h=" + hmac + "&state=s1");

    assertDoesNotThrow(() -> HmacValidator.validate(redirectUri, CLIENT_SECRET));
  }

  @Test
  void computeUrlEncodedHmacProducesConsistentResults() {
    String result1 = HmacValidator.computeUrlEncodedHmac("test-data", "test-key");
    String result2 = HmacValidator.computeUrlEncodedHmac("test-data", "test-key");
    assertEquals(result1, result2);
  }

  @Test
  void differentKeysProduceDifferentHmacs() {
    String hmac1 = HmacValidator.computeUrlEncodedHmac("test-data", "key1");
    String hmac2 = HmacValidator.computeUrlEncodedHmac("test-data", "key2");
    assertNotEquals(hmac1, hmac2);
  }

  /** Helper to compute the expected HMAC for test verification. */
  private static String computeHmac(String data, String key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      String base64 = Base64.getEncoder().encodeToString(digest);
      return URLEncoder.encode(base64, StandardCharsets.UTF_8);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }
}
