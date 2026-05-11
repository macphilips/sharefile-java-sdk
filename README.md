# ShareFile Java SDK

[![CI](https://github.com/macphilips/sharefile-java-sdk/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/macphilips/sharefile-java-sdk/actions/workflows/ci.yml)
![Coverage](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/macphilips/sharefile-java-sdk/main/.github/badges/jacoco.json)

Framework-agnostic Java 17 SDK for the ShareFile REST API v3, with an optional Spring Boot 3 starter and a shared test-support module.

## Status

This repository is currently `1.0.0-SNAPSHOT`.

The current codebase includes:

- `sharefile-sdk-core`
  - ShareFile/OData models and enums
  - request/response DTOs
  - OData query builder and filter DSL
  - Jackson configuration for ShareFile JSON
  - typed exception hierarchy
- `sharefile-sdk-client`
  - `ShareFileClient` and fluent builder
  - OAuth/token lifecycle management
  - streaming HTTP transport abstraction and JDK transport
  - retry, metrics, and logging hooks
  - Tier 1 and Tier 2 resource clients implemented in the repo today:
    - `ItemsClient`
    - `UsersClient`
    - `SharesClient`
    - `AccessControlsClient`
    - `AsyncOperationsClient`
    - `TransferClient`
    - `GroupsClient`
    - `WebhookSubscriptionsClient`
    - `SessionsClient`
    - minimal `AccountsClient`
- `sharefile-spring-boot-starter`
  - Boot auto-configuration
  - configuration properties
  - Actuator health indicator
  - Micrometer metrics provider
  - Spring `RestClient` transport adapter
- `sharefile-sdk-test`
  - `MockHttpTransport`
  - shared JSON fixtures
  - `ShareFileFixtures`
  - `@ShareFileMockServer` for Spring/WireMock integration tests
- `sharefile-sdk-bom`
  - dependency alignment for published modules

## Modules

| Module                          | Purpose                                                       |
| ------------------------------- | ------------------------------------------------------------- |
| `sharefile-sdk-core`            | Models, OData helpers, Jackson config, exceptions             |
| `sharefile-sdk-client`          | Public SDK facade, resource clients, auth, transfers, retries |
| `sharefile-spring-boot-starter` | Spring Boot 3 integration                                     |
| `sharefile-sdk-test`            | Shared test transport, fixtures, and WireMock support         |
| `sharefile-sdk-bom`             | BOM for version alignment                                     |

## Requirements

- Java 17+
- Gradle wrapper included in the repository

## Build And Test

CI-equivalent commands from the repository root:

```bash
./gradlew clean test jacocoTestReport
./gradlew clean check
```

Useful targeted commands:

```bash
./gradlew :sharefile-sdk-core:test
./gradlew :sharefile-sdk-client:test
./gradlew :sharefile-spring-boot-starter:test
./gradlew :sharefile-sdk-test:test
```

Publish to the local Maven cache:

```bash
./gradlew publishToMavenLocal
```

## Coverage

Aggregate JaCoCo coverage is generated at:

- XML: `build/reports/jacoco/test/jacocoTestReport.xml`
- HTML: `build/reports/jacoco/test/html/index.html`

The coverage badge source file is:

- `.github/badges/jacoco.json`

## Basic Usage

```java
import io.github.indraftapp.sharefile.client.ShareFileClient;

try (ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .clientCredentials("client-id", "client-secret")
    .authorizationCode("oauth-code")
    .build()) {

  var currentUser = client.users().getCurrentUser();
  var home = client.items().getById("home");
}
```

Seeded-token flow:

```java
ShareFileClient client = ShareFileClient.builder()
    .subdomain("mycompany")
    .clientCredentials("client-id", "client-secret")
    .accessToken("access-token", "refresh-token")
    .build();
```

## Spring Boot Starter

The starter uses the `sharefile.*` property prefix.

```yaml
sharefile:
  subdomain: mycompany
  auth:
    client-id: your-client-id
    client-secret: your-client-secret
    grant-type: authorization_code
    code: your-auth-code
```

## Publishing

### Snapshot Publishing

Pushes to `main` run the snapshot workflow, which:

- verifies the build and tests
- publishes to GitHub Packages
- publishes snapshots to Sonatype Snapshots

Required secrets:

- `OSSRH_USERNAME`
- `OSSRH_PASSWORD`

GitHub Packages publishing uses the default GitHub Actions token.

### Release Publishing

The release workflow is `workflow_dispatch` only. It:

1. derives the release version from `gradle.properties`
2. runs the full build, checks, and aggregate coverage
3. signs and publishes artifacts to Maven Central via Sonatype
4. creates and pushes a Git tag
5. bumps `gradle.properties` to the next `-SNAPSHOT`

Additional release secrets:

- `GPG_SIGNING_KEY`
- `GPG_SIGNING_PASSWORD`

## Documentation

- [Technical design](docs/TECH_SPEC.md)
- [Implementation tickets](docs/TICKETS.md)
- [Endpoint and model reference](docs/API_REFERENCE.md)

## License

MIT
