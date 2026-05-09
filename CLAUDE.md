# ShareFile Java SDK

This repository contains a framework-agnostic Java SDK for the ShareFile REST API v3, plus an optional Spring Boot 3 starter.

This file defines the rules AI coding agents must follow when working in this repository. Treat it as mandatory project guidance.

---

## 1. Project Status

The project is currently in **pre-implementation**.

The technical specification, API reference, and implementation tickets are expected to exist before source code is generated. Do not invent architecture outside the approved documents.

No production source code should be added until the relevant ticket has been selected and its dependencies are satisfied.

---

## 2. Source of Truth

Use these documents in this order:

1. `docs/TECH_SPEC.md` — primary architecture and design source of truth.
2. `docs/API_REFERENCE.md` — ShareFile REST API endpoint reference.
3. `docs/TICKETS.md` — implementation ticket index and dependency graph.
4. `docs/tickets/` — individual ticket files, `SF-01` through `SF-20`.
5. Official ShareFile docs only when a ticket or spec explicitly requires endpoint verification.

When documents conflict, prefer:

```text
TECH_SPEC.md > API_REFERENCE.md > TICKETS.md > individual ticket notes > generated code assumptions
```

If an endpoint, model field, or behavior is unclear, do **not** guess. Mark it as requiring verification and keep the implementation narrow.

---

## 3. Tech Stack

| Area                  | Standard                                             |
| --------------------- | ---------------------------------------------------- |
| Java                  | Java 17+                                             |
| Build                 | Gradle with Kotlin DSL                               |
| Core HTTP             | `java.net.http.HttpClient`                           |
| Spring HTTP adapter   | Spring `RestClient`                                  |
| Serialization         | Jackson                                              |
| Boilerplate reduction | Lombok, compile-only with annotation processing      |
| JSON style            | ShareFile/OData v3 JSON Light, PascalCase properties |
| Framework requirement | None for core/client modules                         |
| Optional framework    | Spring Boot 3 starter                                |
| Group ID              | `io.github.indraftapp`                               |

Use only dependencies approved in the tech spec or ticket being implemented.

---

## 4. Module Structure

Expected modules:

| Module                          | Purpose                                                                                   |
| ------------------------------- | ----------------------------------------------------------------------------------------- |
| `sharefile-sdk-core`            | Models, enums, OData builder, exceptions. No application framework dependencies.          |
| `sharefile-sdk-client`          | Resource clients, HTTP transport, authentication, retry, transfer pipeline. Pure Java 17. |
| `sharefile-spring-boot-starter` | Auto-configuration, configuration properties, Micrometer metrics, Actuator health.        |
| `sharefile-sdk-bom`             | BOM for dependency/version alignment.                                                     |
| `sharefile-sdk-test`            | WireMock fixtures, `MockHttpTransport`, test utilities, `@ShareFileMockServer`.           |

Do not collapse modules unless explicitly instructed.

---

## 5. Settled Architecture Decisions

These decisions are settled. Do not propose alternatives unless the user explicitly asks for a redesign.

### 5.1 Framework-Agnostic Core

`sharefile-sdk-core` and `sharefile-sdk-client` must not depend on Spring, Spring Boot, Micrometer, Resilience4j, or other application frameworks.

Spring integration belongs only in `sharefile-spring-boot-starter`.

### 5.2 Explicit Resource Clients

Use explicit resource clients:

```java
client.items()
client.shares()
client.accessControls()
client.asyncOperations()
client.users()
client.groups()
client.webhookSubscriptions()
```

Do not create a public generic CRUD base class.

ShareFile resources are not uniform enough for generic public CRUD APIs because they include composite keys, action endpoints, redirects, upload/download flows, and async-polymorphic responses.

### 5.3 Internal ResourceRequestExecutor

Use a package-private `ResourceRequestExecutor` for shared implementation concerns:

- URI construction
- OData query handling
- JSON serialization/deserialization
- request execution
- response parsing
- OData feed handling

It must not be part of the public API.

### 5.4 Streaming HTTP Transport

The HTTP transport must support streaming request and response bodies.

Never require multi-GB uploads/downloads to be represented as `byte[]`.

Acceptable:

```java
Optional<InputStream> bodyStream();
InputStream bodyStream();
byte[] bodyBytes(int maxBytes);
```

Not acceptable for file transfers:

```java
byte[] body();
byte[] responseBody();
```

Small JSON payloads may be buffered internally when appropriate.

### 5.5 Authentication and Credential Resolution

`CredentialProvider.resolve()` must only be called for:

- initial authentication
- explicit re-authentication
- refresh-token failure fallback

Normal token refresh must use:

```text
client_id
client_secret
refresh_token
```

Do not call `CredentialProvider.resolve()` for every API request or every normal refresh cycle.

### 5.6 Password Grant Positioning

Authorization-code OAuth is the preferred production flow.

Password grant may exist for:

- legacy service-account workflows
- internal automation
- development/testing scenarios
- accounts where ShareFile configuration permits it

Do not present password grant as the recommended default flow in docs, examples, or tests.

### 5.7 OperationResult

Use `OperationResult<T>` for endpoints that may return either the expected entity or an `AsyncOperation`.

Example:

```java
OperationResult<Item> result = client.items().copy(itemId, targetFolderId, false);
```

Do not hide async responses or pretend these operations are always synchronous.

### 5.8 Retry Policy

Default retry behavior:

| Method / Condition | Default                                 |
| ------------------ | --------------------------------------- |
| `GET`              | Retry transient failures                |
| `PUT`              | Retry transient failures only when safe |
| `POST`             | No retry unless explicitly opted in     |
| `PATCH`            | No retry unless explicitly opted in     |
| `DELETE`           | No retry unless explicitly opted in     |
| `401`              | Refresh token and retry once            |
| `429`              | Honor `Retry-After` when available      |
| `5xx`              | Retry only if method is retry-safe      |

Do not add default retry-on-delete behavior.

### 5.9 Spring Boot Property Prefix

Use:

```yaml
sharefile:
  subdomain: mycompany
```

Do not use:

```yaml
spring:
  sharefile:
```

---

## 6. Implementation Order

Follow ticket dependency order from `docs/TICKETS.md`.

Do not implement later tickets before their dependencies are complete.

Expected milestones:

### v0.1 MVP

Core SDK foundation:

```text
SF-01 → SF-14, SF-19
```

Includes:

- Gradle/module setup
- core models
- exceptions
- OData support
- HTTP transport
- authentication/token management
- `ItemsClient`
- `TransferClient`
- `AsyncOperationsClient`
- Tier 1 client support as defined by tickets
- test fixtures

### v0.2

```text
SF-15, SF-16
```

Includes:

- pagination helpers
- Tier 2 resource clients

### v0.3

```text
SF-17, SF-18
```

Includes:

- Spring Boot starter
- configuration properties
- health indicator
- Micrometer metrics

### v1.0

```text
SF-20
```

Includes:

- CI/CD
- publishing
- Maven local/public publishing support
- release readiness

---

## 7. Coding Conventions

### 7.1 Package Names

Use:

```text
io.github.indraftapp
```

Example:

```text
io.github.indraftapp.sharefile.core
io.github.indraftapp.sharefile.client
io.github.indraftapp.sharefile.spring
```

Do not use `com.sharefile.*` or any namespace that implies this is an official Citrix/ShareFile SDK.

### 7.2 Public API Style

Prefer:

```java
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .clientCredentials(clientId, clientSecret)
    .accessToken(accessToken, refreshToken)
    .build();

Item home = client.items().getById("home");
```

Resource clients should have no public constructors. They are created internally by `ShareFileClient`.

### 7.3 Jackson

Use explicit Jackson annotations for ShareFile/OData fields.

Example:

```java
@JsonProperty("Id")
private String id;

@JsonProperty("odata.type")
private String type;
```

Use PascalCase JSON names matching the ShareFile API.

Configure Jackson with:

- `FAIL_ON_UNKNOWN_PROPERTIES = false`
- Java Time support
- PascalCase support where useful
- explicit handling for OData metadata fields

### 7.4 Lombok

Use Lombok to reduce repetitive boilerplate when it does not obscure behavior or change API semantics.

Preferred usage:

- `@Getter` / `@Setter` for straightforward Java beans and DTOs
- targeted constructor annotations when they clearly replace trivial boilerplate
- compile-only / annotation-processor wiring only, never as a runtime dependency

Use Lombok conservatively:

- keep explicit methods when behavior is non-trivial
- preserve Jackson annotations and field-level JSON mappings
- preserve JavaDoc on public API types and non-obvious methods
- avoid `@Data` on SDK models
- avoid Lombok-generated `equals`, `hashCode`, or `toString` on recursive or graph-shaped entities unless explicitly required by a ticket

### 7.5 Exceptions

SDK exceptions are unchecked.

Follow the exception hierarchy defined in the tech spec and ticket `SF-03`.

Do not throw raw `IOException`, `InterruptedException`, `HttpTimeoutException`, or Jackson exceptions from public SDK methods. Wrap them in SDK-specific exceptions.

### 7.6 Thread Safety

`ShareFileClient` and resource clients should be safe for concurrent use unless explicitly documented otherwise.

`TokenManager` must prevent duplicate simultaneous authentication/refresh requests. Use the locking strategy defined in the spec.

If implementing async operations, preserve interrupt status when catching `InterruptedException`.

### 7.7 Visibility

Use the narrowest visibility possible.

| Type                            | Visibility                             |
| ------------------------------- | -------------------------------------- |
| Public SDK API                  | `public`                               |
| Resource client implementations | `public final` only if part of SDK API |
| Internal helpers                | package-private                        |
| DTO internals                   | package-private where possible         |
| Test fixtures                   | test module only                       |

Do not expose internal infrastructure as public API unless the spec requires it.

---

## 8. ShareFile API Implementation Rules

### 8.1 Verify Endpoint Mappings

Before implementing any endpoint, check the corresponding endpoint matrix in `docs/TECH_SPEC.md` and `docs/API_REFERENCE.md`.

If the matrix row is marked draft, unverified, or unclear, do not invent behavior. Update the ticket or add a TODO comment only if the ticket explicitly allows it.

### 8.2 URL Format

Base API format:

```text
https://{subdomain}.sf-api.com/sf/v3
```

Entity format:

```text
/sf/v3/{Entity}
/sf/v3/{Entity}({id})
```

Do not hardcode a single tenant subdomain in reusable code.

### 8.3 OData

Support common OData query options:

```text
$select
$expand
$filter
$orderby
$top
$skip
```

Do not concatenate unescaped query strings manually. Use the OData query builder / URI builder.

### 8.4 OData Feeds

OData collection responses should be represented as:

```java
ODataFeed<T>
```

Support:

- `value`
- `odata.count`
- `odata.nextLink`

Pagination helpers must follow `odata.nextLink` lazily.

### 8.5 Redirects

Do not automatically follow redirects blindly for ShareFile download/upload flows.

Download and transfer flows should prefer SDK-controlled resolution:

```text
GET /Items({id})/Download?redirect=false
```

Then stream from the returned download URL without attaching the ShareFile bearer token to storage-zone hosts.

Never forward `Authorization: Bearer ...` to redirect/storage-zone URLs unless the spec explicitly requires it.

### 8.6 Uploads

ShareFile upload is multi-phase.

Do not implement upload as a simple JSON POST.

Expected phases:

1. Negotiate upload with ShareFile API.
2. Upload file bytes to returned `ChunkUri`.
3. Finalize when required using `FinishUri`.

Uploads must stream from `Path` or `InputStream`.

### 8.7 Downloads

Downloads must stream to `Path`, `OutputStream`, or `InputStream`.

Do not load the entire file into memory.

### 8.8 Async Operations

If an endpoint can return `AsyncOperation`, return `OperationResult<T>`.

Do not deserialize blindly into the expected entity type.

Use `odata.type` to determine whether the response is the expected entity or `AsyncOperation`.

---

## 9. Security Rules

### 9.1 Secrets

Never log raw values for:

```text
Authorization
access_token
refresh_token
client_secret
password
ChunkUri
FinishUri
DownloadUrl
UploadUrl
```

Temporary upload/download URLs may contain embedded tokens. Treat them as secrets.

### 9.2 TLS

Use HTTPS only.

Do not add support for insecure HTTP fallback.

### 9.3 Credential Storage

The SDK must not prescribe credential persistence.

Use `CredentialProvider` and `TokenStore` SPIs so applications can store secrets in:

- database with encryption
- Vault
- cloud secret manager
- environment variables
- in-memory test fixtures

### 9.4 HMAC Validation

Authorization-code callback HMAC validation must follow the tech spec.

Do not exchange authorization codes before validation succeeds.

### 9.5 SSRF / Host Safety

SDK-generated API calls should target expected ShareFile hosts.

For upload/download URLs returned by ShareFile, avoid attaching SDK auth headers and avoid rewriting the returned URL.

---

## 10. Testing Requirements

Every ticket must include tests.

### 10.1 Unit Tests

Use unit tests for:

- OData query building
- model serialization/deserialization
- exception mapping
- token refresh behavior
- request URI construction
- retry decision logic

### 10.2 HTTP Tests

Use `MockHttpTransport` or WireMock fixtures for resource clients.

Do not call live ShareFile endpoints in automated tests.

### 10.3 Transfer Tests

Test transfer behavior with streams, not just byte arrays.

Cover:

- upload negotiation
- chunk upload success
- chunk upload failure
- resume behavior where implemented
- download streaming
- cancellation where implemented

### 10.4 Auth Tests

Cover:

- authorization-code token exchange
- pre-existing token usage
- refresh-token grant
- refresh failure fallback
- credential provider not called during normal refresh
- concurrent requests do not trigger duplicate refresh

### 10.5 Spring Boot Tests

Spring tests belong only in `sharefile-spring-boot-starter`.

Cover:

- auto-configuration
- property binding
- custom `CredentialProvider` bean override
- health indicator
- Micrometer metrics binding

---

## 11. Build Commands

After `SF-01` is implemented:

```bash
./gradlew build
./gradlew test
./gradlew publishToMavenLocal
```

Useful targeted commands:

```bash
./gradlew :sharefile-sdk-core:test
./gradlew :sharefile-sdk-client:test
./gradlew :sharefile-spring-boot-starter:test
```

Before considering a ticket complete, run the relevant module tests at minimum.

---

## 12. Documentation Requirements

When adding or changing public SDK APIs, update the relevant docs:

- `docs/TECH_SPEC.md` for architecture changes
- `docs/API_REFERENCE.md` for endpoint coverage changes
- `docs/TICKETS.md` if ticket scope/dependencies change
- module README files if added
- JavaDoc for public API types

Do not leave public methods undocumented if their behavior involves auth, retries, redirects, async operations, pagination, or streaming.

---

## 13. What Not To Do

Do not:

- Add Spring dependencies to `sharefile-sdk-core`.
- Add Spring dependencies to `sharefile-sdk-client`.
- Use a public generic CRUD base class for resource clients.
- Buffer file uploads/downloads as `byte[]`.
- Call `CredentialProvider.resolve()` on every request.
- Call `CredentialProvider.resolve()` during normal refresh-token grant.
- Recommend password grant as the default production auth flow.
- Auto-follow redirects while forwarding bearer tokens.
- Retry `POST`, `PATCH`, or `DELETE` by default.
- Hardcode tenant subdomains.
- Call live ShareFile APIs in automated tests.
- Expose internal helpers as public API.
- Add dependencies not approved by the spec or current ticket.
- Implement future-tier clients before their milestone unless explicitly instructed.
- Guess undocumented ShareFile endpoints.

---

## 14. Agent Workflow

When working on a task:

1. Read this `CLAUDE.md`.
2. Read the relevant ticket in `docs/tickets/`.
3. Read the related sections of `docs/TECH_SPEC.md`.
4. Check `docs/API_REFERENCE.md` for endpoint details.
5. Implement only the ticket scope.
6. Add or update tests.
7. Run the relevant Gradle test task.
8. Summarize:
   - files changed
   - design decisions made
   - tests added/run
   - any unresolved issues or endpoint verification gaps

If the requested change conflicts with this file, stop and explain the conflict before proceeding.

---

## 15. Definition of Done

A ticket is complete only when:

- Implementation matches the relevant ticket.
- Public APIs match the tech spec.
- Endpoint mappings are verified or explicitly marked as draft.
- Unit tests are added.
- HTTP behavior is covered by mock transport or WireMock where applicable.
- No forbidden dependencies were introduced.
- No secrets are logged.
- Streaming rules are respected.
- Relevant docs are updated.
- Relevant Gradle tasks pass.

Partial implementations should be clearly marked and should not be presented as complete.
