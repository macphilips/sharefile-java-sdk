# ShareFile Java SDK — Technical Specification

**Version:** 1.0.0-SNAPSHOT
**Java:** 17+
**Build Tool:** Gradle (Kotlin DSL)
**HTTP Client:** `java.net.http.HttpClient` (core); Spring RestClient adapter (optional)
**Framework:** None required. Spring Boot 3 supported via starter.
**Date:** 2026-05-08

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture & Module Structure](#2-architecture--module-structure)
3. [ShareFileClient Builder (Pure Java)](#3-sharefileclient-builder-pure-java)
4. [HTTP Abstraction Layer](#4-http-abstraction-layer)
5. [Authentication](#5-authentication)
6. [Resource Clients](#6-resource-clients)
7. [OData Query Builder](#7-odata-query-builder)
8. [Model Layer](#8-model-layer)
9. [Error Handling & Reporting](#9-error-handling--reporting)
10. [Retry & Resilience](#10-retry--resilience)
11. [Observability, Metrics & Alerting](#11-observability-metrics--alerting)
12. [File Upload & Download](#12-file-upload--download)
13. [Pagination](#13-pagination)
14. [Spring Boot Integration](#14-spring-boot-integration)
15. [Testing Strategy](#15-testing-strategy)
16. [Security Considerations](#16-security-considerations)
17. [Dependencies](#17-dependencies)
18. [Publishing & Distribution](#18-publishing--distribution)

---

## 1. Overview

This SDK provides a type-safe, **framework-agnostic** Java client for the ShareFile REST API (v3). The initial release targets the core ShareFile API entities required for file, folder, permission, share, webhook, and async-operation workflows. Additional administrative, reporting, and enterprise entities will be added incrementally.

The core SDK runs on **pure Java 17** with zero framework dependencies. A separate `sharefile-spring-boot-starter` module provides first-class Spring Boot 3 integration with auto-configuration, Micrometer metrics, and Actuator health indicators.

### API Coverage Tiers

The ShareFile REST API exposes many entities. This SDK implements them in phased tiers:

| Tier               | Entities                                                                                                                            | Milestone | Rationale                                                                                                     |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------- | --------- | ------------------------------------------------------------------------------------------------------------- |
| **Tier 1 (v0.1)**  | Items, AsyncOperations, AccessControls, Shares, Users                                                                               | MVP       | Core file/folder operations, sharing, permissions, and async polling — covers the primary ShareFile workflows |
| **Tier 2 (v0.2)**  | Groups, WebhookSubscriptions, Sessions                                                                                              | v0.2      | Collaboration, event-driven integrations, and session management                                              |
| **Tier 3 (v0.3)**  | Accounts, Zones, StorageCenters, Capabilities                                                                                       | v0.3      | Administrative and infrastructure management                                                                  |
| **Tier 4 (v1.0+)** | Devices, Reports, FolderTemplates, Apps, Policies, Metadata, Workflows, EncryptedEmails, Favorites, ConnectorGroups, WebhookClients | Future    | Enterprise, reporting, and specialized features                                                               |

> **Note:** The architecture (SPI interfaces, HTTP abstraction, entity service pattern) is designed so that adding new entity clients is incremental — each new resource client is self-contained and does not require changes to the core SDK infrastructure.

### Design Principles

- **Framework-agnostic core:** The SDK depends only on Java 17 standard library + Jackson. No Spring, no Guice, no framework lock-in.
- **Spring as a first-class citizen, not a requirement:** The Spring Boot starter wraps the same core SDK, adding auto-configuration and observability bindings.
- **Builder over annotation:** Construction uses the builder pattern. No `@Component`, `@Service`, or `@Autowired` in the core SDK.
- **SPI for extensibility:** Credentials, observability, HTTP transport, and token storage are defined as interfaces. The core ships default implementations; Spring (or any framework) can plug in its own.
- **Type-safe:** Every API entity and request/response is a strongly-typed Java POJO.
- **Minimal surprise:** Method names mirror the API docs. `client.items().getById(id)` maps to `GET /sf/v3/Items(id)`.
- **No runtime reflection tricks:** Serialization uses Jackson with explicit configuration.

### Quick Start Examples

**1. Authorization Code OAuth (recommended for production):**

```java
// After completing the OAuth authorization code flow in your web app:
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .clientCredentials("client-id", "client-secret")
    .authorizationCode(codeFromRedirect)
    .build();

Item home = client.items().getById("home");
```

**2. Pre-existing Token (from host application's OAuth flow):**

```java
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .clientCredentials("client-id", "client-secret")
    .accessToken(oauthToken.getAccessToken(), oauthToken.getRefreshToken())
    .build();

Item home = client.items().getById("home");
```

**3. Dynamic Credentials (multi-tenant, DB, Vault):**

```java
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .credentialProvider(new DatabaseCredentialProvider(tenantId, repo, cipher))
    .build();

Item home = client.items().getById("home");
```

**4. Password Grant (legacy / service-account / internal automation only):**

> **Warning:** Password grant support exists for legacy service-account, internal automation, and non-interactive workflows where permitted by the ShareFile account configuration. New production integrations should prefer authorization-code OAuth where user authorization is required, or securely managed refresh-token-based authentication.

```java
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .clientCredentials("client-id", "client-secret")
    .passwordGrant("service-account@example.com", "password")
    .build();
```

**5. Spring Boot 3 (auto-configured):**

```yaml
# application.yml — or define a CredentialProvider bean to override
sharefile:
  subdomain: mycompany
  auth:
    client-id: ${SHAREFILE_CLIENT_ID}
    client-secret: ${SHAREFILE_CLIENT_SECRET}
    grant-type: authorization_code
    code: ${SHAREFILE_AUTH_CODE}
```

```java
@Autowired
private ShareFileClient shareFile;

Item home = shareFile.items().getById("home");
```

All five approaches use the identical `ShareFileClient` class and service API.

---

## 2. Architecture & Module Structure

### 2.1 Dependency Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Consumer Application                      │
│                                                             │
│  ┌─ Pure Java ──────────┐   ┌─ Spring Boot ──────────────┐ │
│  │                      │   │                            │ │
│  │  sharefile-sdk-client │   │ sharefile-spring-boot-     │ │
│  │  (direct usage)      │   │ starter                    │ │
│  │                      │   │ (auto-config + metrics)    │ │
│  └──────────┬───────────┘   └──────────┬─────────────────┘ │
│             │                          │                    │
│             │    ┌─────────────────────┘                    │
│             │    │                                          │
│             ▼    ▼                                          │
│  ┌──────────────────────────────────────────────┐          │
│  │           sharefile-sdk-client                │          │
│  │  (services, auth, http, transfer)            │          │
│  │  Pure Java 17 — no framework deps            │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│                     ▼                                       │
│  ┌──────────────────────────────────────────────┐          │
│  │           sharefile-sdk-core                  │          │
│  │  (models, exceptions, OData, enums)          │          │
│  │  Pure Java 17 — Jackson only                 │          │
│  └──────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Module Layout

```
sharefile-java-sdk/
├── sharefile-sdk-core/                  # Models, exceptions, OData builder
│   └── src/main/java/com/sharefile/sdk/core/
│       ├── model/                       # Entity POJOs (Item, User, Share, etc.)
│       ├── model/enums/                 # Enums (ShareType, UploadMethod, etc.)
│       ├── odata/                       # OData query builder + Filter DSL
│       ├── exception/                   # Exception hierarchy
│       └── util/                        # Shared utilities
│
├── sharefile-sdk-client/                # HTTP client, auth, services — PURE JAVA
│   └── src/main/java/com/sharefile/sdk/client/
│       ├── ShareFileClient.java         # Main entry point + builder
│       ├── ShareFileClientBuilder.java  # Fluent builder
│       ├── auth/                        # OAuth2 token manager, TokenStore SPI
│       ├── http/                        # HttpTransport SPI + JDK HttpClient impl
│       ├── resource/                    # Resource client classes (no annotations)
│       ├── transfer/                    # Upload/download pipeline, handles
│       ├── config/                      # ShareFileConfig (plain POJO)
│       ├── spi/                         # Extension point interfaces
│       │   ├── CredentialProvider.java
│       │   ├── MetricsProvider.java
│       │   ├── HttpTransport.java
│       │   └── TokenStore.java
│       └── internal/                    # Package-private implementation details
│
├── sharefile-spring-boot-starter/       # Spring Boot 3 auto-configuration
│   └── src/main/java/com/sharefile/sdk/spring/
│       ├── autoconfigure/
│       │   ├── ShareFileAutoConfiguration.java
│       │   └── ShareFileProperties.java
│       ├── metrics/
│       │   └── MicrometerMetricsProvider.java
│       ├── health/
│       │   └── ShareFileHealthIndicator.java
│       └── http/
│           └── RestClientHttpTransport.java
│
├── sharefile-sdk-bom/                   # Bill of Materials (version alignment)
├── sharefile-sdk-test/                  # WireMock test fixtures
│
├── build.gradle.kts
├── gradle.properties                    # group + version
├── settings.gradle.kts
└── docs/
    ├── TECH_SPEC.md                     # This document
    └── API_REFERENCE.md                 # ShareFile API reference
```

### 2.3 Module Dependency Graph

```
sharefile-sdk-bom (java-platform — version alignment only)

sharefile-spring-boot-starter
  ├── sharefile-sdk-client
  │     └── sharefile-sdk-core
  ├── spring-boot-starter-web           (Spring RestClient adapter)
  ├── spring-boot-starter-actuator      (health indicator)
  └── micrometer-core                   (metrics binding)

sharefile-sdk-test (testFixtures)
  └── sharefile-sdk-client
```

### 2.4 What Goes Where

| Concern                     | Module                | Framework Dependency              |
| --------------------------- | --------------------- | --------------------------------- |
| Entity POJOs, enums         | `core`                | Jackson only                      |
| Exception hierarchy         | `core`                | None                              |
| OData query builder         | `core`                | None                              |
| `ShareFileClient` + builder | `client`              | None                              |
| OAuth2 token management     | `client`              | None (`java.net.http`)            |
| Resource client classes     | `client`              | None                              |
| Upload/download pipeline    | `client`              | None (`java.util.concurrent`)     |
| HTTP transport (JDK impl)   | `client`              | None (`java.net.http.HttpClient`) |
| Retry engine                | `client`              | None                              |
| `CredentialProvider` SPI    | `client`              | None (static default)             |
| `MetricsProvider` SPI       | `client`              | None (no-op default)              |
| `TokenStore` SPI            | `client`              | None (in-memory default)          |
| Spring auto-configuration   | `spring-boot-starter` | Spring Boot 3                     |
| `application.yml` binding   | `spring-boot-starter` | Spring Boot 3                     |
| Micrometer metrics impl     | `spring-boot-starter` | Micrometer                        |
| Actuator health indicator   | `spring-boot-starter` | Spring Actuator                   |
| RestClient HTTP adapter     | `spring-boot-starter` | Spring Web 6                      |

---

## 3. ShareFileClient Builder (Pure Java)

The SDK is constructed via a fluent builder. No DI container, no annotations, no classpath scanning.

### 3.1 Builder API

```java
public final class ShareFileClient implements AutoCloseable {

    public static ShareFileClientBuilder builder() {
        return new ShareFileClientBuilder();
    }

    // Resource client accessors
    public ItemsClient items() { ... }
    public UsersClient users() { ... }
    public SharesClient shares() { ... }
    public GroupsClient groups() { ... }
    public AccountsClient accounts() { ... }
    public AccessControlsClient accessControls() { ... }
    public ZonesClient zones() { ... }
    public WebhookSubscriptionsClient webhookSubscriptions() { ... }
    public SessionsClient sessions() { ... }
    public AsyncOperationsClient asyncOperations() { ... }
    public TransferClient transfers() { ... }

    @Override
    public void close() {
        // Shuts down token refresh scheduler, transfer executor, HTTP client
    }
}
```

### 3.2 Builder Configuration

```java
public final class ShareFileClientBuilder {

    // ── Required ──
    public ShareFileClientBuilder subdomain(String subdomain) { ... }

    // ── Credentials (pick one approach) ──

    // Option A: Dynamic — resolve credentials at authentication time (multi-tenant, DB, vault)
    public ShareFileClientBuilder credentialProvider(CredentialProvider provider) { ... }

    // Option B: Static convenience — wraps values in a StaticCredentialProvider internally
    public ShareFileClientBuilder clientCredentials(String clientId, String clientSecret) { ... }
    public ShareFileClientBuilder passwordGrant(String username, String password) { ... }
    public ShareFileClientBuilder authorizationCode(String code) { ... }
    public ShareFileClientBuilder accessToken(String accessToken, String refreshToken) { ... }

    // ── Optional overrides ──
    public ShareFileClientBuilder apiControlPlane(String apicp) { ... }       // default: "sharefile.com"
    public ShareFileClientBuilder connectTimeout(Duration timeout) { ... }    // default: 10s
    public ShareFileClientBuilder readTimeout(Duration timeout) { ... }       // default: 30s
    public ShareFileClientBuilder uploadTimeout(Duration timeout) { ... }     // default: 300s
    public ShareFileClientBuilder downloadTimeout(Duration timeout) { ... }   // default: 300s

    // ── SPI extension points ──
    public ShareFileClientBuilder httpTransport(HttpTransport transport) { ... }
    public ShareFileClientBuilder tokenStore(TokenStore store) { ... }
    public ShareFileClientBuilder metricsProvider(MetricsProvider metrics) { ... }
    public ShareFileClientBuilder objectMapper(ObjectMapper mapper) { ... }
    public ShareFileClientBuilder executor(ExecutorService executor) { ... }

    // ── Retry configuration ──
    public ShareFileClientBuilder retryConfig(RetryConfig config) { ... }

    // ── Token refresh ──
    public ShareFileClientBuilder tokenRefreshBuffer(Duration buffer) { ... } // default: 5min

    // ── Build ──
    public ShareFileClient build() { ... }
}
```

### 3.3 Usage Examples

**Minimal — authorization code (recommended):**

```java
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .clientCredentials("my-client-id", "my-client-secret")
    .authorizationCode(codeFromRedirect)
    .build();
```

**Pre-existing token (host app manages OAuth):**

```java
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .clientCredentials("client-id", "client-secret")
    .accessToken(oauthToken.getAccessToken(), oauthToken.getRefreshToken())
    .build();
```

**Dynamic credentials from database (multi-tenant):**

```java
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .credentialProvider(new DatabaseCredentialProvider(tenantId, credentialRepository))
    .build();
```

**Full control:**

```java
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .apiControlPlane("securevdr.com")
    .credentialProvider(() -> {
        EncryptedCredential cred = credentialRepo.findByTenant(tenantId);
        return Credentials.builder()
            .clientCredentials(cred.decryptClientId(), cred.decryptClientSecret())
            .passwordGrant(cred.decryptUsername(), cred.decryptPassword())
            .build();
    })
    .connectTimeout(Duration.ofSeconds(15))
    .readTimeout(Duration.ofSeconds(60))
    .tokenStore(new RedisTokenStore(redisClient))
    .metricsProvider(new MicrometerMetricsProvider(meterRegistry))
    .retryConfig(RetryConfig.builder()
        .maxRetries(5)
        .backoffMultiplier(2.0)
        .build())
    .executor(Executors.newFixedThreadPool(16))
    .build();
```

**Password grant (legacy / service-account only):**

```java
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .clientCredentials("client-id", "client-secret")
    .passwordGrant("service-account@example.com", "password")
    .build();
```

### 3.4 Lifecycle

`ShareFileClient` implements `AutoCloseable`. When closed, it:

1. Cancels the token refresh scheduler.
2. Shuts down the transfer executor (awaiting in-flight transfers for 30s).
3. Closes the underlying HTTP client if the SDK owns it.

```java
try (ShareFileClient client = ShareFileClient.builder()
        .subdomain("mycompany")
        .clientCredentials("id", "secret")
        .accessToken(token, refreshToken)
        .build()) {

    Item home = client.items().getById("home");
}
// All resources cleaned up
```

If the caller provides their own `ExecutorService` or `HttpTransport`, the SDK does **not** close them — the caller retains ownership.

---

## 4. HTTP Abstraction Layer

### 4.1 HttpTransport SPI

The SDK defines a transport interface so the HTTP implementation is swappable. The interface supports **both buffered and streaming bodies** — buffered for JSON API calls, streaming for file uploads and downloads:

```java
public interface HttpTransport {

    HttpResponse execute(HttpRequest request) throws ShareFileNetworkException;

    interface HttpRequest {
        String method();
        URI uri();
        Map<String, String> headers();
        Optional<InputStream> bodyStream();   // streaming body (uploads, large payloads)
        OptionalLong contentLength();         // -1 or empty = chunked transfer encoding
        Duration timeout();
    }

    interface HttpResponse {
        int statusCode();
        Map<String, List<String>> headers();
        InputStream bodyStream();             // always streaming — caller decides how to consume
        byte[] bodyBytes();                   // convenience: reads bodyStream() fully into memory
        byte[] bodyBytes(int maxBytes);       // bounded read — throws if body exceeds limit
    }
}
```

**Design rationale:** File transfers can be large (multi-GB). The transport must never require buffering entire upload/download bodies as `byte[]`. For JSON API calls (typically < 1 MB), callers use `bodyBytes()` which reads the stream into memory. For file transfers, callers consume `bodyStream()` directly.

**Request construction helpers** (package-private, used internally):

```java
// For JSON API calls
HttpRequest jsonRequest = HttpRequests.json("POST", uri, jsonBytes, timeout);

// For file upload streaming
HttpRequest uploadRequest = HttpRequests.streaming("POST", uri, fileInputStream, fileSize, timeout);

// For GET/DELETE (no body)
HttpRequest getRequest = HttpRequests.noBody("GET", uri, timeout);
```

### 4.2 JDK HttpClient Implementation (Default)

The default transport uses `java.net.http.HttpClient`, available since Java 11:

```java
public final class JdkHttpTransport implements HttpTransport {
    private final java.net.http.HttpClient httpClient;

    public JdkHttpTransport(ShareFileConfig config) {
        this.httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(config.getConnectTimeout())
            .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
            .build();
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
        java.net.http.HttpRequest.BodyPublisher publisher = request.bodyStream()
            .map(stream -> request.contentLength().isPresent()
                ? java.net.http.HttpRequest.BodyPublishers.ofInputStream(() -> stream)
                : java.net.http.HttpRequest.BodyPublishers.ofInputStream(() -> stream))
            .orElse(java.net.http.HttpRequest.BodyPublishers.noBody());

        java.net.http.HttpRequest jdkRequest = java.net.http.HttpRequest.newBuilder()
            .uri(request.uri())
            .method(request.method(), publisher)
            .timeout(request.timeout())
            .headers(flattenHeaders(request.headers()))
            .build();

        // Stream response body — never buffer entire response in memory
        java.net.http.HttpResponse<InputStream> jdkResponse =
            httpClient.send(jdkRequest, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

        return new JdkHttpResponse(jdkResponse);
    }
}
```

### 4.3 Spring RestClient Adapter (in `spring-boot-starter`)

Spring Boot users can optionally swap in `RestClient` for consistency with their stack:

```java
// In sharefile-spring-boot-starter module
public final class RestClientHttpTransport implements HttpTransport {
    private final RestClient restClient;

    public RestClientHttpTransport(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
        // Delegates to Spring's RestClient
        // Maps streaming body (InputStream) to RestClient's request body
        // Returns streaming response — Spring interceptors, observability apply
    }
}
```

### 4.4 ShareFileHttpClient (Internal)

An internal class wraps the `HttpTransport` and adds SDK concerns:

```java
final class ShareFileHttpClient {
    private final HttpTransport transport;
    private final TokenManager tokenManager;
    private final ObjectMapper objectMapper;
    private final RetryEngine retryEngine;
    private final MetricsProvider metrics;
    private final String baseUrl;

    // Bearer token injection
    // Request ID generation (X-Request-Id)
    // JSON serialization/deserialization
    // Error response parsing → ShareFileApiException
    // Retry logic delegation
    // Metrics recording

    <T> T get(String path, Map<String, String> params, Class<T> responseType) { ... }
    <T> T post(String path, Object body, Class<T> responseType) { ... }
    <T> T patch(String path, Object body, Class<T> responseType) { ... }
    void delete(String path, Map<String, String> params) { ... }
    <T> T execute(String method, String path, Object body,
                  Map<String, String> params, Class<T> responseType) { ... }
}
```

### 4.5 Request Pipeline

```
Service method call
    │
    ▼
ShareFileHttpClient
    ├── Serialize request body (Jackson)
    ├── Build URI (baseUrl + path + OData params)
    ├── Attach Authorization: Bearer header (from TokenManager)
    ├── Attach X-Request-Id: UUID header
    ├── Record metrics start (MetricsProvider)
    │
    ▼
RetryEngine (wraps transport call)
    ├── Call HttpTransport.execute()
    ├── On 401 → CredentialProvider.resolve() + re-authenticate, retry once
    ├── On 429 → wait Retry-After, retry up to 3x
    ├── On 5xx → exponential backoff, retry up to 3x
    ├── On connection failure → retry idempotent, up to 2x
    │
    ▼
HttpTransport.execute()  ← JdkHttpTransport (default) or RestClientHttpTransport (Spring)
    │
    ▼
ShareFileHttpClient (continued)
    ├── Record metrics end (MetricsProvider)
    ├── Check status code → throw ShareFileApiException on error
    ├── Deserialize response body (Jackson)
    └── Return typed result
```

### 4.6 Request/Response Serialization

**Jackson ObjectMapper** configured with:

- `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false` (API may add fields)
- `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS = false`
- `JavaTimeModule` for `Instant`/`ZonedDateTime` handling
- `PropertyNamingStrategy.UPPER_CAMEL_CASE` (ShareFile uses PascalCase JSON)
- Custom `ODataFeedDeserializer` for unwrapping `{ "value": [...] }` collections
- Custom `ODataTypeResolver` — uses the `odata.type` field for polymorphic dispatch (e.g., `ShareFile.Api.Models.File` → `File.class`, `ShareFile.Api.Models.AsyncOperation` → `AsyncOperation.class`). Critical for the `OperationResult<T>` pattern (§8.4) and the `Item` type hierarchy

The SDK creates its own `ObjectMapper` by default. Callers can provide a custom one via the builder to share configuration with their application.

---

## 5. Authentication

### 5.1 OAuth2 Flows Supported

| Flow                   | Use Case                                                                | Builder Method                                                   | Recommendation                                 |
| ---------------------- | ----------------------------------------------------------------------- | ---------------------------------------------------------------- | ---------------------------------------------- |
| **Authorization Code** | Web applications with user interaction                                  | `.authorizationCode(code)` or via `CredentialProvider`           | **Preferred for production**                   |
| **Pre-existing Token** | Token obtained externally (e.g., host app's OAuth flow)                 | `.accessToken(token, refreshToken)`                              | Preferred when host app manages OAuth          |
| **Password Grant**     | Legacy service-accounts, internal automation, non-interactive workflows | `.passwordGrant(username, password)` or via `CredentialProvider` | Legacy — use only where account config permits |

> **Note:** Password grant requires the ShareFile account to allow direct username/password authentication. New production integrations should prefer authorization-code OAuth or externally managed token flows. Password grant is provided for backward compatibility, internal tooling, and tightly controlled service-account scenarios.

### 5.2 CredentialProvider SPI

Credentials are resolved through a `CredentialProvider` interface. This is the core abstraction that decouples the SDK from any particular credential sourcing strategy — environment variables, databases, vaults, or runtime user context.

```java
@FunctionalInterface
public interface CredentialProvider {

    Credentials resolve() throws CredentialResolutionException;
}
```

The `Credentials` record holds everything the SDK needs to authenticate:

```java
public record Credentials(
    String clientId,
    String clientSecret,
    GrantType grantType,       // PASSWORD, AUTHORIZATION_CODE
    String username,           // null unless PASSWORD grant
    String password,           // null unless PASSWORD grant
    String authorizationCode   // null unless AUTHORIZATION_CODE grant
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        public Builder clientCredentials(String clientId, String clientSecret) { ... }
        public Builder passwordGrant(String username, String password) { ... }
        public Builder authorizationCode(String code) { ... }
        public Credentials build() { ... }
    }
}
```

**Built-in implementations:**

| Implementation             | Description                                                                                                            |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `StaticCredentialProvider` | Wraps fixed values. Created internally when using `builder().clientCredentials().passwordGrant()` convenience methods. |

**`CredentialProvider` is called only during authentication** — when the SDK needs to obtain an initial access token or a new token after expiry. Once a valid access token exists, it is reused for all API calls until it expires. This means:

- Zero overhead on normal API calls (just the cached Bearer token)
- Provider is invoked only on: first request, token expiry + refresh failure, or explicit re-authentication
- Database/vault lookups happen at most once per token lifetime (typically 8 hours for ShareFile)

**Example: AES-GCM encrypted credentials from database:**

```java
public class DatabaseCredentialProvider implements CredentialProvider {
    private final String tenantId;
    private final CredentialRepository repository;
    private final AesGcmCipher cipher;

    public DatabaseCredentialProvider(String tenantId,
                                      CredentialRepository repository,
                                      AesGcmCipher cipher) {
        this.tenantId = tenantId;
        this.repository = repository;
        this.cipher = cipher;
    }

    @Override
    public Credentials resolve() {
        EncryptedCredentialEntity entity = repository.findByTenantId(tenantId)
            .orElseThrow(() -> new CredentialResolutionException(
                "No credentials found for tenant: " + tenantId));

        return Credentials.builder()
            .clientCredentials(
                cipher.decrypt(entity.getEncryptedClientId()),
                cipher.decrypt(entity.getEncryptedClientSecret()))
            .passwordGrant(
                cipher.decrypt(entity.getEncryptedUsername()),
                cipher.decrypt(entity.getEncryptedPassword()))
            .build();
    }
}
```

**Example: HashiCorp Vault:**

```java
CredentialProvider vaultProvider = () -> {
    VaultResponse response = vaultClient.read("secret/data/sharefile/" + tenantId);
    Map<String, String> data = response.getData();
    return Credentials.builder()
        .clientCredentials(data.get("clientId"), data.get("clientSecret"))
        .passwordGrant(data.get("username"), data.get("password"))
        .build();
};
```

**Example: Lambda (inline, for simple cases):**

```java
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .credentialProvider(() -> Credentials.builder()
        .clientCredentials(System.getenv("SF_CLIENT_ID"), System.getenv("SF_CLIENT_SECRET"))
        .passwordGrant(System.getenv("SF_USER"), System.getenv("SF_PASS"))
        .build())
    .build();
```

**Static convenience methods** on the builder (`clientCredentials()`, `passwordGrant()`, `authorizationCode()`) still work — they create a `StaticCredentialProvider` internally:

```java
// These two are equivalent:
builder.clientCredentials("id", "secret").passwordGrant("user", "pass")

builder.credentialProvider(new StaticCredentialProvider(
    Credentials.builder()
        .clientCredentials("id", "secret")
        .passwordGrant("user", "pass")
        .build()))
```

### 5.3 Token Management

```java
final class TokenManager implements AutoCloseable {
    private final ShareFileConfig config;
    private final CredentialProvider credentialProvider;  // resolved lazily at auth time
    private final HttpTransport transport;               // raw transport (no auth header)
    private final TokenStore tokenStore;
    private volatile OAuthToken currentToken;
    private final ReentrantReadWriteLock tokenLock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService scheduler;
    private final MetricsProvider metrics;
}
```

**Authentication flow:**

```
API call requires a valid access token
    │
    ▼
TokenManager.getAccessToken()
    │
    ├── 1. Check TokenStore for cached token
    │      └── Valid access token exists? → return it (fast path, zero overhead)
    │
    ├── 2. Access token expired, refresh token available?
    │      │
    │      ▼
    │   POST /oauth/token (refresh_token grant)
    │      Uses: client_id, client_secret, refresh_token    ← NO CredentialProvider call
    │      │
    │      ├── Success → save rotated token pair to TokenStore, schedule next refresh
    │      └── Failure → fall through to step 3
    │
    ├── 3. No token, no refresh token, or refresh failed?
    │      │
    │      ▼
    │   CredentialProvider.resolve()                        ← called ONLY here
    │      │
    │      ▼
    │   POST /oauth/token (password or authorization_code grant)
    │      │
    │      ▼
    │   Save token pair to TokenStore, schedule proactive refresh
    │      │
    │      ▼
    │   Return new access token
    │
    └── 4. Proactive refresh (80% of expires_in elapsed, background)
           │
           ▼
        POST /oauth/token (refresh_token grant)             ← NO CredentialProvider call
           │
           ├── Success → save rotated token, reschedule
           └── Failure → CredentialProvider.resolve() + full re-auth (step 3)
```

The key distinction: **refresh-token grant only needs `client_id`, `client_secret`, and `refresh_token`** — it does not require username/password or an authorization code. `CredentialProvider.resolve()` is called only for initial authentication or as a last-resort fallback when refresh fails entirely. This means:

- Normal token refresh cycles never hit your credential store (DB, Vault, etc.)
- `CredentialProvider` is called at most once per session under normal operation
- Credential store outages don't affect the SDK as long as refresh tokens remain valid

To support this, `TokenManager` holds client credentials separately from the full credential provider:

```java
final class TokenManager implements AutoCloseable {
    private final ShareFileConfig config;
    private final String clientId;                        // extracted once at build time
    private final String clientSecret;                    // extracted once at build time
    private final CredentialProvider credentialProvider;   // full re-auth fallback only
    private final HttpTransport transport;
    private final TokenStore tokenStore;
    private volatile OAuthToken currentToken;
    private final ReentrantReadWriteLock tokenLock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService scheduler;
    private final MetricsProvider metrics;
}
```

**Key behaviors:**

- **Thread-safe:** `ReentrantReadWriteLock` protects token reads/writes. Concurrent requests share the same valid token.
- **Refresh does not require CredentialProvider:** Token refresh uses the stored refresh token + client credentials. No call to `CredentialProvider.resolve()` unless refresh fails.
- **CredentialProvider as last resort:** Only called for initial authentication (step 3) or when refresh-token grant fails and a full re-authentication is needed.
- **Proactive refresh:** A `ScheduledExecutorService` refreshes the token when 80% of `expires_in` has elapsed, avoiding request-time latency.
- **Retry on refresh failure:** Up to 3 retries with exponential backoff (1s, 2s, 4s) before throwing `ShareFileAuthenticationException`.
- **Token persistence SPI:** In-memory by default. Pluggable via `TokenStore` interface.

```java
public interface TokenStore {
    void save(OAuthToken token);
    Optional<OAuthToken> load();
    void clear();
}
```

Built-in implementations:

- `InMemoryTokenStore` (default)
- Spring starter adds: property-driven selection with extensibility for Redis, JDBC, etc.

### 5.4 Token Endpoint

```
POST https://{subdomain}.{apicp}/oauth/token
Content-Type: application/x-www-form-urlencoded
```

| Grant Type           | Parameters                                                         |
| -------------------- | ------------------------------------------------------------------ |
| `authorization_code` | `grant_type`, `code`, `client_id`, `client_secret`                 |
| `password`           | `grant_type`, `username`, `password`, `client_id`, `client_secret` |
| `refresh_token`      | `grant_type`, `refresh_token`, `client_id`, `client_secret`        |

### 5.5 HMAC Validation (Authorization Code Flow)

The SDK validates the `h` parameter returned during the authorization code redirect:

1. Strip the `h` query parameter from the redirect URI.
2. Extract path + remaining query string.
3. Compute HMAC-SHA256 using the client secret.
4. Base64-encode, then URL-encode the digest.
5. Compare against the received `h` value.
6. Reject if validation fails, throwing `HmacValidationException`.

---

## 6. Resource Clients

Each API resource gets a dedicated client class with **explicit, resource-specific methods**. ShareFile resources are not uniform enough for a generic CRUD base class — resources use composite keys (AccessControls), action-style sub-endpoints (Items/Copy, Items/Upload), nested navigation (Shares/Recipients), and polymorphic responses (OperationResult). The public API must be explicit per resource.

**No framework annotations** — resource clients are plain Java classes, constructed internally by the builder.

### 6.1 Internal Shared Infrastructure

While the public API is explicit, resource clients share internal helpers for common concerns:

```java
// Package-private — not part of the public API
final class ResourceRequestExecutor {
    private final ShareFileHttpClient httpClient;
    private final String basePath;  // e.g., "/Items"

    // URI construction
    URI entityUri(String id) { ... }                        // /Items({id})
    URI entityActionUri(String id, String action) { ... }   // /Items({id})/Copy
    URI collectionUri() { ... }                             // /Items
    URI compositeKeyUri(String... keys) { ... }             // /AccessControls(principalid=x,itemid=y)

    // Typed request execution
    <T> T get(URI uri, ODataQuery query, Class<T> type) { ... }
    <T> T get(URI uri, ODataQuery query, TypeReference<T> type) { ... }
    <T> T post(URI uri, Object body, Class<T> type) { ... }
    <T> T patch(URI uri, Object body, Class<T> type) { ... }
    void delete(URI uri) { ... }

    // OData feed handling
    <T> ODataFeed<T> getCollection(URI uri, ODataQuery query, TypeReference<ODataFeed<T>> type) { ... }
    <T> ODataFeed<T> getNextPage(ODataFeed<T> current, TypeReference<ODataFeed<T>> type) { ... }
}
```

This ensures consistent URI construction, OData handling, request execution, and response parsing — without exposing unsafe generic CRUD methods publicly.

### 6.2 Resource Client Registry

| Resource Client              | Base Path               | Primary Type          | Tier |
| ---------------------------- | ----------------------- | --------------------- | ---- |
| `ItemsClient`                | `/Items`                | `Item`                | 1    |
| `AsyncOperationsClient`      | `/AsyncOperations`      | `AsyncOperation`      | 1    |
| `AccessControlsClient`       | `/AccessControls`       | `AccessControl`       | 1    |
| `SharesClient`               | `/Shares`               | `Share`               | 1    |
| `UsersClient`                | `/Users`                | `User`                | 1    |
| `GroupsClient`               | `/Groups`               | `Group`               | 2    |
| `WebhookSubscriptionsClient` | `/WebhookSubscriptions` | `WebhookSubscription` | 2    |
| `SessionsClient`             | `/Sessions`             | `Session`             | 2    |
| `AccountsClient`             | `/Accounts`             | `Account`             | 3    |
| `ZonesClient`                | `/Zones`                | `Zone`                | 3    |

### 6.3 ItemsClient

```java
public final class ItemsClient {
    private final ResourceRequestExecutor executor;

    // ── Navigation ──
    public Item getById(String id) { ... }
    public Item getById(String id, ODataQuery query) { ... }
    public Item getByPath(String path) { ... }
    public Item getByPath(String rootId, String relativePath) { ... }
    public Item getParent(String id) { ... }
    public ODataFeed<Item> getChildren(String id) { ... }
    public ODataFeed<Item> getChildren(String id, ODataQuery query) { ... }
    public ODataFeed<Item> getBreadcrumbs(String id) { ... }
    public ODataFeed<Item> getDeletedChildren(String parentId) { ... }
    public ODataFeed<Item> getVersions(String id) { ... }
    public ItemInfo getFolderAccessInfo(String id) { ... }
    public Redirection getThumbnail(String id, int size) { ... }

    // ── Search ──
    public SearchResults search(String query, Integer maxResults, Integer skip) { ... }
    public SearchResults search(String folderId, String query, Integer maxResults, Integer skip) { ... }
    public AdvancedSearchResults advancedSearch(AdvancedSearchRequest request) { ... }

    // ── Create ──
    public Item createFolder(String parentId, FolderCreateRequest request) { ... }
    public Item createNote(String parentId, NoteCreateRequest request) { ... }
    public Item createLink(String parentId, LinkCreateRequest request) { ... }

    // ── Update (may return AsyncOperation for cross-zone moves) ──
    public OperationResult<Item> update(String id, Item item) { ... }

    // ── Copy (may return AsyncOperation for cross-zone copies) ──
    public OperationResult<Item> copy(String id, String targetFolderId, boolean overwrite) { ... }

    // ── Delete / Restore ──
    public void delete(String id) { ... }
    public void bulkDelete(String parentId, List<String> itemIds, boolean permanently) { ... }
    public void bulkRestore(List<String> itemIds) { ... }

    // ── Check In / Check Out ──
    public Item checkOut(String id) { ... }
    public Item checkIn(String id, String message) { ... }
    public void discardCheckOut(String id) { ... }

    // ── Transfer convenience (delegates to TransferClient §12) ──
    public UploadResult upload(String folderId, Path file, UploadOptions options) { ... }
    public void download(String itemId, Path target, DownloadOptions options) { ... }

    // ── Pagination helpers ──
    public Iterable<Item> listAllChildren(String folderId) { ... }
    public Stream<Item> streamAllChildren(String folderId) { ... }
}
```

### 6.4 AccessControlsClient

AccessControls use **composite keys** (`principalId` + `itemId`), not simple string IDs. The public API reflects this explicitly:

```java
public final class AccessControlsClient {
    private final ResourceRequestExecutor executor;

    // ── Read ──
    public AccessControl getById(String principalId, String itemId) { ... }
    public ODataFeed<AccessControl> getByItem(String itemId) { ... }

    // ── Create / Update (may return AsyncOperation when recursive=true) ──
    public OperationResult<AccessControl> create(
            String itemId, AccessControl acl, boolean recursive) { ... }
    public OperationResult<AccessControl> update(
            String itemId, AccessControl acl, boolean recursive) { ... }

    // ── Delete ──
    public void delete(String principalId, String itemId) { ... }

    // ── Bulk operations ──
    public AccessControlBulkResult bulkSet(
            String itemId, BulkAccessControlRequest request) { ... }
    public AccessControlBulkResult bulkSetForPrincipal(
            String principalId, BulkAccessControlRequest request) { ... }
    public void clone(String folderId, String principalId, List<String> clonePrincipalIds) { ... }
    public void bulkDelete(String itemId, List<String> principalIds) { ... }
    public void bulkDeleteForPrincipal(String principalId, List<String> folderIds) { ... }

    // ── Notifications ──
    public void notifyUsers(String itemId, List<String> userIds, String message) { ... }
}
```

### 6.5 AsyncOperationsClient

```java
public final class AsyncOperationsClient {
    private final ResourceRequestExecutor executor;

    public AsyncOperation getById(String operationId) { ... }
    public ODataFeed<AsyncOperation> list() { ... }
    public ODataFeed<AsyncOperation> list(ODataQuery query) { ... }

    // ── Polling helper ──
    public AsyncOperation awaitCompletion(String operationId, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            AsyncOperation op = getById(operationId);
            if (op.isTerminal()) return op;
            Thread.sleep(pollInterval.toMillis());
        }
        throw new ShareFileTimeoutException("AsyncOperation %s did not complete within %s"
            .formatted(operationId, timeout));
    }
}
```

### 6.6 SharesClient

```java
public final class SharesClient {
    private final ResourceRequestExecutor executor;

    public Share getById(String id) { ... }
    public Share getById(String id, ODataQuery query) { ... }
    public ODataFeed<Share> list() { ... }
    public ODataFeed<Share> list(ODataQuery query) { ... }

    // ── Create (explicit share types, not generic create) ──
    public Share createSendShare(SendShareRequest request) { ... }
    public Share createRequestShare(RequestShareRequest request) { ... }

    // ── Update / Delete ──
    public Share update(String id, Share share) { ... }
    public void delete(String id) { ... }

    // ── Share-specific operations ──
    public ODataFeed<Share> getByUser(String userId) { ... }
    public ODataFeed<Contact> getRecipients(String shareId) { ... }
    public void sendNotification(String shareId, ShareNotificationRequest request) { ... }

    // ── Share items ──
    public ODataFeed<Item> getItems(String shareId) { ... }
    public DownloadSpecification downloadItems(String shareId) { ... }
}
```

### 6.7 UsersClient

````java
public final class UsersClient {
    private final ResourceRequestExecutor executor;

    public User getById(String id) { ... }
    public User getById(String id, ODataQuery query) { ... }
    public ODataFeed<User> list() { ... }
    public ODataFeed<User> list(ODataQuery query) { ... }

    // ── Current user ──
    public User getCurrentUser() { ... }

    // ── Lookup ──
    public User getByEmail(String email) { ... }

    // ── CRUD ──
    public User create(User user) { ... }
    public User update(String id, User user) { ... }
    public void delete(String id) { ... }

    // ── User-specific operations ──
    public UserPreferences getPreferences(String id) { ... }
    public UserPreferences updatePreferences(String id, UserPreferences prefs) { ... }
    public UserSecurity getSecurity(String id) { ... }
    public UserSecurity updateSecurity(String id, UserSecurity security) { ... }
    public void resetPassword(String id) { ... }
    public void sendWelcomeEmail(String id) { ... }
    public ODataFeed<Group> getGroups(String userId) { ... }
}

### 6.8 Endpoint Coverage Matrices

These tables define the exact HTTP method, endpoint, request/response types for each SDK method. Use as the source of truth for implementation and testing.

#### Items (Tier 1)

| SDK Method | HTTP | Endpoint | Request | Response | Notes |
|---|---|---|---|---|---|
| `items().getById(id)` | GET | `/sf/v3/Items({id})` | — | `Item` | Supports special IDs: `home`, `favorites`, `allshared`, `connectors`, `box`, `top` |
| `items().getById(id, query)` | GET | `/sf/v3/Items({id})?$select=...` | — | `Item` | OData query params appended |
| `items().getByPath(path)` | GET | `/sf/v3/Items/ByPath?path={path}` | — | `Item` | URL-encoded path |
| `items().getByPath(rootId, path)` | GET | `/sf/v3/Items({rootId})/ByPath?path={path}` | — | `Item` | Relative to root |
| `items().getParent(id)` | GET | `/sf/v3/Items({id})/Parent` | — | `Item` | |
| `items().getChildren(id)` | GET | `/sf/v3/Items({id})/Children` | — | `ODataFeed<Item>` | May redirect for symbolic links |
| `items().getChildren(id, query)` | GET | `/sf/v3/Items({id})/Children?$...` | — | `ODataFeed<Item>` | Supports $top, $skip, $filter |
| `items().getBreadcrumbs(id)` | GET | `/sf/v3/Items({id})/Breadcrumbs` | — | `ODataFeed<Item>` | |
| `items().getDeletedChildren(id)` | GET | `/sf/v3/Items({id})/DeletedChildren` | — | `ODataFeed<Item>` | |
| `items().getVersions(id)` | GET | `/sf/v3/Items({id})/Versions` | — | `ODataFeed<Item>` | File version history |
| `items().getFolderAccessInfo(id)` | GET | `/sf/v3/Items({id})/AccessInfo` | — | `ItemInfo` | |
| `items().getThumbnail(id, size)` | GET | `/sf/v3/Items({id})/Thumbnail?size={s}` | — | `Redirection` | Redirect URL to thumbnail |
| `items().search(query, max, skip)` | GET | `/sf/v3/Items/Search?q={q}&$top=...` | — | `SearchResults` | Global search |
| `items().search(folderId, q, max, skip)` | GET | `/sf/v3/Items({id})/Search?q=...` | — | `SearchResults` | Scoped search |
| `items().advancedSearch(request)` | POST | `/sf/v3/Items/AdvancedSearch` | `AdvancedSearchRequest` | `AdvancedSearchResults` | |
| `items().createFolder(parentId, req)` | POST | `/sf/v3/Items({parentId})/Folder` | `FolderCreateRequest` | `Item` (Folder) | |
| `items().createNote(parentId, req)` | POST | `/sf/v3/Items({parentId})/Note` | `NoteCreateRequest` | `Item` (Note) | |
| `items().createLink(parentId, req)` | POST | `/sf/v3/Items({parentId})/Link` | `LinkCreateRequest` | `Item` (Link) | |
| `items().update(id, item)` | PATCH | `/sf/v3/Items({id})` | `Item` | `OperationResult<Item>` | **Async** if cross-zone move |
| `items().copy(id, targetId, overwrite)` | POST | `/sf/v3/Items({id})/Copy?targetid={t}&overwrite={o}` | — | `OperationResult<Item>` | **Async** if cross-zone |
| `items().delete(id)` | DELETE | `/sf/v3/Items({id})` | — | — | Single item delete |
| `items().bulkDelete(parentId, ids, perm)` | POST | `/sf/v3/Items({parentId})/BulkDelete` | `BulkDeleteRequest` | — | |
| `items().bulkRestore(ids)` | POST | `/sf/v3/Items/BulkRestore` | `BulkRestoreRequest` | — | |
| `items().checkOut(id)` | POST | `/sf/v3/Items({id})/CheckOut` | — | `Item` | |
| `items().checkIn(id, message)` | POST | `/sf/v3/Items({id})/CheckIn` | `CheckInRequest` | `Item` | |
| `items().discardCheckOut(id)` | POST | `/sf/v3/Items({id})/DiscardCheckOut` | — | — | |

#### AsyncOperations (Tier 1)

| SDK Method | HTTP | Endpoint | Request | Response | Notes |
|---|---|---|---|---|---|
| `asyncOperations().getById(id)` | GET | `/sf/v3/AsyncOperations({id})` | — | `AsyncOperation` | |
| `asyncOperations().list()` | GET | `/sf/v3/AsyncOperations` | — | `ODataFeed<AsyncOperation>` | |
| `asyncOperations().list(query)` | GET | `/sf/v3/AsyncOperations?$...` | — | `ODataFeed<AsyncOperation>` | |
| `asyncOperations().awaitCompletion(id, timeout)` | GET (poll) | `/sf/v3/AsyncOperations({id})` | — | `AsyncOperation` | SDK-side polling loop |

#### AccessControls (Tier 1)

| SDK Method | HTTP | Endpoint | Request | Response | Notes |
|---|---|---|---|---|---|
| `accessControls().getById(principalId, itemId)` | GET | `/sf/v3/AccessControls(principalid={p},itemid={i})` | — | `AccessControl` | Composite key |
| `accessControls().getByItem(itemId)` | GET | `/sf/v3/Items({itemId})/AccessControls` | — | `ODataFeed<AccessControl>` | Via Items endpoint |
| `accessControls().create(itemId, acl, recursive)` | POST | `/sf/v3/Items({itemId})/AccessControls?recursive={r}` | `AccessControl` | `OperationResult<AccessControl>` | **Async** if recursive=true |
| `accessControls().update(itemId, acl, recursive)` | PATCH | `/sf/v3/Items({itemId})/AccessControls?recursive={r}` | `AccessControl` | `OperationResult<AccessControl>` | **Async** if recursive=true |
| `accessControls().delete(principalId, itemId)` | DELETE | `/sf/v3/AccessControls(principalid={p},itemid={i})` | — | — | |
| `accessControls().bulkSet(itemId, req)` | POST | `/sf/v3/Items({itemId})/AccessControls/BulkSet` | `BulkAccessControlRequest` | `AccessControlBulkResult` | |
| `accessControls().bulkSetForPrincipal(principalId, req)` | POST | `/sf/v3/AccessControls({principalId})/BulkSet` | `BulkAccessControlRequest` | `AccessControlBulkResult` | |
| `accessControls().bulkDelete(itemId, principalIds)` | POST | `/sf/v3/Items({itemId})/AccessControls/BulkDelete` | `BulkDeleteRequest` | — | |
| `accessControls().clone(folderId, principalId, targets)` | POST | `/sf/v3/AccessControls({principalId})/Clone` | `CloneRequest` | — | |
| `accessControls().notifyUsers(itemId, userIds, msg)` | POST | `/sf/v3/Items({itemId})/AccessControls/Notify` | `NotifyRequest` | — | |

#### Shares (Tier 1)

| SDK Method | HTTP | Endpoint | Request | Response | Notes |
|---|---|---|---|---|---|
| `shares().getById(id)` | GET | `/sf/v3/Shares({id})` | — | `Share` | |
| `shares().getById(id, query)` | GET | `/sf/v3/Shares({id})?$...` | — | `Share` | |
| `shares().list()` | GET | `/sf/v3/Shares` | — | `ODataFeed<Share>` | |
| `shares().list(query)` | GET | `/sf/v3/Shares?$...` | — | `ODataFeed<Share>` | |
| `shares().createSendShare(req)` | POST | `/sf/v3/Shares` | `SendShareRequest` | `Share` | ShareType=Send |
| `shares().createRequestShare(req)` | POST | `/sf/v3/Shares` | `RequestShareRequest` | `Share` | ShareType=Request |
| `shares().update(id, share)` | PATCH | `/sf/v3/Shares({id})` | `Share` | `Share` | |
| `shares().delete(id)` | DELETE | `/sf/v3/Shares({id})` | — | — | |
| `shares().getByUser(userId)` | GET | `/sf/v3/Users({userId})/Shares` | — | `ODataFeed<Share>` | |
| `shares().getRecipients(shareId)` | GET | `/sf/v3/Shares({id})/Recipients` | — | `ODataFeed<Contact>` | |
| `shares().sendNotification(id, req)` | POST | `/sf/v3/Shares({id})/Notify` | `ShareNotificationRequest` | — | |
| `shares().getItems(shareId)` | GET | `/sf/v3/Shares({id})/Items` | — | `ODataFeed<Item>` | |
| `shares().downloadItems(shareId)` | GET | `/sf/v3/Shares({id})/Download` | — | `DownloadSpecification` | |

#### Users (Tier 1)

| SDK Method | HTTP | Endpoint | Request | Response | Notes |
|---|---|---|---|---|---|
| `users().getById(id)` | GET | `/sf/v3/Users({id})` | — | `User` | |
| `users().list()` | GET | `/sf/v3/Users` | — | `ODataFeed<User>` | |
| `users().list(query)` | GET | `/sf/v3/Users?$...` | — | `ODataFeed<User>` | |
| `users().getCurrentUser()` | GET | `/sf/v3/Users` | — | `User` | Returns authenticated user |
| `users().getByEmail(email)` | GET | `/sf/v3/Users?$filter=Email eq '{e}'` | — | `ODataFeed<User>` | Filter-based lookup |
| `users().create(user)` | POST | `/sf/v3/Users` | `User` | `User` | |
| `users().update(id, user)` | PATCH | `/sf/v3/Users({id})` | `User` | `User` | |
| `users().delete(id)` | DELETE | `/sf/v3/Users({id})` | — | — | |
| `users().getPreferences(id)` | GET | `/sf/v3/Users({id})/Preferences` | — | `UserPreferences` | |
| `users().updatePreferences(id, prefs)` | PATCH | `/sf/v3/Users({id})/Preferences` | `UserPreferences` | `UserPreferences` | |
| `users().getSecurity(id)` | GET | `/sf/v3/Users({id})/Security` | — | `UserSecurity` | |
| `users().updateSecurity(id, sec)` | PATCH | `/sf/v3/Users({id})/Security` | `UserSecurity` | `UserSecurity` | |
| `users().resetPassword(id)` | POST | `/sf/v3/Users({id})/ResetPassword` | — | — | |
| `users().sendWelcomeEmail(id)` | POST | `/sf/v3/Users({id})/ResendWelcome` | — | — | |
| `users().getGroups(userId)` | GET | `/sf/v3/Users({id})/Groups` | — | `ODataFeed<Group>` | |

#### Groups (Tier 2)

| SDK Method | HTTP | Endpoint | Request | Response | Notes |
|---|---|---|---|---|---|
| `groups().getById(id)` | GET | `/sf/v3/Groups({id})` | — | `Group` | |
| `groups().list()` | GET | `/sf/v3/Groups` | — | `ODataFeed<Group>` | |
| `groups().create(group)` | POST | `/sf/v3/Groups` | `Group` | `Group` | |
| `groups().update(id, group)` | PATCH | `/sf/v3/Groups({id})` | `Group` | `Group` | |
| `groups().delete(id)` | DELETE | `/sf/v3/Groups({id})` | — | — | |
| `groups().getMembers(id)` | GET | `/sf/v3/Groups({id})/Contacts` | — | `ODataFeed<Contact>` | |
| `groups().addMember(groupId, userId)` | POST | `/sf/v3/Groups({id})/Contacts` | `Contact` | `Contact` | |
| `groups().removeMember(groupId, userId)` | DELETE | `/sf/v3/Groups({gid})/Contacts({uid})` | — | — | |

#### WebhookSubscriptions (Tier 2)

| SDK Method | HTTP | Endpoint | Request | Response | Notes |
|---|---|---|---|---|---|
| `webhookSubscriptions().getById(id)` | GET | `/sf/v3/WebhookSubscriptions({id})` | — | `WebhookSubscription` | |
| `webhookSubscriptions().list()` | GET | `/sf/v3/WebhookSubscriptions` | — | `ODataFeed<WebhookSubscription>` | |
| `webhookSubscriptions().create(sub)` | POST | `/sf/v3/WebhookSubscriptions` | `WebhookSubscription` | `WebhookSubscription` | |
| `webhookSubscriptions().delete(id)` | DELETE | `/sf/v3/WebhookSubscriptions({id})` | — | — | |

#### File Transfers (Tier 1 — via TransferClient)

| SDK Method | HTTP | Endpoint | Request | Response | Notes |
|---|---|---|---|---|---|
| `transfers().upload(folderId, file, opts)` | POST | `/sf/v3/Items({folderId})/Upload2` | `UploadRequestParams` | `UploadSpecification` | Phase 1: negotiate |
| *(internal: phase 2)* | POST | `{ChunkUri}` | File bytes (stream) | — | Unauthenticated, to storage zone |
| *(internal: phase 3)* | POST | `{FinishUri}` | — | `UploadResult` | Threaded uploads only |
| `transfers().download(itemId, target, opts)` | GET | `/sf/v3/Items({id})/Download?redirect=false` | — | `DownloadSpecification` | Phase 1: resolve URL |
| *(internal: phase 2)* | GET | `{DownloadUrl}` | — | File bytes (stream) | Unauthenticated, from storage zone |
| `transfers().resolveDownloadUrl(itemId)` | GET | `/sf/v3/Items({id})/Download?redirect=false` | — | `DownloadSpecification` | URL only, no streaming |
| `transfers().uploadToShare(shareId, file, opts)` | POST | `/sf/v3/Shares({shareId})/Upload2` | `UploadRequestParams` | `UploadSpecification` | |

---

## 7. OData Query Builder

Lives in `sharefile-sdk-core` — zero dependencies beyond Java standard library.

### 7.1 Fluent API

```java
ODataQuery query = ODataQuery.builder()
    .select("Name", "Email", "CreationDate")
    .expand("Parent", "Zone")
    .expandAll()                              // $expand=*
    .filter("IsHidden eq false")
    .filter(Filter.eq("Name", "Reports"))     // type-safe alternative
    .filter(Filter.and(
        Filter.eq("IsHidden", false),
        Filter.substringOf("Name", "report")
    ))
    .orderBy("CreationDate", SortDirection.DESC)
    .top(100)
    .skip(0)
    .build();

// Serializes to: ?$select=Name,Email,CreationDate&$expand=Parent,Zone&$filter=...&$orderby=CreationDate desc&$top=100&$skip=0
````

### 7.2 Filter Builder

```java
public final class Filter {
    public static Filter eq(String field, Object value) { ... }
    public static Filter ne(String field, Object value) { ... }
    public static Filter gt(String field, Object value) { ... }
    public static Filter lt(String field, Object value) { ... }
    public static Filter substringOf(String field, String value) { ... }
    public static Filter startsWith(String field, String value) { ... }
    public static Filter endsWith(String field, String value) { ... }
    public static Filter isOf(String type) { ... }
    public static Filter and(Filter... filters) { ... }
    public static Filter or(Filter... filters) { ... }
}
```

---

## 8. Model Layer

Lives in `sharefile-sdk-core`. Depends on Jackson annotations only.

### 8.1 Base Entity

```java
public abstract class ODataEntity {
    @JsonProperty("odata.metadata")
    private String metadata;

    @JsonProperty("odata.type")
    private String type;

    @JsonProperty("Id")
    private String id;

    @JsonProperty("url")
    private String url;
}
```

### 8.2 OData Feed (Collection Wrapper)

```java
public class ODataFeed<T> implements Iterable<T> {
    @JsonProperty("odata.count")
    private Integer count;

    @JsonProperty("odata.nextLink")
    private String nextLink;

    @JsonProperty("value")
    private List<T> value;

    public boolean hasNextPage() { return nextLink != null; }
    public List<T> getItems() { return value; }
}
```

### 8.3 Entity Type Hierarchy

```
ODataEntity
├── Item
│   ├── File
│   ├── Folder
│   ├── Note
│   ├── Link
│   └── SymbolicLink
├── User
│   └── AccountUser
├── Share
├── Group
├── Account
├── AccessControl
├── Zone
├── Device
├── DeviceUser
├── WebhookSubscription
├── AsyncOperation
├── Contact
├── Favorite
├── Metadata
└── ... (supporting types)
```

### 8.4 Async-Polymorphic Responses (`OperationResult<T>`)

Four ShareFile endpoints can return **either** the expected entity **or** an `AsyncOperation`:

| Endpoint                            | Async Trigger                                            |
| ----------------------------------- | -------------------------------------------------------- |
| `PATCH /Items({id})`                | Item's Zone or Parent Zone is modified (cross-zone move) |
| `POST /Items({id})/Copy`            | Target folder is in a different zone                     |
| `POST /Items({id})/AccessControls`  | Recursive permission application                         |
| `PATCH /Items({id})/AccessControls` | Recursive permission update                              |

```java
public sealed interface OperationResult<T> {

    record Completed<T>(T entity) implements OperationResult<T> {}
    record Pending<T>(AsyncOperation operation) implements OperationResult<T> {}

    default boolean isAsync() { return this instanceof Pending; }

    default T getEntityOrThrow() {
        return switch (this) {
            case Completed<T> c -> c.entity();
            case Pending<T> p -> throw new ShareFileAsyncOperationException(
                "Operation returned AsyncOperation (id=%s). Poll via asyncOperations().awaitCompletion()."
                    .formatted(p.operation().getId()),
                p.operation());
        };
    }

    default Optional<AsyncOperation> asyncOperation() {
        return this instanceof Pending<T> p ? Optional.of(p.operation()) : Optional.empty();
    }
}
```

**Deserialization:** The response JSON `odata.type` field is inspected before binding. If it contains `AsyncOperation`, the body is deserialized as `AsyncOperation` and wrapped in `Pending`; otherwise deserialized as the expected entity and wrapped in `Completed`.

**Usage:**

```java
OperationResult<Item> result = client.items().copy(itemId, targetFolderId, false);

switch (result) {
    case OperationResult.Completed<Item> c ->
        log.info("Copy completed: {}", c.entity().getName());
    case OperationResult.Pending<Item> p -> {
        log.info("Cross-zone copy started: {}", p.operation().getId());
        client.asyncOperations().awaitCompletion(p.operation().getId(), Duration.ofMinutes(5));
    }
}

// Or when async is unexpected:
Item copied = client.items().copy(itemId, targetFolderId, false).getEntityOrThrow();
```

**Polling helper:**

```java
// On AsyncOperationsClient — see §6.5 for full definition
AsyncOperation result = client.asyncOperations().awaitCompletion(opId, Duration.ofMinutes(5));
```

### 8.5 Enums

```java
public enum ShareType { Send, Request }
public enum UploadMethod { Standard, Streamed, Threaded }
public enum TreeMode { Copy, Move, Manage }
public enum ItemOrderingMode { FoldersFirst, DateDesc, DateAsc, NameAsc, NameDesc }
public enum DlpStatus { Unscanned, ScannedOK, ScannedRejected }
public enum PreviewStatus { None, Available, Unavailable }
public enum ZoneService { StorageZone, SharePoint, NetworkShareConnector }
```

---

## 9. Error Handling & Reporting

### 9.1 Exception Hierarchy

All exceptions are unchecked and live in `sharefile-sdk-core`:

```
ShareFileException (unchecked, base)
├── ShareFileApiException                    # API returned an error response
│   ├── ShareFileNotFoundException           # 404
│   ├── ShareFileConflictException           # 409
│   ├── ShareFileForbiddenException          # 403
│   ├── ShareFileUnauthorizedException       # 401
│   ├── ShareFileRateLimitException          # 429
│   ├── ShareFileBadRequestException         # 400
│   └── ShareFileServerException             # 5xx
├── ShareFileAuthenticationException         # OAuth2 token acquisition/refresh failure
├── ShareFileNetworkException                # Connection timeout, DNS, TLS errors
├── ShareFileSerializationException          # JSON parse/serialize failure
├── ShareFileAsyncOperationException         # Expected entity but got AsyncOperation (§8.4)
├── ShareFileTimeoutException                # Async polling timeout
└── ShareFileTransferException               # Base for upload/download errors (§12.15)
    ├── ShareFileUploadException             # Upload failed
    │   ├── ShareFileUploadNegotiationException    # Phase 1 failure
    │   ├── ShareFileChunkUploadException          # Chunk failed after retries
    │   └── ShareFileUploadFinalizationException   # Phase 3 failure
    ├── ShareFileDownloadException           # Download failed
    │   ├── ShareFileDownloadUrlExpiredException   # URL timed out
    │   └── ShareFileDownloadWriteException        # Disk write failure
    └── ShareFileTransferCancelledException  # Cooperative cancellation
```

### 9.2 Error Response Parsing

ShareFile returns errors as:

```json
{
  "code": "NotFound",
  "message": { "lang": "en-US", "value": "The item was not found." }
}
```

Parsed into:

```java
public class ShareFileApiException extends ShareFileException {
    private final int httpStatus;
    private final String errorCode;
    private final String errorMessage;
    private final String requestId;       // X-Request-Id
    private final String requestMethod;
    private final String requestUri;
}
```

### 9.3 Error Handler

```java
// In ShareFileHttpClient (internal)
private void handleErrorResponse(int status, byte[] body, RequestContext context) {
    ShareFileErrorBody errorBody = parseErrorBody(body);

    throw switch (status) {
        case 400 -> new ShareFileBadRequestException(errorBody, context);
        case 401 -> new ShareFileUnauthorizedException(errorBody, context);
        case 403 -> new ShareFileForbiddenException(errorBody, context);
        case 404 -> new ShareFileNotFoundException(errorBody, context);
        case 409 -> new ShareFileConflictException(errorBody, context);
        case 429 -> new ShareFileRateLimitException(errorBody, context,
                        parseRetryAfter(responseHeaders));
        default  -> {
            if (status >= 500) yield new ShareFileServerException(errorBody, context);
            else yield new ShareFileApiException(status, errorBody, context);
        }
    };
}
```

### 9.4 Error Context Enrichment

Every exception includes:

- **Request ID** (`X-Request-Id`) for correlating with server-side logs
- **Request method + URI** for identifying which call failed
- **Elapsed time** since request start
- **Retry count** if the error occurred after retries

---

## 10. Retry & Resilience

The retry engine is built into `sharefile-sdk-client` with zero external dependencies.

### 10.1 RetryConfig

```java
public final class RetryConfig {
    private final int maxRetries;             // default: 3
    private final Duration initialBackoff;     // default: 1s
    private final double backoffMultiplier;    // default: 2.0
    private final double jitterFactor;         // default: 0.2 (±20%)
    private final Set<Integer> retryableStatuses;  // default: {429, 500, 502, 503, 504}
    private final boolean retryOnConnectionFailure; // default: true

    public static RetryConfig defaults() { ... }
    public static Builder builder() { ... }
}
```

### 10.2 Retry Policy

| Condition           | Retries | Backoff                               | Strategy                      |
| ------------------- | ------- | ------------------------------------- | ----------------------------- |
| 429 Rate Limited    | 3       | `Retry-After` header, else 5s/10s/20s | Honor server directive        |
| 5xx Server Error    | 3       | Exponential: 1s, 2s, 4s (±jitter)     | Configurable                  |
| Connection timeout  | 2       | Fixed 2s                              | Idempotent requests only      |
| Token expired (401) | 1       | Immediate                             | Refresh token, then retry     |
| 400, 403, 404, 409  | 0       | —                                     | Client errors are not retried |

### 10.3 Idempotency

- `GET`, `PUT` are retried on transient failures.
- `POST`, `PATCH`, `DELETE` are **not** retried by default. Callers opt in:

```java
client.items().createFolder(parentId, request, RetryPolicy.retryOnServerError(2));
```

### 10.4 Circuit Breaker

The core SDK does **not** ship a circuit breaker (that would require a dependency like Resilience4j). Instead:

- The `spring-boot-starter` auto-configures Resilience4j if it's on the classpath.
- Pure Java users can wrap the `HttpTransport` with their own circuit breaker.

---

## 11. Observability, Metrics & Alerting

### 11.1 MetricsProvider SPI

```java
public interface MetricsProvider {

    Timer startTimer();

    void recordRequest(Timer timer, String entity, String method, int status);
    void recordError(String entity, String method, int status);
    void incrementCounter(String name, String... tags);
    void recordValue(String name, double value, String... tags);
    void setGauge(String name, double value, String... tags);

    interface Timer {
        void stop();
        Duration elapsed();
    }

    static MetricsProvider noop() { return NoopMetricsProvider.INSTANCE; }
}
```

**Default:** `NoopMetricsProvider` — all methods are empty. Zero overhead when metrics are not needed.

### 11.2 Metrics Emitted by Core SDK

The core SDK calls `MetricsProvider` at well-defined points. The **metric names** are consistent regardless of the implementation:

| Metric Name                            | Type    | Description                      |
| -------------------------------------- | ------- | -------------------------------- |
| `sharefile.http.requests`              | Timer   | Request duration                 |
| `sharefile.http.requests.active`       | Gauge   | In-flight requests               |
| `sharefile.http.errors`                | Counter | Errors by status code            |
| `sharefile.auth.token.refresh`         | Counter | Token refresh (tagged `outcome`) |
| `sharefile.auth.token.expiry`          | Gauge   | Seconds until token expires      |
| `sharefile.transfer.upload.bytes`      | Summary | Upload sizes                     |
| `sharefile.transfer.upload.duration`   | Timer   | Upload duration                  |
| `sharefile.transfer.download.bytes`    | Summary | Download sizes                   |
| `sharefile.transfer.download.duration` | Timer   | Download duration                |
| `sharefile.retry.attempts`             | Counter | Retries by entity and cause      |
| `sharefile.transfer.active`            | Gauge   | In-flight transfers              |

### 11.3 Micrometer Binding (in `spring-boot-starter`)

```java
// sharefile-spring-boot-starter module
public final class MicrometerMetricsProvider implements MetricsProvider {
    private final MeterRegistry registry;

    public MicrometerMetricsProvider(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Timer startTimer() {
        io.micrometer.core.instrument.Timer.Sample sample =
            io.micrometer.core.instrument.Timer.start(registry);
        return new MicrometerTimer(sample, registry);
    }

    @Override
    public void recordRequest(Timer timer, String entity, String method, int status) {
        ((MicrometerTimer) timer).getSample().stop(
            io.micrometer.core.instrument.Timer.builder("sharefile.http.requests")
                .tags("entity", entity, "method", method, "status", String.valueOf(status))
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry));
    }

    // ... other methods delegate to MeterRegistry
}
```

### 11.4 Logging

The SDK uses **SLF4J** (the only non-Jackson dependency in the client module). This works with any logging backend (Logback, Log4j2, JUL):

| Level   | What Gets Logged                                          |
| ------- | --------------------------------------------------------- |
| `ERROR` | 5xx responses, auth failures, unrecoverable errors        |
| `WARN`  | 4xx responses, retry attempts, rate limiting              |
| `INFO`  | Token refresh, lifecycle events (client created/closed)   |
| `DEBUG` | Every request/response (method, URI, status, duration)    |
| `TRACE` | Full request/response bodies (redacting sensitive fields) |

**Sensitive field redaction** at TRACE level:

```
Authorization: Bearer ****
Password: ****
client_secret: ****
```

### 11.5 Health Check (Pure Java)

The core SDK provides a health-check method:

```java
// On ShareFileClient
public HealthStatus checkHealth() {
    try {
        Account account = accounts().get();
        return HealthStatus.up(account.getSubdomain(), tokenManager.getSecondsUntilExpiry());
    } catch (Exception e) {
        return HealthStatus.down(e);
    }
}

public record HealthStatus(
    boolean up,
    String subdomain,
    Long tokenExpiresInSeconds,
    String error
) {
    public static HealthStatus up(String subdomain, long tokenExpiry) { ... }
    public static HealthStatus down(Exception cause) { ... }
}
```

The Spring starter wraps this in an Actuator `HealthIndicator` (§14.3).

### 11.6 Alerting Recommendations

| Alert               | Condition                                                    | Severity |
| ------------------- | ------------------------------------------------------------ | -------- |
| **Auth Failure**    | `sharefile.auth.token.refresh{outcome=failure}` > 0 for 5min | Critical |
| **High Error Rate** | `rate(sharefile.http.errors{status=~"5.."}[5m])` > 10%       | High     |
| **Rate Limiting**   | `sharefile.http.errors{status="429"}` > 0                    | Warning  |
| **Token Expiry**    | `sharefile.auth.token.expiry` < 300 seconds                  | Warning  |
| **Slow Requests**   | `sharefile.http.requests` p99 > 10s for 5min                 | Warning  |
| **Upload Failures** | `sharefile.http.errors{entity="Items", method="POST"}` spike | High     |

---

## 12. File Upload & Download

File transfers use **pure Java** (`java.util.concurrent`, `java.net.http`, `java.nio`). No framework dependency. Both synchronous and asynchronous APIs are provided.

### 12.1 Design Principles

| Principle                         | Rationale                                                                                    |
| --------------------------------- | -------------------------------------------------------------------------------------------- |
| **Sync by default, async opt-in** | Sync uses direct calls; async layers `CompletableFuture` on a configurable `ExecutorService` |
| **Streaming over buffering**      | Files never fully loaded into heap; all methods stream through bounded buffers               |
| **Resumable by default**          | Uploads track chunk state for resume after failures                                          |
| **Progress observable**           | `TransferProgressListener` callback on both uploads and downloads                            |
| **Cancellable**                   | Async transfers return a `TransferHandle` with cooperative cancellation                      |

### 12.2 TransferClient API

```java
public final class TransferClient {

    // ──── Synchronous ────
    UploadResult upload(String folderId, Path file, UploadOptions options);
    UploadResult upload(String folderId, InputStream stream, String fileName,
                        long fileSize, UploadOptions options);
    void download(String itemId, Path target, DownloadOptions options);
    InputStream downloadStream(String itemId, DownloadOptions options);
    DownloadSpecification resolveDownloadUrl(String itemId);
    void bulkDownload(String parentId, List<String> itemIds, Path target,
                      DownloadOptions options);

    // ──── Asynchronous ────
    UploadHandle uploadAsync(String folderId, Path file, UploadOptions options);
    DownloadHandle downloadAsync(String itemId, Path target, DownloadOptions options);

    // ──── Share uploads ────
    UploadResult uploadToShare(String shareId, Path file, UploadOptions options);
    UploadHandle uploadToShareAsync(String shareId, Path file, UploadOptions options);
}
```

### 12.3 Async Transfer Handles

```java
public sealed interface TransferHandle<T> {
    CompletableFuture<T> future();
    TransferProgress progress();
    boolean cancel();

    default T awaitOrThrow(Duration timeout) {
        try {
            return future().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw unwrap(e);
        }
    }
}

public record UploadHandle(...) implements TransferHandle<UploadResult> { ... }
public record DownloadHandle(...) implements TransferHandle<Path> { ... }
```

```java
public class TransferProgress {
    public long getBytesTransferred();
    public long getTotalBytes();
    public double getPercentComplete();            // 0.0 – 100.0
    public Duration getElapsed();
    public OptionalDouble getBytesPerSecond();
    public Optional<Duration> getEstimatedTimeRemaining();
    public TransferState getState();               // PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    public int getChunksCompleted();               // uploads only
    public int getTotalChunks();                   // uploads only
}
```

### 12.4 Upload Pipeline

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        UPLOAD PIPELINE                                   │
│                                                                          │
│  ┌─────────────┐    ┌──────────────────┐    ┌────────────────────────┐  │
│  │  Phase 1:   │    │    Phase 2:      │    │    Phase 3:            │  │
│  │  Negotiate  │───▶│    Transfer      │───▶│    Finalize            │  │
│  │             │    │                  │    │                        │  │
│  │ POST        │    │ POST file bytes  │    │ Confirm upload with    │  │
│  │ /Upload2    │    │ to ChunkUri      │    │ FinishUri (threaded)   │  │
│  │             │    │                  │    │                        │  │
│  │ Returns:    │    │ Standard: 1 req  │    │ Returns:               │  │
│  │ UploadSpec  │    │ Streamed: 1 req  │    │ Item (created file)    │  │
│  │ + ChunkUri  │    │ Threaded: N reqs │    │                        │  │
│  └─────────────┘    └──────────────────┘    └────────────────────────┘  │
│         │                    │                         │                 │
│         │          ┌────────────────────┐              │                 │
│         └─────────▶│  Progress / Resume │◀─────────────┘                │
│                    │  Cancellation      │                                │
│                    └────────────────────┘                                │
└──────────────────────────────────────────────────────────────────────────┘
```

Phase 1 uses the authenticated `ShareFileHttpClient`. Phases 2 and 3 use a **separate `HttpTransport` instance** without auth headers — storage zone URLs embed the token in the URL itself.

**Upload methods:**

| Method     | Behavior                                     | Auto-Selected When |
| ---------- | -------------------------------------------- | ------------------ |
| `Standard` | Single POST with entire file                 | File < 4 MB        |
| `Streamed` | Single POST with chunked transfer encoding   | 4 MB – 256 MB      |
| `Threaded` | Parallel chunk uploads via `ExecutorService` | > 256 MB           |

**Threaded upload** uses `java.util.concurrent` directly:

```java
ExecutorService chunkExecutor = Executors.newFixedThreadPool(options.getThreadCount());
List<Future<ChunkResult>> futures = new ArrayList<>();

for (int i = startChunk; i < totalChunks; i++) {
    if (cancelled.get()) throw new ShareFileTransferCancelledException();
    final int chunkIndex = i;
    futures.add(chunkExecutor.submit(() ->
        uploadChunk(spec.getChunkUri(), chunkIndex, readChunk(file, chunkIndex), progress)));
}

for (Future<ChunkResult> f : futures) { f.get(); }
return finishUpload(spec.getFinishUri());
```

**Resume:** Each chunk is individually retried. If all retries fail, the caller can re-invoke `upload()` — the server detects the partial upload and returns `IsResume=true` with the correct `ResumeIndex`.

### 12.5 Upload Options

```java
public final class UploadOptions {
    private UploadMethod method;               // null = auto-select by file size
    private int threadCount = 4;
    private int chunkSizeBytes = 8 * 1024 * 1024;   // 8MB
    private boolean overwrite = false;
    private boolean notifyUsers = false;
    private boolean autoResume = true;
    private int maxResumeAttempts = 3;
    private Instant clientCreatedDate;
    private Instant clientModifiedDate;
    private Integer expirationDays;
    private TransferProgressListener progressListener;
    private String batchId;
    private boolean batchLast = false;

    public static UploadOptions defaults() { return new UploadOptions(); }
    public static Builder builder() { return new Builder(); }
}
```

### 12.6 Download Pipeline

Two-phase: resolve URL → stream bytes.

```java
// Phase 1: Get download URL (via authenticated ShareFileHttpClient)
DownloadSpecification spec = httpClient.get(
    "/Items({id})/Download?redirect=false", itemId, DownloadSpecification.class);

// Phase 2: Stream from storage zone (via unauthenticated HttpTransport)
try (InputStream in = storageTransport.execute(getRequest).bodyAsStream();
     OutputStream out = Files.newOutputStream(target)) {
    byte[] buffer = new byte[64 * 1024];
    int bytesRead;
    while ((bytesRead = in.read(buffer)) != -1) {
        if (cancelled.get()) throw new ShareFileTransferCancelledException();
        out.write(buffer, 0, bytesRead);
        progress.addBytesTransferred(bytesRead);
    }
}
```

### 12.7 Download Options

```java
public final class DownloadOptions {
    private boolean includeAllVersions = false;
    private boolean includeDeleted = false;
    private TransferProgressListener progressListener;

    public static DownloadOptions defaults() { return new DownloadOptions(); }
    public static Builder builder() { return new Builder(); }
}
```

### 12.8 Sync vs Async Usage

**Synchronous:**

```java
// Upload
UploadResult result = client.transfers().upload("folderId", Path.of("report.pdf"),
    UploadOptions.defaults());

// Download
client.transfers().download(itemId, Path.of("/tmp/report.pdf"), DownloadOptions.defaults());

// Stream
try (InputStream in = client.transfers().downloadStream(itemId, DownloadOptions.defaults())) {
    Files.copy(in, targetPath);
}
```

**Asynchronous:**

```java
// Upload with progress
UploadHandle handle = client.transfers().uploadAsync("folderId", Path.of("large.zip"),
    UploadOptions.builder()
        .method(UploadMethod.Threaded)
        .threadCount(8)
        .progressListener((transferred, total) ->
            log.info("{}%", transferred * 100 / total))
        .build());

UploadResult result = handle.awaitOrThrow(Duration.ofMinutes(30));

// Concurrent downloads
List<DownloadHandle> handles = itemIds.stream()
    .map(id -> client.transfers().downloadAsync(id,
        downloadDir.resolve(id + ".dat"), DownloadOptions.defaults()))
    .toList();

CompletableFuture.allOf(
    handles.stream().map(DownloadHandle::future).toArray(CompletableFuture[]::new)
).join();

// Cancellation
handle.cancel();  // cooperative — in-flight chunk finishes, no new work starts
```

### 12.9 TransferProgressListener

```java
@FunctionalInterface
public interface TransferProgressListener {
    void onProgress(long bytesTransferred, long totalBytes);
}
```

### 12.10 Transfer Exception Hierarchy

```
ShareFileTransferException
├── ShareFileUploadException
│   ├── ShareFileUploadNegotiationException     # Phase 1 failure
│   ├── ShareFileChunkUploadException           # Chunk failed after retries
│   │     └── getChunkIndex(), getOffset()
│   └── ShareFileUploadFinalizationException    # Phase 3 failure
├── ShareFileDownloadException
│   ├── ShareFileDownloadUrlExpiredException
│   └── ShareFileDownloadWriteException
└── ShareFileTransferCancelledException
```

`ShareFileUploadException` includes:

```java
public class ShareFileUploadException extends ShareFileTransferException {
    private final boolean resumable;
    private final int lastSuccessfulChunkIndex;
    private final long bytesTransferred;
}
```

---

## 13. Pagination

### 13.1 Manual Pagination

```java
ODataFeed<Item> page = client.items().getChildren(folderId,
    ODataQuery.builder().top(100).skip(0).build());

while (page.hasNextPage()) {
    page = client.items().getNextPage(page);  // follows odata.nextLink
}
```

### 13.2 Auto-Pagination Iterator

```java
for (Item item : client.items().listAll(folderId)) {
    process(item);
}

// Or as a Stream
client.items().streamAll(folderId)
    .filter(item -> item.getFileSizeBytes() > 1_000_000)
    .forEach(this::processLargeFile);
```

The auto-paginator follows `odata.nextLink` lazily.

---

## 14. Spring Boot Integration

The `sharefile-spring-boot-starter` module wraps the pure Java SDK with Spring-native features. It depends on the SDK but never the other way around.

### 14.1 Auto-Configuration

```java
@AutoConfiguration
@ConditionalOnClass(ShareFileClient.class)
@EnableConfigurationProperties(ShareFileProperties.class)
public class ShareFileAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ShareFileClient shareFileClient(ShareFileProperties props,
                                            Optional<CredentialProvider> credentialProvider,
                                            Optional<MeterRegistry> meterRegistry,
                                            Optional<RestClient.Builder> restClientBuilder) {

        ShareFileClientBuilder builder = ShareFileClient.builder()
            .subdomain(props.getSubdomain())
            .apiControlPlane(props.getApiControlPlane())
            .connectTimeout(props.getHttp().getConnectTimeout())
            .readTimeout(props.getHttp().getReadTimeout())
            .uploadTimeout(props.getHttp().getUploadReadTimeout())
            .downloadTimeout(props.getHttp().getDownloadReadTimeout())
            .tokenRefreshBuffer(Duration.ofSeconds(
                props.getAuth().getToken().getRefreshBufferSeconds()));

        // Credential resolution: user-defined bean takes priority over properties
        if (credentialProvider.isPresent()) {
            builder.credentialProvider(credentialProvider.get());
        } else {
            builder.clientCredentials(props.getAuth().getClientId(),
                                       props.getAuth().getClientSecret());
            switch (props.getAuth().getGrantType()) {
                case PASSWORD -> builder.passwordGrant(
                    props.getAuth().getUsername(), props.getAuth().getPassword());
                case AUTHORIZATION_CODE -> builder.authorizationCode(
                    props.getAuth().getCode());
            }
        }

        // Plug in Micrometer metrics
        meterRegistry.ifPresent(registry ->
            builder.metricsProvider(new MicrometerMetricsProvider(registry)));

        // Plug in Spring RestClient transport (if available)
        restClientBuilder.ifPresent(rcb ->
            builder.httpTransport(new RestClientHttpTransport(rcb.build())));

        return builder.build();
    }

    @Bean
    @ConditionalOnClass(name = "io.github.resilience4j.circuitbreaker.CircuitBreaker")
    @ConditionalOnProperty("sharefile.resilience.circuit-breaker.enabled")
    public CircuitBreakerCustomizer shareFileCircuitBreaker(ShareFileProperties props) { ... }
}
```

**Dynamic credentials in Spring Boot:**

When a `CredentialProvider` bean exists, the auto-configuration uses it instead of `application.yml` properties. This lets Spring Boot apps resolve credentials from a database, vault, or any runtime source:

```java
@Configuration
public class ShareFileCredentialConfig {

    @Bean
    public CredentialProvider shareFileCredentialProvider(
            CredentialRepository credentialRepository,
            AesGcmCipher cipher) {
        return new DatabaseCredentialProvider("default-tenant", credentialRepository, cipher);
    }
}
```

For multi-tenant apps where each request uses a different tenant's credentials, see §5.2 for the `CredentialProvider` pattern. The Spring starter supports this via request-scoped or custom-scoped beans.

### 14.2 Configuration Properties

```java
@ConfigurationProperties(prefix = "sharefile")
public class ShareFileProperties {
    private String subdomain;
    private String apiControlPlane = "sharefile.com";
    private Auth auth = new Auth();
    private Http http = new Http();
    private Transfer transfer = new Transfer();
    private Resilience resilience = new Resilience();

    public static class Auth {
        private String clientId;
        private String clientSecret;
        private GrantType grantType = GrantType.PASSWORD;
        private String username;
        private String password;
        private String code;
        private Token token = new Token();
    }

    public static class Token {
        private int refreshBufferSeconds = 300;
    }

    public static class Http {
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(30);
        private Duration uploadReadTimeout = Duration.ofSeconds(300);
        private Duration downloadReadTimeout = Duration.ofSeconds(300);
    }

    public static class Transfer {
        private int corePoolSize = 2;
        private int maxPoolSize = 8;
        private int queueCapacity = 50;
    }

    public static class Resilience {
        private CircuitBreaker circuitBreaker = new CircuitBreaker();
    }

    public static class CircuitBreaker {
        private boolean enabled = false;
        private int failureRateThreshold = 50;
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        private int slidingWindowSize = 20;
    }
}
```

### 14.3 Health Indicator

```java
@Component
@ConditionalOnProperty("sharefile.health.enabled", havingValue = "true", matchIfMissing = true)
public class ShareFileHealthIndicator implements HealthIndicator {
    private final ShareFileClient client;

    @Override
    public Health health() {
        HealthStatus status = client.checkHealth();
        Health.Builder builder = status.up() ? Health.up() : Health.down();
        return builder
            .withDetail("subdomain", status.subdomain())
            .withDetail("tokenExpiresIn", status.tokenExpiresInSeconds() + "s")
            .build();
    }
}
```

Exposed at `/actuator/health/sharefile`.

### 14.4 Distributed Tracing

If Micrometer Tracing is on the classpath, the `RestClientHttpTransport` adapter inherits Spring's observation context, propagating trace IDs through to ShareFile API calls.

### 14.5 Spring Boot Usage

```yaml
sharefile:
  subdomain: mycompany
  auth:
    client-id: ${SHAREFILE_CLIENT_ID}
    client-secret: ${SHAREFILE_CLIENT_SECRET}
    grant-type: password
    username: ${SHAREFILE_USERNAME}
    password: ${SHAREFILE_PASSWORD}
  http:
    connect-timeout: 10s
    read-timeout: 30s
  transfer:
    core-pool-size: 2
    max-pool-size: 8
```

```java
@SpringBootApplication
public class MyApp {
    @Autowired
    private ShareFileClient shareFile;  // same class as pure Java usage

    public void listFiles() {
        ODataFeed<Item> children = shareFile.items().getChildren("home");
        children.getItems().forEach(item -> log.info(item.getName()));
    }
}
```

---

## 15. Testing Strategy

### 15.1 Unit Tests (Pure Java)

- Resource client methods tested with a mock `HttpTransport` (return canned responses).
- OData query builder tested with assertion on generated query strings.
- Error handler tested with synthetic HTTP responses.
- Token manager tested with clock manipulation.
- No Spring test context needed.

```java
class ItemsServiceTest {
    private final MockHttpTransport transport = new MockHttpTransport();
    private final ShareFileClient client = ShareFileClient.builder()
        .subdomain("test")
        .clientCredentials("id", "secret")
        .accessToken("test-token", "test-refresh")
        .httpTransport(transport)
        .build();

    @Test
    void getByIdReturnsItem() {
        transport.enqueue(200, loadFixture("items/get-by-id.json"));

        Item item = client.items().getById("abc123");
        assertThat(item.getName()).isEqualTo("My Document.pdf");
    }
}
```

### 15.2 Integration Tests (WireMock)

```java
class ItemsServiceIntegrationTest {
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private ShareFileClient client;

    @BeforeEach
    void setUp() {
        client = ShareFileClient.builder()
            .subdomain("test")
            .clientCredentials("id", "secret")
            .accessToken("test-token", "test-refresh")
            .baseUrl(wireMock.baseUrl() + "/sf/v3")  // override for testing
            .build();
    }

    @Test
    void getByIdReturnsItem() {
        wireMock.stubFor(get("/sf/v3/Items(abc123)")
            .willReturn(okJson(fixture("items/get-by-id.json"))));

        Item item = client.items().getById("abc123");
        assertThat(item.getName()).isEqualTo("My Document.pdf");
    }
}
```

### 15.3 Spring Integration Tests

The `sharefile-sdk-test` module provides `@ShareFileMockServer` for Spring Boot tests:

```java
@SpringBootTest
@ShareFileMockServer
class SpringItemsServiceTest {
    @Autowired
    private ShareFileClient client;

    @Test
    void getByIdReturnsItem(WireMockServer server) {
        server.stubFor(get("/sf/v3/Items(abc123)")
            .willReturn(okJson(fixture("items/get-by-id.json"))));

        Item item = client.items().getById("abc123");
        assertThat(item.getName()).isEqualTo("My Document.pdf");
    }
}
```

---

## 16. Security Considerations

| Concern                    | Mitigation                                                                                                                                                                                                               |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Credential storage         | `CredentialProvider` SPI decouples credential sourcing — supports DB with AES-GCM encryption, Vault, env vars, or any custom strategy. Credentials are resolved lazily at auth time only, never stored in builder state. |
| Token in logs              | Redacted at TRACE level; never logged at DEBUG or above                                                                                                                                                                  |
| HMAC validation            | Authorization code redirect validated before token exchange                                                                                                                                                              |
| TLS                        | HTTPS enforced; no HTTP fallback                                                                                                                                                                                         |
| Upload redirect            | Separate `HttpTransport` instance with no auth headers for storage zone calls                                                                                                                                            |
| Dependency vulnerabilities | Gradle dependency-check plugin in CI                                                                                                                                                                                     |
| SSRF                       | SDK only connects to `*.sf-api.com`, `*.sharefile.com`, and upload redirect hosts                                                                                                                                        |

---

## 17. Dependencies

### sharefile-sdk-core

```kotlin
dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
```

### sharefile-sdk-client

```kotlin
dependencies {
    api(project(":sharefile-sdk-core"))
    implementation("org.slf4j:slf4j-api")
    // Everything else is java.* standard library
}
```

### sharefile-spring-boot-starter

```kotlin
dependencies {
    api(project(":sharefile-sdk-client"))

    implementation("org.springframework.boot:spring-boot-starter-web")      // RestClient adapter
    implementation("org.springframework.boot:spring-boot-starter-actuator") // Health indicator
    implementation("io.micrometer:micrometer-core")                        // Metrics binding

    // Optional
    compileOnly("io.github.resilience4j:resilience4j-spring-boot3")
    compileOnly("io.micrometer:micrometer-tracing")
}
```

### Minimum Dependency Footprint

| Module                          | Dependencies                     |
| ------------------------------- | -------------------------------- |
| `sharefile-sdk-core`            | Jackson                          |
| `sharefile-sdk-client`          | Jackson + SLF4J                  |
| `sharefile-spring-boot-starter` | Above + Spring Boot + Micrometer |

---

## 18. Publishing & Distribution

### 18.1 Published Artifacts

| Artifact    | Artifact ID                     | Use Case                                   |
| ----------- | ------------------------------- | ------------------------------------------ |
| **Core**    | `sharefile-sdk-core`            | Models only (shared contract libraries)    |
| **Client**  | `sharefile-sdk-client`          | Pure Java usage (any framework)            |
| **Starter** | `sharefile-spring-boot-starter` | Spring Boot 3 auto-configured              |
| **BOM**     | `sharefile-sdk-bom`             | Version alignment                          |
| **Test**    | `sharefile-sdk-test`            | WireMock fixtures + `@ShareFileMockServer` |

**Consumer dependency by use case:**

```kotlin
// Pure Java / Micronaut / Quarkus / Dropwizard / CLI
implementation("io.github.indraftapp:sharefile-sdk-client:1.0.0")

// Spring Boot 3
implementation("io.github.indraftapp:sharefile-spring-boot-starter:1.0.0")

// Models only
implementation("io.github.indraftapp:sharefile-sdk-core:1.0.0")
```

### 18.2 Repository Targets

| Repository          | Purpose           | When                            |
| ------------------- | ----------------- | ------------------------------- |
| **Maven Central**   | Public releases   | Tagged release builds           |
| **GitHub Packages** | Snapshots         | Every merge to `main`           |
| **Local `~/.m2`**   | Developer testing | `./gradlew publishToMavenLocal` |

### 18.3 Versioning

Semantic Versioning 2.0.0: `{major}.{minor}.{patch}[-SNAPSHOT]`

Declared once in `gradle.properties`:

```properties
group=io.github.indraftapp
version=1.0.0-SNAPSHOT
```

### 18.4 Gradle Publishing Configuration

```kotlin
// Root build.gradle.kts
plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(providers.environmentVariable("OSSRH_USERNAME"))
            password.set(providers.environmentVariable("OSSRH_PASSWORD"))
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withJavadocJar()
        withSourcesJar()
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                pom {
                    name.set(project.name)
                    description.set("ShareFile REST API SDK for Java")
                    url.set("https://github.com/your-org/sharefile-java-sdk")
                    licenses { license { name.set("MIT License") } }
                    developers { developer { id.set("your-id") } }
                    scm { url.set("https://github.com/your-org/sharefile-java-sdk") }
                }
            }
        }
    }

    signing {
        val signingKey = providers.environmentVariable("GPG_SIGNING_KEY")
        val signingPassword = providers.environmentVariable("GPG_SIGNING_PASSWORD")
        if (signingKey.isPresent) {
            useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
        }
        sign(publishing.publications["mavenJava"])
    }

    tasks.withType<Sign>().configureEach {
        onlyIf { !version.toString().endsWith("-SNAPSHOT") }
    }
}
```

### 18.5 CI/CD (GitHub Actions)

Three workflows:

1. **CI** (every push/PR) — build + test
2. **Snapshot** (every merge to `main`) — publish to GitHub Packages + Sonatype Snapshots
3. **Release** (manual trigger) — strip `-SNAPSHOT`, sign, publish to Maven Central, tag, bump to next snapshot

### 18.6 BOM

```kotlin
// sharefile-sdk-bom/build.gradle.kts
plugins { id("java-platform") }
dependencies {
    constraints {
        api(project(":sharefile-sdk-core"))
        api(project(":sharefile-sdk-client"))
        api(project(":sharefile-spring-boot-starter"))
    }
}
```

### 18.7 Local Development

```bash
./gradlew publishToMavenLocal
```

Or Gradle composite build for live editing:

```kotlin
// Consumer settings.gradle.kts
includeBuild("../sharefile-java-sdk") {
    dependencySubstitution {
        substitute(module("io.github.indraftapp:sharefile-sdk-client"))
            .using(project(":sharefile-sdk-client"))
    }
}
```
