# detekt-rules-koin

[![Latest Release](https://img.shields.io/github/v/release/androidbroadcast/Koin-Detekt)](https://github.com/androidbroadcast/Koin-Detekt/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/dev.androidbroadcast.rules.koin/detekt-koin-rules)](https://central.sonatype.com/artifact/dev.androidbroadcast.rules.koin/detekt-koin-rules)
[![Release Date](https://img.shields.io/github/release-date/androidbroadcast/Koin-Detekt)](https://github.com/androidbroadcast/Koin-Detekt/releases)
[![PR Validation](https://github.com/androidbroadcast/Koin-Detekt/actions/workflows/pr-validation.yml/badge.svg)](https://github.com/androidbroadcast/Koin-Detekt/actions/workflows/pr-validation.yml)
![Coverage](https://img.shields.io/badge/coverage-96%25-brightgreen.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Detekt 1.x extension library with 35 rules for Koin 4.x to enforce best practices and catch common anti-patterns via static analysis.

## Features

✅ **35 Rules** across 6 categories: Service Locator, Module DSL, Scope Management, Platform, Architecture, Koin Annotations
✅ **Zero runtime overhead** — pure syntactic analysis via Kotlin PSI
✅ **No Koin dependency** in consumer projects
✅ **Configurable** — customize rules via detekt config
✅ **Production-ready** — comprehensive test coverage

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    detektPlugins("dev.androidbroadcast.rules.koin:detekt-koin-rules:0.4.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    detektPlugins 'dev.androidbroadcast.rules.koin:detekt-koin-rules:0.4.0'
}
```

### Availability

This library is published to:
- **Maven Central** - primary distribution (recommended)
- **GitHub Packages** - alternative source

For Maven Central, no additional repository configuration is needed - it's available by default in all Gradle projects.

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

### Module DSL Rules (9)

| Rule | Severity | Default |
|------|----------|---------|
| `EmptyModule` | Warning | Active |
| `SingleForNonSharedDependency` | Warning | Active |
| `MissingScopedDependencyQualifier` | Warning | Active |
| `DeprecatedKoinApi` | Warning | Active |
| `ModuleIncludesOrganization` | Style | Inactive |
| `UnassignedQualifierInWithOptions` | Warning | Active |
| `DuplicateBindingWithoutQualifier` | Warning | Active |
| `GenericDefinitionWithoutQualifier` | Warning | Active |
| `EnumQualifierCollision` | Warning | Active |

### Scope Management Rules (4)

| Rule | Severity | Default |
|------|----------|---------|
| `MissingScopeClose` | Warning | Active |
| `ScopedDependencyOutsideScopeBlock` | Warning | Active |
| `FactoryInScopeBlock` | Style | Inactive |
| `KtorRequestScopeMisuse` | Warning | Active |

### Platform Rules (7)

| Rule | Severity | Default |
|------|----------|---------|
| `KoinViewModelOutsideComposable` | Warning | Active |
| `KoinInjectInPreview` | Warning | Active |
| `RememberKoinModulesLeak` | Warning | Active |
| `KtorApplicationKoinInit` | Warning | Active |
| `KtorRouteScopeMisuse` | Warning | Active |
| `AndroidContextNotFromKoin` | Warning | Active |
| `ActivityFragmentKoinScope` | Warning | Active |

### Architecture Rules (3)

| Rule | Severity | Default |
|------|----------|---------|
| `LayerBoundaryViolation` | Warning | Inactive |
| `PlatformImportRestriction` | Warning | Inactive |
| `CircularModuleDependency` | Warning | Active |

### Koin Annotations Rules (5)

| Rule | Severity | Default |
|------|----------|---------|
| `MixingDslAndAnnotations` | Warning | Active |
| `MissingModuleAnnotation` | Warning | Active |
| `ConflictingBindings` | Warning | Active |
| `ScopedWithoutQualifier` | Warning | Active |
| `AnnotationProcessorNotConfigured` | Warning | Active |

📖 **[Complete Rule Documentation](docs/rules.md)**

## Configuration

### Quick Start

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

📖 **[Comprehensive Configuration Guide](docs/configuration.md)** - Includes:
- Advanced per-rule configuration
- Baseline file usage for existing projects
- Exclude patterns and multi-module setup
- Real-world examples (Android, KMP, Ktor, Compose)
- Migration strategies for legacy codebases

## Example Output

When Detekt finds violations, you'll see clear messages:

```bash
src/main/kotlin/com/example/MyRepository.kt:5:1: [koin-rules] NoKoinComponentInterface
  Class 'MyRepository' implements KoinComponent but is not a framework entry point.
  Use constructor injection instead.

src/main/kotlin/com/example/di/AppModule.kt:10:5: [koin-rules] EmptyModule
  Module is empty. Remove it or add definitions/includes().

src/main/kotlin/com/example/MyService.kt:12:9: [koin-rules] NoGetOutsideModuleDefinition
  Found get() call outside module definition. Use constructor injection or define inside module { }.
```

Run analysis:

```bash
./gradlew detekt
```

View results in `build/reports/detekt/detekt.html`.

## Code Coverage

This project uses [Kover](https://github.com/Kotlin/kotlinx-kover) for code coverage tracking.

### Generate Coverage Reports

```bash
./gradlew koverHtmlReport
```

View the HTML report at `build/reports/kover/html/index.html`.

### Verify Coverage

Coverage verification runs automatically with:

```bash
./gradlew check
```

This enforces minimum coverage thresholds:
- **Line coverage**: 98%
- **Branch coverage**: 70%

> **Note:** The coverage badge in this README is updated manually. After significant coverage changes, update the badge percentage to reflect the latest `koverHtmlReport` results.

### Coverage Rules

- Test code is excluded from coverage
- Generated code and providers are excluded
- All public APIs must have tests
- All rule implementations must have tests
- 220+ unit tests covering all 29 rules
- 3 integration tests validating end-to-end functionality

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
