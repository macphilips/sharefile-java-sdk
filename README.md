# ShareFile Java SDK

Framework-agnostic Java 17 SDK for the ShareFile REST API v3, with a multi-module Gradle build and an optional Spring Boot starter module.

This repository is currently a `1.0.0-SNAPSHOT`. The architecture and planned API surface are documented in [`docs/TECH_SPEC.md`](docs/TECH_SPEC.md) and [`docs/TICKETS.md`](docs/TICKETS.md), while the codebase today implements the SDK foundation: models, OData support, Jackson configuration, typed exceptions, authentication/token management, retry primitives, and a streaming JDK HTTP transport.

## Status

What exists in the current snapshot:

- `sharefile-sdk-core`
  - ShareFile/OData models
  - request/response DTOs
  - enum types
  - OData query builder and filter DSL
  - Jackson `ObjectMapper` factory for ShareFile JSON
  - typed exception hierarchy
- `sharefile-sdk-client`
  - `ShareFileConfig`
  - credential and token store SPIs
  - `TokenManager`
  - `HttpTransport` abstraction
  - `JdkHttpTransport`
  - retry configuration primitives
  - internal HTTP client helpers
- `sharefile-sdk-bom`
  - dependency alignment for published modules

What is not implemented yet in this snapshot:

- public `ShareFileClient` entry point
- resource clients such as `ItemsClient`, `SharesClient`, or `UsersClient`
- Spring Boot auto-configuration and health/metrics integration
- test-fixture utilities in `sharefile-sdk-test`

If you need the planned architecture, module responsibilities, or rollout order, use the docs in [`docs/`](docs/).

## Modules

| Module                          | Purpose                                                       | Current state |
| ------------------------------- | ------------------------------------------------------------- | ------------- |
| `sharefile-sdk-core`            | Models, OData helpers, Jackson config, exceptions             | Implemented   |
| `sharefile-sdk-client`          | Auth, config, HTTP transport, retry, internal client plumbing | Implemented   |
| `sharefile-spring-boot-starter` | Spring Boot 3 integration                                     | Scaffold only |
| `sharefile-sdk-bom`             | BOM for version alignment                                     | Implemented   |
| `sharefile-sdk-test`            | Shared test fixtures and utilities                            | Scaffold only |

## Requirements

- Java 17+
- Gradle wrapper included in the repo

## Build

```bash
./gradlew build
```

Run tests:

```bash
./gradlew test
```

Build a specific module:

```bash
./gradlew :sharefile-sdk-core:build
./gradlew :sharefile-sdk-client:build
```

Publish snapshots to your local Maven cache:

```bash
./gradlew publishToMavenLocal
```

## Project Layout

```text
sharefile-java-sdk/
├── sharefile-sdk-core/
├── sharefile-sdk-client/
├── sharefile-spring-boot-starter/
├── sharefile-sdk-bom/
├── sharefile-sdk-test/
└── docs/
```

## Current Usage

There is no top-level `ShareFileClient` yet, so the current code is most useful for integration work at the foundation layer.

### Create SDK configuration

```java
import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import java.time.Duration;

ShareFileConfig config = ShareFileConfig.builder()
    .subdomain("mycompany")
    .connectTimeout(Duration.ofSeconds(10))
    .readTimeout(Duration.ofSeconds(30))
    .build();

String baseUrl = config.getBaseUrl();
String tokenEndpoint = config.getTokenEndpointUrl();
```

### Build credentials

Authorization code flow:

```java
import io.github.indraftapp.sharefile.client.spi.Credentials;

Credentials credentials = Credentials.builder()
    .clientCredentials("client-id", "client-secret")
    .authorizationCode("oauth-code")
    .build();
```

Password grant for legacy or internal automation scenarios:

```java
Credentials credentials = Credentials.builder()
    .clientCredentials("client-id", "client-secret")
    .passwordGrant("service-account@example.com", "password")
    .build();
```

### Resolve credentials dynamically

```java
import io.github.indraftapp.sharefile.client.spi.CredentialProvider;
import io.github.indraftapp.sharefile.client.spi.Credentials;

CredentialProvider provider = () -> Credentials.builder()
    .clientCredentials(System.getenv("SF_CLIENT_ID"), System.getenv("SF_CLIENT_SECRET"))
    .passwordGrant(System.getenv("SF_USER"), System.getenv("SF_PASS"))
    .build();
```

The SDK only calls `CredentialProvider.resolve()` for initial authentication, explicit re-authentication, or refresh-token failure fallback. Normal refresh cycles use the cached token plus refresh token.

### Create a ShareFile-tuned `ObjectMapper`

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.core.jackson.ShareFileObjectMapper;

ObjectMapper mapper = ShareFileObjectMapper.create();
```

This mapper is configured for:

- ShareFile PascalCase JSON properties
- Java time types
- OData polymorphic entity deserialization
- unknown-property tolerance

### Build OData queries

```java
import io.github.indraftapp.sharefile.core.odata.Filter;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import io.github.indraftapp.sharefile.core.odata.SortDirection;

ODataQuery query = ODataQuery.builder()
    .select("Id", "Name", "CreationDate")
    .expand("Parent")
    .filter(Filter.eq("Name", "Reports"))
    .orderBy("CreationDate", SortDirection.DESC)
    .top(100)
    .build();
```

Serialize query options for a request:

```java
var params = query.toQueryParams();
```

### Use the streaming HTTP transport

```java
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.http.JdkHttpTransport;

HttpTransport transport = new JdkHttpTransport(config);
```

The transport is designed for streaming uploads and downloads and avoids buffering large bodies in memory.

## Dependency Coordinates

The Gradle group is:

```text
io.github.indraftapp
```

Current version:

```text
1.0.0-SNAPSHOT
```

Until the SDK is published, the practical workflow is to build or publish it locally from this repository.

## Publishing

The build is set up for Sonatype OSSRH / Maven Central style publishing. The root build reads these environment variables when publishing:

- `OSSRH_USERNAME`
- `OSSRH_PASSWORD`
- `GPG_SIGNING_KEY`
- `GPG_SIGNING_PASSWORD`

Example:

```bash
./gradlew publishToSonatype
./gradlew closeAndReleaseSonatypeStagingRepository
```

## Documentation

- Technical design: [`docs/TECH_SPEC.md`](docs/TECH_SPEC.md)
- API planning and rollout: [`docs/TICKETS.md`](docs/TICKETS.md)
- Endpoint and model reference: [`docs/API_REFERENCE.md`](docs/API_REFERENCE.md)

## License

MIT
