# detekt-rules-koin

[![Latest Release](https://img.shields.io/github/v/release/androidbroadcast/Koin-Detekt)](https://github.com/androidbroadcast/Koin-Detekt/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/dev.androidbroadcast.rules.koin/detekt-koin4-rules)](https://central.sonatype.com/artifact/dev.androidbroadcast.rules.koin/detekt-koin4-rules)
[![PR Validation](https://github.com/androidbroadcast/Koin-Detekt/actions/workflows/pr-validation.yml/badge.svg)](https://github.com/androidbroadcast/Koin-Detekt/actions/workflows/pr-validation.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Detekt extension with 51 rules for Koin 4.x — enforces best practices and catches common anti-patterns via static analysis.

## Installation

```kotlin
dependencies {
    detektPlugins("dev.androidbroadcast.rules.koin:detekt-koin4-rules:1.0.0")
}
```

## Rules

51 rules across 6 categories:

| Category | Rules |
|----------|-------|
| Service Locator | 5 |
| Module DSL | 13 |
| Scope Management | 8 |
| Platform | 8 |
| Architecture | 4 |
| Koin Annotations | 12 |

📖 [Complete Rule Documentation](docs/rules.md)

## Configuration

Add to `.detekt.yml`:

```yaml
koin-rules:
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes:
      - 'Application'
      - 'Activity'

  SingleForNonSharedDependency:
    active: true
    namePatterns:
      - '.*UseCase'
      - '.*Command'
```

📖 [Configuration Guide](docs/configuration.md)

## Requirements

- Kotlin 2.0+
- Detekt 1.23.8+
- Koin 4.x

## License

Licensed under the [Apache License 2.0](https://opensource.org/licenses/Apache-2.0).
