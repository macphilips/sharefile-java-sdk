# Development Workflow

This project enforces Java style and formatting through Gradle.

Gradle manages the Node.js and npm toolchain used by Spotless/Prettier. The first formatting run may download that toolchain into the local Gradle cache.

## Commands

Run automatic formatting:

```bash
./gradlew spotlessApply
```

Verify formatting only:

```bash
./gradlew spotlessCheck
```

Run Checkstyle for Java main and test sources:

```bash
./gradlew checkstyleMain checkstyleTest
```

Run the standard verification lifecycle:

```bash
./gradlew check
```

`./gradlew check` validates:

- Java tests
- Checkstyle for all Java modules
- Spotless formatting checks

## Checkstyle Reports

Each Java module writes Checkstyle reports to:

```text
<module>/build/reports/checkstyle/
```

Both XML and HTML reports are generated for `main` and `test` source sets.

## Git Hooks

Install the repository-local pre-commit hook with:

```bash
./gradlew installGitHooks
```

The installed hook runs:

```bash
./gradlew spotlessCheck checkstyleMain checkstyleTest
```

You can run the hook manually with:

```bash
.git/hooks/pre-commit
```

## Exceptional Bypass

Only bypass the hook in exceptional cases:

```bash
git commit --no-verify
```

If you bypass the hook, run the Gradle checks manually before pushing changes.
