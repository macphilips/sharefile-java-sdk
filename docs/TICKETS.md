# ShareFile Java SDK — Implementation Tickets

Each ticket is self-contained with enough context for an AI agent to implement it against the tech spec (`docs/TECH_SPEC.md`) and API reference (`docs/API_REFERENCE.md`).

**Dependency order matters.** Tickets have explicit `Depends on` fields. An agent must not start a ticket until its dependencies are complete.

Individual ticket files are in [`docs/tickets/`](tickets/).

---

## Release Milestones

| Milestone      | Tickets              | Scope                                                                                                                            |
| -------------- | -------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| **v0.1 — MVP** | SF-01 → SF-14, SF-19 | Auth, OData, streaming HTTP, all Tier 1 resource clients, TransferClient, metrics SPI, builder, typed exceptions, WireMock tests |
| **v0.2**       | SF-15, SF-16         | Pagination, Tier 2 resource clients (Groups, Webhooks, Sessions)                                                                 |
| **v0.3**       | SF-17, SF-18         | Spring Boot starter, Micrometer metrics, health indicator                                                                        |
| **v1.0**       | SF-19, SF-20         | Test infrastructure, CI/CD, Maven Central publishing                                                                             |

---

## Ticket Dependency Graph

```
SF-01  Gradle Project Scaffolding
  │
  ├──▶ SF-02  Core Models & Enums
  │      │
  │      ├──▶ SF-03  Exception Hierarchy
  │      │
  │      ├──▶ SF-04  OData Query Builder
  │      │
  │      └──▶ SF-05  Jackson Serialization Config
  │             │
  │             └──▶ SF-06  HTTP Abstraction Layer (Streaming)
  │                    │
  │                    ├──▶ SF-07  CredentialProvider SPI, Auth & Token Management
  │                    │      │
  │                    │      └──▶ SF-08  ShareFileHttpClient + Retry Engine
  │                    │             │
  │                    │             ├──▶ SF-09  ResourceRequestExecutor + ItemsClient
  │                    │             │
  │                    │             ├──▶ SF-10  AccessControlsClient + AsyncOperationsClient
  │                    │             │
  │                    │             ├──▶ SF-11  SharesClient + UsersClient
  │                    │             │
  │                    │             └──▶ SF-12  TransferClient (Upload + Download)
  │                    │
  │                    └──▶ SF-13  MetricsProvider SPI & NoopMetrics
  │
  ├──▶ SF-14  ShareFileClient Builder & Lifecycle
  │      (depends on: SF-07 through SF-13)
  │
  ├──▶ SF-15  Pagination (Manual + Auto-Paginator)
  │      (depends on: SF-09)
  │
  ├──▶ SF-16  Tier 2 Resource Clients (Groups, Webhooks, Sessions)
  │      (depends on: SF-14)
  │
  ├──▶ SF-17  Spring Boot Starter — Auto-Configuration
  │      (depends on: SF-14)
  │
  ├──▶ SF-18  Spring Boot Starter — Micrometer Metrics & Health
  │      (depends on: SF-17)
  │
  ├──▶ SF-19  Test Infrastructure & Fixtures
  │      (depends on: SF-14)
  │
  └──▶ SF-20  CI/CD & Publishing
       (depends on: SF-01)
```

---

## SF-01: Gradle Project Scaffolding

**Module:** Root + all submodules
**Depends on:** None
**Milestone:** v0.1
**Spec Reference:** §2 (Architecture & Module Structure), §17 (Dependencies), §18.4 (Gradle Publishing)

### Goal

Set up the multi-module Gradle project with Kotlin DSL, including all 5 modules, dependency declarations, Java 17 target, and publishing configuration.

### Requirements

1. Create root `build.gradle.kts` with:

   - `io.github.gradle-nexus.publish-plugin` v2.0.0
   - Sonatype OSSRH config reading from environment variables `OSSRH_USERNAME` / `OSSRH_PASSWORD`
   - Subproject defaults: `java-library`, `maven-publish`, `signing` plugins
   - Java 17 source/target compatibility
   - `withJavadocJar()` and `withSourcesJar()`
   - Maven POM metadata (name, description, license MIT, SCM URL)
   - GPG signing from `GPG_SIGNING_KEY` / `GPG_SIGNING_PASSWORD` env vars
   - Signing only for non-SNAPSHOT versions

2. Create `settings.gradle.kts` including all modules:

   - `sharefile-sdk-core`
   - `sharefile-sdk-client`
   - `sharefile-spring-boot-starter`
   - `sharefile-sdk-bom`
   - `sharefile-sdk-test`

3. Create `gradle.properties`:

   ```properties
   group=io.github.indraftapp
   version=1.0.0-SNAPSHOT
   ```

4. Create each module's `build.gradle.kts` with exact dependencies per §17:

   **sharefile-sdk-core:**

   ```kotlin
   dependencies {
       implementation("com.fasterxml.jackson.core:jackson-databind")
       implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
   }
   ```

   **sharefile-sdk-client:**

   ```kotlin
   dependencies {
       api(project(":sharefile-sdk-core"))
       implementation("org.slf4j:slf4j-api")
   }
   ```

   **sharefile-spring-boot-starter:**

   ```kotlin
   dependencies {
       api(project(":sharefile-sdk-client"))
       implementation("org.springframework.boot:spring-boot-starter-web")
       implementation("org.springframework.boot:spring-boot-starter-actuator")
       implementation("io.micrometer:micrometer-core")
       compileOnly("io.github.resilience4j:resilience4j-spring-boot3")
       compileOnly("io.micrometer:micrometer-tracing")
   }
   ```

   **sharefile-sdk-bom:**

   ```kotlin
   plugins { id("java-platform") }
   dependencies {
       constraints {
           api(project(":sharefile-sdk-core"))
           api(project(":sharefile-sdk-client"))
           api(project(":sharefile-spring-boot-starter"))
       }
   }
   ```

   **sharefile-sdk-test:**

   ```kotlin
   dependencies {
       api(project(":sharefile-sdk-client"))
       // WireMock, JUnit 5, AssertJ
   }
   ```

5. Create empty source directory structures matching §2.2 module layout.

6. Add a Gradle wrapper (latest stable 8.x).

### Acceptance Criteria

- `./gradlew build` compiles with zero errors (no source files yet, just structure)
- `./gradlew projects` shows all 5 modules
- `./gradlew publishToMavenLocal` succeeds (empty JARs)

---

## SF-02: Core Models & Enums

**Module:** `sharefile-sdk-core`
**Depends on:** SF-01
**Milestone:** v0.1
**Spec Reference:** §8 (Model Layer), API_REFERENCE.md §13 (Model Definitions)

### Goal

Implement all entity POJOs, the `ODataEntity` base class, `ODataFeed<T>` collection wrapper, and all enums.

### Requirements

1. **Base class** — `ODataEntity` per §8.1:

   ```java
   public abstract class ODataEntity {
       @JsonProperty("odata.metadata") private String metadata;
       @JsonProperty("odata.type") private String type;
       @JsonProperty("Id") private String id;
       @JsonProperty("url") private String url;
       // getters, setters
   }
   ```

2. **ODataFeed<T>** per §8.2:

   - Fields: `odata.count` (Integer), `odata.nextLink` (String), `value` (List<T>)
   - Implements `Iterable<T>`
   - `hasNextPage()`, `getItems()` methods

3. **Entity hierarchy** per §8.3 — all types extend `ODataEntity`:

   - `Item` (with subclasses `File`, `Folder`, `Note`, `Link`, `SymbolicLink`)
   - `User` (with subclass `AccountUser`)
   - `Share`, `Group`, `Account`, `AccessControl`, `Zone`, `Device`, `DeviceUser`
   - `WebhookSubscription`, `AsyncOperation`, `Session`
   - Supporting types: `Contact`, `Favorite`, `Metadata`
   - Request types: `FolderCreateRequest`, `NoteCreateRequest`, `LinkCreateRequest`, `AdvancedSearchRequest`, `SendShareRequest`, `RequestShareRequest`, `ShareNotificationRequest`, `BulkAccessControlRequest`, `BulkDeleteRequest`, `BulkRestoreRequest`, `CheckInRequest`, `CloneRequest`, `NotifyRequest`, `UploadRequestParams`
   - Response types: `SearchResults`, `AdvancedSearchResults`, `ItemInfo`, `Redirection`, `UploadSpecification`, `DownloadSpecification`, `UploadResult`, `ChunkResult`, `AccessControlBulkResult`, `UserPreferences`, `UserSecurity`

   Refer to `docs/API_REFERENCE.md` §13 for all field names and types. Use Jackson `@JsonProperty` with PascalCase names.

4. **OperationResult<T>** sealed interface per §8.4:

   ```java
   public sealed interface OperationResult<T> {
       record Completed<T>(T entity) implements OperationResult<T> {}
       record Pending<T>(AsyncOperation operation) implements OperationResult<T> {}
       default boolean isAsync() { ... }
       default T getEntityOrThrow() { ... }
       default Optional<AsyncOperation> asyncOperation() { ... }
   }
   ```

5. **Enums** per §8.5 and API_REFERENCE.md §14:

   - `ShareType`, `UploadMethod`, `TreeMode`, `ItemOrderingMode`, `DlpStatus`, `PreviewStatus`, `ZoneService`, `GrantType`
   - All enums must use Jackson `@JsonValue` / `@JsonCreator` for serialization.

6. **HealthStatus** record per §11.5.

7. **OAuthToken** — holds access_token, refresh_token, token_type, expires_in, and computed expiresAt (Instant).

### Package Structure

```
io.github.indraftapp.core.model/          — Entity POJOs
io.github.indraftapp.core.model.enums/    — Enums
io.github.indraftapp.core.model.request/  — Request types
io.github.indraftapp.core.model.response/ — Response/supporting types
```

### Acceptance Criteria

- All entity classes compile with proper Jackson annotations
- `OperationResult<T>` sealed interface compiles with Java 17 sealed types
- All enums have JSON serialization/deserialization
- Unit test: serialize an `Item` to JSON → verify PascalCase field names
- Unit test: deserialize a sample JSON response → verify all fields populated

---

## SF-03: Exception Hierarchy

**Module:** `sharefile-sdk-core`
**Depends on:** SF-02
**Milestone:** v0.1
**Spec Reference:** §9 (Error Handling & Reporting)

### Goal

Implement the full exception hierarchy as unchecked exceptions.

### Requirements

1. Create the hierarchy per §9.1:

   ```
   ShareFileException (extends RuntimeException)
   ├── ShareFileApiException
   │   ├── ShareFileNotFoundException (404)
   │   ├── ShareFileConflictException (409)
   │   ├── ShareFileForbiddenException (403)
   │   ├── ShareFileUnauthorizedException (401)
   │   ├── ShareFileRateLimitException (429)
   │   ├── ShareFileBadRequestException (400)
   │   └── ShareFileServerException (5xx)
   ├── ShareFileAuthenticationException
   │   └── CredentialResolutionException
   ├── ShareFileNetworkException
   ├── ShareFileSerializationException
   ├── ShareFileAsyncOperationException
   ├── ShareFileTimeoutException
   └── ShareFileTransferException
       ├── ShareFileUploadException
       │   ├── ShareFileUploadNegotiationException
       │   ├── ShareFileChunkUploadException
       │   └── ShareFileUploadFinalizationException
       ├── ShareFileDownloadException
       │   ├── ShareFileDownloadUrlExpiredException
       │   └── ShareFileDownloadWriteException
       └── ShareFileTransferCancelledException
   ```

2. `ShareFileApiException` fields per §9.2: `httpStatus`, `errorCode`, `errorMessage`, `requestId`, `requestMethod`, `requestUri`.

3. `ShareFileRateLimitException`: include `retryAfterSeconds`.

4. `ShareFileUploadException`: include `resumable`, `lastSuccessfulChunkIndex`, `bytesTransferred`.

5. `ShareFileChunkUploadException`: include `chunkIndex` and `offset`.

6. `ShareFileAsyncOperationException`: include the `AsyncOperation` object.

### Package Structure

```
io.github.indraftapp.core.exception/
```

### Acceptance Criteria

- All exceptions compile and follow the hierarchy
- Each exception has meaningful constructors (message, cause, context fields)
- Unit test: create each exception, verify fields are accessible via getters

---

## SF-04: OData Query Builder

**Module:** `sharefile-sdk-core`
**Depends on:** SF-02
**Milestone:** v0.1
**Spec Reference:** §7 (OData Query Builder)

### Goal

Implement the fluent OData query builder and type-safe filter DSL.

### Requirements

1. **ODataQuery** builder per §7.1 with `select`, `expand`, `expandAll`, `filter` (raw string and type-safe `Filter`), `orderBy`, `top`, `skip`.

2. Serializes to `Map<String, String>` of OData query parameters (`$select`, `$expand`, `$filter`, `$orderby`, `$top`, `$skip`).

3. **Filter DSL** per §7.2: `eq`, `ne`, `gt`, `lt`, `substringOf`, `startsWith`, `endsWith`, `isOf`, `and`, `or`.

4. String values properly escaped (single-quoted).

5. `SortDirection` enum: `ASC`, `DESC`.

### Acceptance Criteria

- `ODataQuery.builder().select("Name").top(10).build().toQueryParams()` returns `{$select=Name, $top=10}`
- Complex filter with `and`/`or` produces correct OData expression
- String values are single-quoted: `Name eq 'Reports'`
- `substringOf` produces `substringof('value', FieldName)`
- Unit tests for all filter operators and combinations

---

## SF-05: Jackson Serialization Config

**Module:** `sharefile-sdk-core`
**Depends on:** SF-02
**Milestone:** v0.1
**Spec Reference:** §4.6 (Request/Response Serialization)

### Goal

Create the SDK's default `ObjectMapper` configuration and custom deserializers.

### Requirements

1. **ShareFileObjectMapper** factory with: `FAIL_ON_UNKNOWN_PROPERTIES = false`, `WRITE_DATES_AS_TIMESTAMPS = false`, `JavaTimeModule`, `UPPER_CAMEL_CASE` naming.

2. **ODataFeedDeserializer** for unwrapping `{ "odata.count": N, "value": [...] }`.

3. **ODataTypeResolver** — inspects `odata.type` JSON field for polymorphic dispatch:

   - `ShareFile.Api.Models.File` → `File.class`, etc.
   - Critical for `OperationResult<T>` and `Item` type hierarchy.

4. **ShareFileModule** — Jackson module registering all custom serializers/deserializers.

### Acceptance Criteria

- Deserialize `{"odata.type": "ShareFile.Api.Models.File", "Id": "abc"}` → `File` instance
- Deserialize `{"odata.type": "ShareFile.Api.Models.AsyncOperation", "Id": "op1"}` → `AsyncOperation` instance
- Deserialize feed JSON → `ODataFeed` with items
- PascalCase field names in serialized JSON
- `Instant` / `ZonedDateTime` serialize as ISO-8601

---

## SF-06: HTTP Abstraction Layer (Streaming)

**Module:** `sharefile-sdk-client`
**Depends on:** SF-05
**Milestone:** v0.1
**Spec Reference:** §4.1-4.2 (HTTP Abstraction Layer)

### Goal

Implement the `HttpTransport` SPI with **streaming support** and the default JDK `HttpClient` implementation.

### Requirements

1. **HttpTransport** interface per §4.1 — streaming-first design:

   ```java
   public interface HttpTransport {
       HttpResponse execute(HttpRequest request) throws ShareFileNetworkException;

       interface HttpRequest {
           String method();
           URI uri();
           Map<String, String> headers();
           Optional<InputStream> bodyStream();   // streaming body for uploads
           OptionalLong contentLength();          // -1 or empty = chunked
           Duration timeout();
       }

       interface HttpResponse {
           int statusCode();
           Map<String, List<String>> headers();
           InputStream bodyStream();              // always streaming
           byte[] bodyBytes();                    // convenience: reads stream to memory
           byte[] bodyBytes(int maxBytes);        // bounded read
       }
   }
   ```

   **Design rationale from spec:** File transfers can be multi-GB. The transport must never require buffering entire upload/download bodies as byte arrays. For JSON API calls (typically < 1MB), callers use `bodyBytes()`. For file transfers, callers consume `bodyStream()` directly.

2. **Request construction helpers** (package-private):

   ```java
   HttpRequests.json("POST", uri, jsonBytes, timeout)       // JSON API calls
   HttpRequests.streaming("POST", uri, inputStream, size, timeout)  // file uploads
   HttpRequests.noBody("GET", uri, timeout)                  // GET/DELETE
   ```

3. **JdkHttpTransport** per §4.2:

   - Uses `java.net.http.HttpClient` with `BodyHandlers.ofInputStream()` — never buffers full response
   - Configurable connect timeout from `ShareFileConfig`
   - `followRedirects` set to `NEVER`
   - Wraps `IOException` / `InterruptedException` in `ShareFileNetworkException`

4. **ShareFileConfig** — plain POJO: `subdomain`, `apiControlPlane` (default "sharefile.com"), `connectTimeout` (10s), `readTimeout` (30s), `uploadTimeout` (300s), `downloadTimeout` (300s), `tokenRefreshBuffer` (5min). Computed `baseUrl`.

### Acceptance Criteria

- `JdkHttpTransport` can execute a GET and return status + streaming body
- JSON response read via `bodyBytes()` returns full byte array
- Large response via `bodyStream()` can be consumed incrementally
- Upload request carries `InputStream` body without buffering
- Connection timeouts produce `ShareFileNetworkException`
- Unit test with a local HTTP server verifying streaming request/response

---

## SF-07: CredentialProvider SPI, Auth & Token Management

**Module:** `sharefile-sdk-client`
**Depends on:** SF-06
**Milestone:** v0.1
**Spec Reference:** §5 (Authentication)

### Goal

Implement `CredentialProvider`, `Credentials`, `TokenManager`, `TokenStore`, and all OAuth2 flows. This is a single ticket because the auth components are tightly coupled.

### Requirements

#### CredentialProvider (§5.2)

1. **CredentialProvider** interface:

   ```java
   @FunctionalInterface
   public interface CredentialProvider {
       Credentials resolve() throws CredentialResolutionException;
   }
   ```

2. **Credentials** record with builder:

   ```java
   public record Credentials(
       String clientId, String clientSecret,
       GrantType grantType,
       String username, String password,       // null unless PASSWORD
       String authorizationCode                // null unless AUTHORIZATION_CODE
   ) { ... }
   ```

3. **StaticCredentialProvider** — wraps fixed `Credentials`. Used by builder convenience methods.

4. **Validation:** clientId + clientSecret always required. Grant-type-specific fields validated.

#### Token Management (§5.3)

5. **TokenManager** with corrected refresh flow per §5.3:

   ```
   1. Check TokenStore for cached token → valid? return it (fast path)
   2. Token expired, refresh token available?
      → POST /oauth/token (refresh_token grant)
        Uses: client_id, client_secret, refresh_token    ← NO CredentialProvider call
      → Success: save rotated token, schedule refresh
      → Failure: fall through to step 3
   3. No token, no refresh, or refresh failed?
      → CredentialProvider.resolve()                     ← ONLY here
      → POST /oauth/token (password or authorization_code grant)
      → Save token, schedule proactive refresh
   4. Proactive refresh (80% of expires_in, background)
      → POST /oauth/token (refresh_token)                ← NO CredentialProvider call
      → Failure: CredentialProvider.resolve() + full re-auth (step 3)
   ```

   **Critical:** Refresh-token grant uses only `client_id`, `client_secret`, and `refresh_token` — it does NOT call `CredentialProvider.resolve()`. The credential provider is only called for initial authentication or when refresh fails entirely.

6. **TokenManager holds client credentials separately** (extracted once at build time via initial `CredentialProvider.resolve()` or builder convenience methods):

   ```java
   final class TokenManager implements AutoCloseable {
       private final String clientId;
       private final String clientSecret;
       private final CredentialProvider credentialProvider;  // fallback only
       private final HttpTransport transport;
       private final TokenStore tokenStore;
       private volatile OAuthToken currentToken;
       private final ReentrantReadWriteLock tokenLock;
       private final ScheduledExecutorService scheduler;
       private final MetricsProvider metrics;
   }
   ```

7. **TokenStore SPI** with `InMemoryTokenStore` default.

8. **Token endpoint** per §5.4: `POST https://{subdomain}.{apicp}/oauth/token`, form-encoded.

9. **HMAC validation** per §5.5 for authorization code flow.

10. **Retry:** 3 retries with exponential backoff on refresh failure.

11. **Thread-safe:** `ReentrantReadWriteLock` for concurrent access.

### Acceptance Criteria

- Authorization code grant: sends correct params, parses token
- Password grant: sends correct form parameters, parses token
- Token caching: second `getAccessToken()` returns cached value
- Proactive refresh uses refresh_token grant **without** calling CredentialProvider
- Refresh failure triggers CredentialProvider.resolve() + full re-auth
- Thread safety: concurrent calls don't duplicate auth requests
- TokenStore.save() called after acquisition; TokenStore.load() checked on startup
- HMAC validation passes/fails correctly
- CredentialProvider is only called at most once under normal operation

---

## SF-08: ShareFileHttpClient + Retry Engine

**Module:** `sharefile-sdk-client`
**Depends on:** SF-07
**Milestone:** v0.1
**Spec Reference:** §4.4-4.5 (ShareFileHttpClient), §9.2-9.4 (Error Handling), §10 (Retry & Resilience)

### Goal

Implement the internal request pipeline and retry engine as a single ticket — they are tightly coupled.

### Requirements

#### ShareFileHttpClient (§4.4)

1. Internal class wrapping `HttpTransport`:

   ```java
   final class ShareFileHttpClient {
       private final HttpTransport transport;
       private final TokenManager tokenManager;
       private final ObjectMapper objectMapper;
       private final RetryEngine retryEngine;
       private final MetricsProvider metrics;
       private final String baseUrl;
   }
   ```

2. **Request pipeline** per §4.5:

   - Serialize body with Jackson → pass as `InputStream` via `HttpRequests.json()`
   - Build URI: `baseUrl + path + OData query params`
   - Attach `Authorization: Bearer {token}` from TokenManager
   - Attach `X-Request-Id: {UUID}`
   - `Content-Type: application/json` for POST/PATCH
   - Read response via `bodyBytes()` for JSON, parse with Jackson
   - Record metrics start/end

3. **Error parsing** per §9.2-9.3:
   - Parse `{ "code": "...", "message": { "lang": "...", "value": "..." } }`
   - Map status codes to exception subclasses
   - Enrich every exception with RequestContext (requestId, method, URI, elapsed, retryCount)

#### Retry Engine (§10)

4. **RetryConfig**: `maxRetries` (3), `initialBackoff` (1s), `backoffMultiplier` (2.0), `jitterFactor` (0.2), `retryableStatuses` ({429, 500, 502, 503, 504}), `retryOnConnectionFailure` (true).

5. **Retry policy** per §10.2:

   - 429: honor `Retry-After` header, else 5s/10s/20s, up to 3 retries
   - 5xx: exponential backoff + jitter, up to 3 retries
   - Connection failure: 2 retries for idempotent methods only (GET, PUT)
   - 401: 1 retry after token refresh via TokenManager (which handles the CredentialProvider cascade internally)
   - 400, 403, 404, 409: no retry

6. **Idempotency** per §10.3: POST/PATCH not retried by default, opt-in via `RetryPolicy` parameter.

### Acceptance Criteria

- GET includes Bearer token and X-Request-Id
- POST serializes body to PascalCase JSON
- 404 → `ShareFileNotFoundException` with context
- 429 with `Retry-After: 5` → waits 5s then retries
- 500 → retries with 1s, 2s, 4s backoff (±jitter)
- 401 → token refresh + retry once
- POST on 500 → throws immediately (unless opted in)
- Max retries exhausted → throws last exception
- Metrics counter incremented on each retry

---

## SF-09: ResourceRequestExecutor + ItemsClient

**Module:** `sharefile-sdk-client`
**Depends on:** SF-08
**Milestone:** v0.1
**Spec Reference:** §6.1-6.3 (Resource Clients), §6.8 Items endpoint matrix

### Goal

Implement the internal `ResourceRequestExecutor` helper and the first resource client: `ItemsClient`.

### Requirements

#### ResourceRequestExecutor (§6.1)

1. Package-private helper shared by all resource clients:

   ```java
   final class ResourceRequestExecutor {
       private final ShareFileHttpClient httpClient;
       private final String basePath;

       URI entityUri(String id) { ... }                        // /{basePath}({id})
       URI entityActionUri(String id, String action) { ... }   // /{basePath}({id})/{action}
       URI collectionUri() { ... }                             // /{basePath}
       URI compositeKeyUri(String... keys) { ... }             // /{basePath}(key1=v1,key2=v2)

       <T> T get(URI uri, ODataQuery query, Class<T> type) { ... }
       <T> T get(URI uri, ODataQuery query, TypeReference<T> type) { ... }
       <T> T post(URI uri, Object body, Class<T> type) { ... }
       <T> T patch(URI uri, Object body, Class<T> type) { ... }
       void delete(URI uri) { ... }
       <T> ODataFeed<T> getCollection(URI uri, ODataQuery query, TypeReference<ODataFeed<T>> type) { ... }
       <T> ODataFeed<T> getNextPage(ODataFeed<T> current, TypeReference<ODataFeed<T>> type) { ... }
   }
   ```

#### ItemsClient (§6.3 + §6.8 Items endpoint matrix)

2. **All methods** per §6.3 and the endpoint coverage matrix in §6.8. Use the matrix as the source of truth for HTTP method, endpoint URL, request/response types. Key methods:

   | SDK Method                       | HTTP   | Endpoint                    | Response                | Notes                              |
   | -------------------------------- | ------ | --------------------------- | ----------------------- | ---------------------------------- |
   | `getById(id)`                    | GET    | `/Items({id})`              | `Item`                  | Supports `home`, `favorites`, etc. |
   | `getChildren(id, query)`         | GET    | `/Items({id})/Children`     | `ODataFeed<Item>`       |                                    |
   | `getByPath(path)`                | GET    | `/Items/ByPath?path=...`    | `Item`                  |                                    |
   | `createFolder(parentId, req)`    | POST   | `/Items({parentId})/Folder` | `Item`                  |                                    |
   | `update(id, item)`               | PATCH  | `/Items({id})`              | `OperationResult<Item>` | **Async** if cross-zone            |
   | `copy(id, targetId, overwrite)`  | POST   | `/Items({id})/Copy?...`     | `OperationResult<Item>` | **Async** if cross-zone            |
   | `delete(id)`                     | DELETE | `/Items({id})`              | —                       |                                    |
   | `upload(folderId, file, opts)`   | —      | —                           | `UploadResult`          | Delegates to TransferClient        |
   | `download(itemId, target, opts)` | —      | —                           | —                       | Delegates to TransferClient        |

   (See full matrix in TECH_SPEC.md §6.8 for all ~25 methods)

3. Resource clients have **no public constructors** — created by `ShareFileClient` builder internally.

4. **No generic CRUD base class.** Each method is explicitly defined with its specific endpoint path, parameters, and return type.

### Acceptance Criteria

- All ItemsClient methods map to correct HTTP method + URL per the endpoint matrix
- `update()` and `copy()` return `OperationResult<Item>`
- OData query params passed through correctly
- Special IDs (`home`, `favorites`) work in URL construction
- `compositeKeyUri()` produces correct OData composite key format
- Unit tests with mock transport for representative methods

---

## SF-10: AccessControlsClient + AsyncOperationsClient

**Module:** `sharefile-sdk-client`
**Depends on:** SF-08
**Milestone:** v0.1
**Spec Reference:** §6.4-6.5 (Resource Clients), §6.8 AccessControls + AsyncOperations endpoint matrices

### Goal

Implement `AccessControlsClient` (composite keys, async-polymorphic) and `AsyncOperationsClient` (with polling helper).

### Requirements

1. **AccessControlsClient** per §6.4 and §6.8 endpoint matrix:

   - Composite key: `getById(principalId, itemId)` → `GET /AccessControls(principalid={p},itemid={i})`
   - `getByItem(itemId)` → `GET /Items({itemId})/AccessControls`
   - `create(itemId, acl, recursive)` → `OperationResult<AccessControl>` (**async** if recursive=true)
   - `update(itemId, acl, recursive)` → `OperationResult<AccessControl>` (**async** if recursive=true)
   - `delete`, `bulkSet`, `bulkSetForPrincipal`, `clone`, `bulkDelete`, `notifyUsers`

2. **AsyncOperationsClient** per §6.5:
   - `getById(operationId)`, `list()`, `list(query)`
   - **Polling helper:**
     ```java
     public AsyncOperation awaitCompletion(String operationId, Duration timeout) {
         // Poll getById() until op.isTerminal(), sleep between polls
         // Throw ShareFileTimeoutException on timeout
     }
     ```

### Acceptance Criteria

- Composite key URL format correct: `AccessControls(principalid=x,itemid=y)`
- `create()` / `update()` return `OperationResult<AccessControl>`
- `awaitCompletion` returns on terminal state, throws on timeout
- Unit tests for async-polymorphic deserialization

---

## SF-11: SharesClient + UsersClient

**Module:** `sharefile-sdk-client`
**Depends on:** SF-08
**Milestone:** v0.1
**Spec Reference:** §6.6-6.7 (Resource Clients), §6.8 Shares + Users endpoint matrices

### Goal

Implement `SharesClient` and `UsersClient` with all their resource-specific methods.

### Requirements

1. **SharesClient** per §6.6 and §6.8 endpoint matrix:

   - `getById`, `list`, `update`, `delete`
   - **Explicit share creation** (not generic `create`): `createSendShare(req)`, `createRequestShare(req)`
   - `getByUser(userId)`, `getRecipients(shareId)`, `sendNotification(shareId, req)`
   - `getItems(shareId)`, `downloadItems(shareId)`

2. **UsersClient** per §6.7 and §6.8 endpoint matrix:
   - `getById`, `list`, `create`, `update`, `delete`
   - `getCurrentUser()`, `getByEmail(email)` (filter-based lookup)
   - `getPreferences`, `updatePreferences`, `getSecurity`, `updateSecurity`
   - `resetPassword`, `sendWelcomeEmail`, `getGroups(userId)`

### Acceptance Criteria

- SharesClient.createSendShare and createRequestShare both POST to `/Shares` with different request bodies
- UsersClient.getByEmail uses `$filter=Email eq '{email}'`
- UsersClient.getCurrentUser uses `GET /Users` (returns authenticated user)
- Unit tests for representative methods

---

## SF-12: TransferClient (Upload + Download)

**Module:** `sharefile-sdk-client`
**Depends on:** SF-08
**Milestone:** v0.1
**Spec Reference:** §12 (File Upload & Download), §6.8 File Transfers endpoint matrix

### Goal

Implement the upload and download pipelines with streaming transport, sync/async APIs, and cooperative cancellation.

### Requirements

#### Upload Pipeline (§12.2-12.5)

1. **TransferClient** (was TransferService) per §12.2:

   ```java
   public final class TransferClient {
       UploadResult upload(String folderId, Path file, UploadOptions options);
       UploadResult upload(String folderId, InputStream stream, String fileName, long fileSize, UploadOptions options);
       UploadHandle uploadAsync(String folderId, Path file, UploadOptions options);
       UploadResult uploadToShare(String shareId, Path file, UploadOptions options);
       UploadHandle uploadToShareAsync(String shareId, Path file, UploadOptions options);
   }
   ```

2. **Three-phase pipeline** per §12.4:

   - Phase 1 (Negotiate): POST `/Items({folderId})/Upload2` → `UploadSpecification`
   - Phase 2 (Transfer): POST file bytes via **streaming `InputStream`** to ChunkUri using **unauthenticated** `HttpTransport`. Uses `HttpRequests.streaming()` — never buffers entire file.
     - Standard: single POST, file < 4MB
     - Streamed: single POST with chunked encoding, 4MB-256MB
     - Threaded: parallel chunk uploads via `ExecutorService`, > 256MB
   - Phase 3 (Finalize): POST to FinishUri (threaded only)

3. Auto-select upload method by file size. Resume support via server's `IsResume`/`ResumeIndex`.

4. Threaded upload: parallel chunks, per-chunk retry, cooperative cancellation via `AtomicBoolean`, progress tracking.

#### Download Pipeline (§12.6-12.7)

5. **Download methods:**

   ```java
   void download(String itemId, Path target, DownloadOptions options);
   InputStream downloadStream(String itemId, DownloadOptions options);
   DownloadSpecification resolveDownloadUrl(String itemId);
   void bulkDownload(String parentId, List<String> itemIds, Path target, DownloadOptions options);
   DownloadHandle downloadAsync(String itemId, Path target, DownloadOptions options);
   ```

6. Two-phase: resolve URL (`GET /Items({id})/Download?redirect=false`) → stream bytes from storage zone via **`bodyStream()`** on unauthenticated transport. 64KB buffer, progress tracking, cancellation.

#### Async Handles (§12.3)

7. `UploadHandle` and `DownloadHandle` per §12.3: `CompletableFuture`, `progress()`, `cancel()`, `awaitOrThrow(timeout)`.

8. `TransferProgress`: bytesTransferred, totalBytes, percentComplete, elapsed, bytesPerSecond, estimatedTimeRemaining, state, chunksCompleted, totalChunks.

9. All transfer exceptions per §12.10.

### Acceptance Criteria

- Standard upload: single POST with streamed file body < 4MB
- Threaded upload: parallel chunks with configurable thread count
- Download streams bytes to file via `bodyStream()` — never buffers entire file
- Resume: skips already-uploaded chunks
- Cancel: `handle.cancel()` stops cooperatively
- Progress listener receives updates
- Unauthenticated transport for storage zone calls
- Correct exceptions at each failure point

---

## SF-13: MetricsProvider SPI & NoopMetrics

**Module:** `sharefile-sdk-client`
**Depends on:** SF-06
**Milestone:** v0.1
**Spec Reference:** §11 (Observability, Metrics & Alerting)

### Goal

Implement the `MetricsProvider` SPI, `NoopMetricsProvider`, metric name constants, SLF4J logging, and health check.

### Requirements

1. **MetricsProvider** interface per §11.1: `startTimer()`, `recordRequest()`, `recordError()`, `incrementCounter()`, `recordValue()`, `setGauge()`, inner `Timer` interface.

2. **NoopMetricsProvider** — singleton, all no-ops.

3. **Metric name constants** per §11.2.

4. **SLF4J logging** per §11.4 at correct levels with sensitive field redaction at TRACE.

5. **HealthStatus check** per §11.5.

### Acceptance Criteria

- `MetricsProvider.noop()` returns singleton with no-op behavior
- Metric name constants defined and used consistently
- Sensitive fields redacted in TRACE logs

---

## SF-14: ShareFileClient Builder & Lifecycle

**Module:** `sharefile-sdk-client`
**Depends on:** SF-07, SF-08, SF-09, SF-10, SF-11, SF-12, SF-13
**Milestone:** v0.1
**Spec Reference:** §3 (ShareFileClient Builder)

### Goal

Implement `ShareFileClient` and `ShareFileClientBuilder` — the main entry point that wires all components together.

### Requirements

1. **ShareFileClient** per §3.1:

   - `static builder()` returns `ShareFileClientBuilder`
   - Resource client accessors: `items()`, `users()`, `shares()`, `accessControls()`, `asyncOperations()`, `transfers()`, plus stubs for tier 2/3 clients (groups, webhooks, sessions, accounts, zones)
   - `checkHealth()` per §11.5
   - Implements `AutoCloseable`

2. **ShareFileClientBuilder** per §3.2:

   - Required: `subdomain(String)`
   - Credentials: `credentialProvider(CredentialProvider)`, `clientCredentials()` + `authorizationCode()` (recommended), `accessToken()`, `passwordGrant()` (legacy)
   - Optional: `apiControlPlane`, timeouts, `httpTransport`, `tokenStore`, `metricsProvider`, `objectMapper`, `executor`, `retryConfig`, `tokenRefreshBuffer`, `baseUrl` (testing)
   - `build()` validates, wires all components, returns `ShareFileClient`

3. **build() wiring** per §3.2: creates config → transport → credential provider → token manager → ObjectMapper → retry engine → http client → resource clients → transfer client.

4. **Lifecycle** per §3.4: `close()` cancels scheduler, shuts down executor (30s grace), closes SDK-owned transport. Caller-provided resources not closed.

### Acceptance Criteria

- Authorization code builder: `builder().subdomain("x").clientCredentials("id","secret").authorizationCode(code).build()` succeeds
- Dynamic builder: `builder().subdomain("x").credentialProvider(provider).build()` succeeds
- Missing subdomain → `IllegalStateException`
- Missing credentials → `IllegalStateException`
- `close()` cleans up internal resources
- Caller-provided executor survives `close()`
- All Tier 1 resource clients accessible

---

## SF-15: Pagination (Manual + Auto-Paginator)

**Module:** `sharefile-sdk-client`
**Depends on:** SF-09
**Milestone:** v0.2
**Spec Reference:** §13 (Pagination)

### Goal

Implement manual pagination via `getNextPage()` and auto-pagination via `listAllChildren()` / `streamAllChildren()` on `ItemsClient`, plus generic support in `ResourceRequestExecutor`.

### Requirements

1. **Manual pagination**: `ResourceRequestExecutor.getNextPage(feed)` follows `odata.nextLink`.

2. **Auto-paginator** (`ODataPaginator`): lazy `Iterator`/`Spliterator` that fetches next page only when current page is exhausted.

3. `ItemsClient.listAllChildren(folderId)` → `Iterable<Item>`, `streamAllChildren(folderId)` → `Stream<Item>`.

4. Pattern reusable for other resource clients that support collection queries.

### Acceptance Criteria

- Auto-paginator follows 3 pages of results, yielding all items
- `streamAllChildren()` is lazy — doesn't fetch page 2 until page 1 consumed
- Unit test with mock transport returning 3 pages with nextLink

---

## SF-16: Tier 2 Resource Clients (Groups, Webhooks, Sessions)

**Module:** `sharefile-sdk-client`
**Depends on:** SF-14
**Milestone:** v0.2
**Spec Reference:** §6.8 Groups + WebhookSubscriptions endpoint matrices

### Goal

Implement `GroupsClient`, `WebhookSubscriptionsClient`, and `SessionsClient`.

### Requirements

1. **GroupsClient** per §6.8: `getById`, `list`, `create`, `update`, `delete`, `getMembers`, `addMember`, `removeMember`.

2. **WebhookSubscriptionsClient** per §6.8: `getById`, `list`, `create`, `delete`.

3. **SessionsClient**: `login`, `logout`, `get` (basic session management).

4. Register all three on `ShareFileClient` accessors.

### Acceptance Criteria

- All methods map to correct endpoints
- GroupsClient member management uses nested URLs
- WebhookSubscriptions CRUD works

---

## SF-17: Spring Boot Starter — Auto-Configuration

**Module:** `sharefile-spring-boot-starter`
**Depends on:** SF-14
**Milestone:** v0.3
**Spec Reference:** §14.1-14.2, §14.5 (Spring Boot Integration)

### Goal

Implement Spring Boot auto-configuration that creates a `ShareFileClient` bean from `application.yml` properties, with support for user-defined `CredentialProvider` beans.

### Requirements

1. **ShareFileAutoConfiguration** per §14.1:

   - `@AutoConfiguration`, `@ConditionalOnClass`, `@EnableConfigurationProperties`
   - Accepts `Optional<CredentialProvider>` — overrides properties if present
   - Accepts `Optional<MeterRegistry>` — plugs in `MicrometerMetricsProvider`
   - Accepts `Optional<RestClient.Builder>` — plugs in `RestClientHttpTransport`

2. **ShareFileProperties** per §14.2 with all nested classes.

3. **RestClientHttpTransport** per §4.3: implements streaming `HttpTransport` via Spring's `RestClient`.

4. Spring Boot META-INF auto-configuration imports file.

### Acceptance Criteria

- `sharefile.subdomain=test` auto-creates `ShareFileClient` bean
- Custom `CredentialProvider` bean overrides properties-based auth
- `@ConditionalOnMissingBean` allows user-defined `ShareFileClient`

---

## SF-18: Spring Boot Starter — Micrometer Metrics & Health

**Module:** `sharefile-spring-boot-starter`
**Depends on:** SF-17
**Milestone:** v0.3
**Spec Reference:** §11.3 (Micrometer), §14.3 (Health Indicator)

### Goal

Implement `MicrometerMetricsProvider` and `ShareFileHealthIndicator`.

### Requirements

1. **MicrometerMetricsProvider** per §11.3 — implements `MetricsProvider` using Micrometer `MeterRegistry`.

2. **ShareFileHealthIndicator** per §14.3 — Actuator health at `/actuator/health/sharefile`.

3. Distributed tracing support per §14.4 (inherited via RestClient observation context).

### Acceptance Criteria

- `sharefile.http.requests` timer recorded in MeterRegistry
- Health returns UP with subdomain + token expiry
- Health returns DOWN with error on failure

---

## SF-19: Test Infrastructure & Fixtures

**Module:** `sharefile-sdk-test`
**Depends on:** SF-14
**Milestone:** v0.1
**Spec Reference:** §15 (Testing Strategy)

### Goal

Create `MockHttpTransport`, JSON fixture files, fixture loader, and `@ShareFileMockServer` for Spring Boot tests.

### Requirements

1. **MockHttpTransport**: enqueue responses, record requests, support streaming responses.

2. **JSON fixtures** in `src/main/resources/fixtures/`: items, users, shares, auth, errors, async, upload, download — all with valid `odata.type` and PascalCase fields.

3. **ShareFileFixtures** loader utility.

4. **@ShareFileMockServer** annotation for Spring Boot integration tests.

### Acceptance Criteria

- MockHttpTransport enqueues and records
- Fixtures deserialize to correct model types
- @ShareFileMockServer starts WireMock + configures Spring context

---

## SF-20: CI/CD & Publishing

**Module:** Root (GitHub Actions)
**Depends on:** SF-01
**Milestone:** v1.0
**Spec Reference:** §18.5 (CI/CD)

### Goal

Create GitHub Actions workflows for CI, snapshots, and releases.

### Requirements

1. **CI** (`.github/workflows/ci.yml`): push/PR → build + test, cache Gradle, upload reports.
2. **Snapshot** (`.github/workflows/snapshot.yml`): merge to main → publish to GitHub Packages + Sonatype Snapshots.
3. **Release** (`.github/workflows/release.yml`): manual trigger → strip SNAPSHOT, sign, publish to Maven Central, tag, bump version.

### Acceptance Criteria

- CI runs on PRs
- Snapshots publish on merge to main
- Release signs, publishes, tags, and bumps
