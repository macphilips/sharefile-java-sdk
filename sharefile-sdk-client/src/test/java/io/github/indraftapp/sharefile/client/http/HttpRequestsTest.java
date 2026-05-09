package io.github.indraftapp.sharefile.client.http;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-06: Tests for the {@link HttpRequests} factory.
 */
class HttpRequestsTest {

    private static final URI TEST_URI = URI.create("https://example.sf-api.com/sf/v3/Items");
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Test
    void json_setsContentTypeAndBody() throws Exception {
        byte[] body = "{\"Name\":\"Test\"}".getBytes(StandardCharsets.UTF_8);
        var request = HttpRequests.json("POST", TEST_URI, body, TIMEOUT);

        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.uri()).isEqualTo(TEST_URI);
        assertThat(request.headers().get("Content-Type")).isEqualTo("application/json");
        assertThat(request.headers().get("Accept")).isEqualTo("application/json");
        assertThat(request.bodyStream()).isPresent();
        assertThat(request.contentLength()).hasValue(body.length);
        assertThat(request.timeout()).isEqualTo(TIMEOUT);

        byte[] read = request.bodyStream().get().readAllBytes();
        assertThat(read).isEqualTo(body);
    }

    @Test
    void streaming_setsBodyAndSize() {
        byte[] data = "file-data".getBytes(StandardCharsets.UTF_8);
        var stream = new ByteArrayInputStream(data);

        var request = HttpRequests.streaming("POST", TEST_URI, stream, data.length, TIMEOUT);

        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.bodyStream()).isPresent();
        assertThat(request.contentLength()).hasValue(data.length);
        assertThat(request.headers().get("Accept")).isEqualTo("application/json");
        assertThat(request.headers()).doesNotContainKey("Content-Type");
    }

    @Test
    void streaming_unknownSize_emptyContentLength() {
        var stream = new ByteArrayInputStream(new byte[0]);
        var request = HttpRequests.streaming("POST", TEST_URI, stream, -1, TIMEOUT);

        assertThat(request.contentLength()).isEmpty();
    }

    @Test
    void noBody_hasNoBodyStream() {
        var request = HttpRequests.noBody("GET", TEST_URI, TIMEOUT);

        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.uri()).isEqualTo(TEST_URI);
        assertThat(request.bodyStream()).isEmpty();
        assertThat(request.contentLength()).isEmpty();
        assertThat(request.headers().get("Accept")).isEqualTo("application/json");
    }

    @Test
    void formEncoded_setsContentType() throws Exception {
        byte[] body = "grant_type=password".getBytes(StandardCharsets.UTF_8);
        var request = HttpRequests.formEncoded("POST", TEST_URI, body, TIMEOUT);

        assertThat(request.headers().get("Content-Type")).isEqualTo("application/x-www-form-urlencoded");
        assertThat(request.bodyStream()).isPresent();
        assertThat(request.contentLength()).hasValue(body.length);

        byte[] read = request.bodyStream().get().readAllBytes();
        assertThat(read).isEqualTo(body);
    }

    @Test
    void headers_areUnmodifiable() {
        var request = HttpRequests.noBody("GET", TEST_URI, TIMEOUT);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> request.headers().put("X-Test", "val")
        ).isInstanceOf(UnsupportedOperationException.class);
    }
}
