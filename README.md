# ShareFile Java SDK

[![CI](https://github.com/macphilips/sharefile-java-sdk/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/macphilips/sharefile-java-sdk/actions/workflows/ci.yml)
![Coverage](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/macphilips/sharefile-java-sdk/main/.github/badges/jacoco.json)
![Branch Coverage](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/macphilips/sharefile-java-sdk/main/.github/badges/jacoco-branches.json)

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

Release-version override checks:

```bash
./gradlew properties
./gradlew -PreleaseVersion=1.0.0 properties
./gradlew -PreleaseVersion=1.0.0 clean test jacocoTestReport check
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

The coverage badge source files are:

- `.github/badges/jacoco.json`
- `.github/badges/jacoco-branches.json`

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

## Upload Examples

File-backed uploads can use ShareFile's threaded upload protocol with parallel chunk uploads:

```java
UploadResult result = client.transfers().upload(
    "fo-folder",
    Path.of("/tmp/report.pdf"),
    UploadOptions.builder()
        .overwrite(true)
        .threadCount(4)
        .build());
```

InputStream uploads are supported when you know the file name and exact byte size. They use
ShareFile's threaded upload protocol by default, but chunks are uploaded sequentially because a
plain `InputStream` is forward-only. Use the `Path` overload when you need parallel chunk uploads.

```java
Path audio = Path.of("/tmp/audio.wav");

try (InputStream in = Files.newInputStream(audio)) {
  UploadResult result = client.transfers().upload(
      "fo-folder",
      in,
      "audio.wav",
      Files.size(audio),
      UploadOptions.builder()
          .progressListener((sent, total) -> {
            System.out.printf("Uploaded %d of %d bytes%n", sent, total);
          })
          .build());
}
```

For non-blocking uploads, use `uploadAsync`. The call returns an `UploadHandle` immediately; for
stream-backed uploads, chunking still runs sequentially in the background.

```java
UploadHandle handle = client.transfers().uploadAsync(
    "fo-folder",
    inputStream,
    "audio.wav",
    sizeInBytes,
    UploadOptions.builder()
        .callback(new UploadCallback() {
          @Override
          public void onProgress(TransferProgress progress) {
            System.out.printf("Uploaded %d bytes%n", progress.getBytesTransferred());
          }

          @Override
          public void onCompleted(UploadResult result) {
            System.out.printf("Uploaded item id: %s%n", result.getItemId());
          }

          @Override
          public void onFailed(Throwable error) {
            error.printStackTrace();
          }
        })
        .build());
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
- stages Maven artifacts locally with Gradle
- publishes snapshots to Sonatype Central with JReleaser using Central's Maven snapshots repository

Required secrets:

- `CENTRAL_PORTAL_USERNAME`
- `CENTRAL_PORTAL_PASSWORD`
- `GPG_SIGNING_KEY`
- `GPG_SIGNING_PASSWORD`

GitHub Packages publishing uses the default GitHub Actions token. JReleaser also requires a non-empty GitHub token even when release creation is skipped, so the workflows pass `GITHUB_TOKEN` into the deploy step.

### Release Publishing

Releases are tag-driven. Pushing a tag that matches `v*.*.*` triggers the release workflow. It:

1. derives the release version from the Git tag by stripping the leading `v`
2. runs the full build, checks, and aggregate coverage
3. publishes to GitHub Packages with Gradle using `-PreleaseVersion=<tag-version>`
4. stages Maven artifacts with Gradle, then signs and publishes the release to Sonatype Central Portal with JReleaser

Maintainer flow:

1. merge release-ready changes to `main` through a pull request
2. confirm `gradle.properties` still contains the development snapshot version
3. create and push the release tag from the exact commit to publish, for example `v1.0.0`

The release workflow does not commit, push, retag, or edit `gradle.properties`, so it remains compatible with protected branches that require pull requests.

Required Central release secrets:

- `CENTRAL_PORTAL_USERNAME`
- `CENTRAL_PORTAL_PASSWORD`

These must be the Central Publisher Portal user token credentials, not the interactive website login password. The release workflow base64-encodes `username:password` into the Bearer token format required by the Portal Publisher API.

- `GPG_SIGNING_KEY`
- `GPG_SIGNING_PASSWORD`

## Documentation

- [Technical design](docs/TECH_SPEC.md)
- [Implementation tickets](docs/TICKETS.md)
- [Endpoint and model reference](docs/API_REFERENCE.md)

## License

MIT
