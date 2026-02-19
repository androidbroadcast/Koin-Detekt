# detekt-rules-koin

[![Latest Release](https://img.shields.io/github/v/release/androidbroadcast/Koin-Detekt)](https://github.com/androidbroadcast/Koin-Detekt/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/dev.androidbroadcast.rules.koin/detekt-koin-rules)](https://central.sonatype.com/artifact/dev.androidbroadcast.rules.koin/detekt-koin-rules)
[![PR Validation](https://github.com/androidbroadcast/Koin-Detekt/actions/workflows/pr-validation.yml/badge.svg)](https://github.com/androidbroadcast/Koin-Detekt/actions/workflows/pr-validation.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Detekt extension with 51 rules for Koin 4.x — enforces best practices and catches common anti-patterns via static analysis.

## Installation

```kotlin
dependencies {
    detektPlugins("dev.androidbroadcast.rules.koin:detekt-koin-rules:0.4.0")
}
```

## Rules

### Service Locator (5)

| Rule | Default |
|------|---------|
| `NoGetOutsideModuleDefinition` | Active |
| `NoInjectDelegate` | Active |
| `NoKoinComponentInterface` | Active |
| `NoGlobalContextAccess` | Active |
| `NoKoinGetInApplication` | Active |

### Module DSL (9)

| Rule | Default |
|------|---------|
| `EmptyModule` | Active |
| `SingleForNonSharedDependency` | Active |
| `MissingScopedDependencyQualifier` | Active |
| `DeprecatedKoinApi` | Active |
| `ModuleIncludesOrganization` | Inactive |
| `UnassignedQualifierInWithOptions` | Active |
| `DuplicateBindingWithoutQualifier` | Active |
| `GenericDefinitionWithoutQualifier` | Active |
| `EnumQualifierCollision` | Active |

### Scope Management (7)

| Rule | Default |
|------|---------|
| `MissingScopeClose` | Active |
| `ScopedDependencyOutsideScopeBlock` | Active |
| `ViewModelAsSingleton` | Active |
| `CloseableWithoutOnClose` | Active |
| `ScopeAccessInOnDestroy` | Active |
| `FactoryInScopeBlock` | Inactive |
| `KtorRequestScopeMisuse` | Active |

### Platform (7)

| Rule | Default |
|------|---------|
| `KoinViewModelOutsideComposable` | Active |
| `KoinInjectInPreview` | Active |
| `RememberKoinModulesLeak` | Active |
| `KtorApplicationKoinInit` | Active |
| `KtorRouteScopeMisuse` | Active |
| `AndroidContextNotFromKoin` | Active |
| `ActivityFragmentKoinScope` | Active |

### Architecture (3)

| Rule | Default |
|------|---------|
| `LayerBoundaryViolation` | Inactive |
| `PlatformImportRestriction` | Inactive |
| `CircularModuleDependency` | Active |

### Koin Annotations (12)

| Rule | Default |
|------|---------|
| `MixingDslAndAnnotations` | Active |
| `MissingModuleAnnotation` | Active |
| `ConflictingBindings` | Active |
| `ScopedWithoutQualifier` | Active |
| `AnnotationProcessorNotConfigured` | Active |
| `SingleAnnotationOnObject` | Active |
| `TooManyInjectedParams` | Active |
| `InvalidNamedQualifierCharacters` | Active |
| `KoinAnnotationOnExtensionFunction` | Active |
| `ViewModelAnnotatedAsSingle` | Active |
| `AnnotatedClassImplementsNestedInterface` | Active |
| `InjectedParamWithNestedGenericType` | Active |

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
