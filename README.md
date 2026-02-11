# detekt-rules-koin

[![PR Validation](https://github.com/androidbroadcast/Koin-Detekt/actions/workflows/pr-validation.yml/badge.svg)](https://github.com/androidbroadcast/Koin-Detekt/actions/workflows/pr-validation.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Gradle](https://img.shields.io/badge/Gradle-8.14.4-green.svg)](https://gradle.org)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)

Detekt 1.x extension library with 14 rules for Koin 4.x to enforce best practices and catch common anti-patterns via static analysis.

## Features

✅ **14 Rules** across 3 categories: Service Locator, Module DSL, Scope Management
✅ **Zero runtime overhead** — pure syntactic analysis via Kotlin PSI
✅ **No Koin dependency** in consumer projects
✅ **Configurable** — customize rules via detekt config
✅ **Production-ready** — comprehensive test coverage

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    detektPlugins("io.github.krozov:detekt-rules-koin:0.1.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    detektPlugins 'io.github.krozov:detekt-rules-koin:0.1.0'
}
```

## Usage

After adding the plugin, Detekt will automatically discover and apply the Koin rules.

Run analysis:

```bash
./gradlew detekt
```

## Rules

### Service Locator Rules (5)

| Rule | Severity | Default |
|------|----------|---------|
| `NoGetOutsideModuleDefinition` | Warning | Active |
| `NoInjectDelegate` | Warning | Active |
| `NoKoinComponentInterface` | Warning | Active |
| `NoGlobalContextAccess` | Warning | Active |
| `NoKoinGetInApplication` | Warning | Active |

### Module DSL Rules (5)

| Rule | Severity | Default |
|------|----------|---------|
| `EmptyModule` | Warning | Active |
| `SingleForNonSharedDependency` | Warning | Active |
| `MissingScopedDependencyQualifier` | Warning | Active |
| `DeprecatedKoinApi` | Warning | Active |
| `ModuleIncludesOrganization` | Style | Inactive |

### Scope Management Rules (4)

| Rule | Severity | Default |
|------|----------|---------|
| `MissingScopeClose` | Warning | Active |
| `ScopedDependencyOutsideScopeBlock` | Warning | Active |
| `FactoryInScopeBlock` | Style | Inactive |
| `KtorRequestScopeMisuse` | Warning | Active |

📖 **[Complete Rule Documentation](docs/rules.md)**

## Configuration

Create or update `.detekt.yml`:

```yaml
koin-rules:
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes:
      - 'Application'
      - 'Activity'
      - 'Fragment'

  SingleForNonSharedDependency:
    active: true
    namePatterns:
      - '.*UseCase'
      - '.*Command'

  ModuleIncludesOrganization:
    active: true
    maxIncludesWithDefinitions: 5
```

## Requirements

- Kotlin 2.0+
- Detekt 1.23.8+
- Targets Koin 4.x (all platforms: Core, Android, Compose, Ktor)

## License

```
Copyright 2026 Kirill Rozov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

Contributions are welcome! Please open an issue or pull request.

## Roadmap

- v0.2.0: Architecture rules (Koin imports in domain layer)
- v0.3.0: Koin Annotations support
- v1.0.0: API stabilization, Detekt 2.x compatibility
