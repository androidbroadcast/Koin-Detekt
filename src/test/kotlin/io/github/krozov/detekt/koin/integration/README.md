# Integration Tests

This directory contains comprehensive integration tests for detekt-koin-rules.

## Test Organization

### `samples/`
Tests verifying rules work correctly on real Android, Compose, and Ktor sample code.

- **AndroidSampleIntegrationTest.kt** - Android-specific violations
- **ComposeSampleIntegrationTest.kt** - Jetpack Compose violations
- **KtorSampleIntegrationTest.kt** - Ktor framework violations

### `configuration/`
Tests for YAML configuration and rule parameter overrides.

- **YamlConfigurationIntegrationTest.kt** - Config file loading and application
- **RuleConfigurationOverrideTest.kt** - Parameter overrides and validation

### `interaction/`
Tests for rule interactions, multiple violations, and suppression.

- **MultipleViolationsIntegrationTest.kt** - Multiple violations in single file
- **RulePriorityIntegrationTest.kt** - Rule priority and conflict resolution
- **SuppressionIntegrationTest.kt** - @Suppress annotation behavior

### `performance/`
Performance tests for large files and multi-file analysis.

- **LargeFileAnalysisIntegrationTest.kt** - Files with 1000-5000 lines
- **MultiFileAnalysisIntegrationTest.kt** - Analyzing 10-50 files

### `compatibility/`
Tests for compatibility with different Koin and Kotlin versions.

- **KoinVersionCompatibilityTest.kt** - Koin 3.x vs 4.x API differences
- **KotlinVersionCompatibilityTest.kt** - Modern Kotlin features

### `edgecases/`
Tests for complex type resolution and platform-specific scenarios.

- **ComplexTypeResolutionIntegrationTest.kt** - Nested generics, type aliases, etc.
- **PlatformSpecificIntegrationTest.kt** - Android, Ktor, Compose APIs

## Running Tests

```bash
# Run all integration tests
./gradlew test --tests "*IntegrationTest*"

# Run specific category
./gradlew test --tests "*integration.samples.*"
./gradlew test --tests "*integration.configuration.*"

# Run with coverage report
./gradlew test koverHtmlReport
open build/reports/kover/html/index.html
```

## Test Coverage Goals

| Metric | Target |
|--------|--------|
| Line Coverage | 98% |
| Branch Coverage | 90% |

## Contributing

When adding new rules, add corresponding integration tests in the appropriate subdirectory:

1. **Platform-specific rules** → `samples/`
2. **Configurable rules** → `configuration/`
3. **Rules that interact** → `interaction/`
4. **Performance-sensitive rules** → `performance/`
5. **Version-specific rules** → `compatibility/`
6. **Complex type handling** → `edgecases/`

See [implementation plan](../../../docs/plans/2026-02-17-integration-tests-implementation.md) for details.
