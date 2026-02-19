# Configuration Guide

Comprehensive guide to configuring `detekt-rules-koin` for your project.

---

## Table of Contents

- [Basic Setup](#basic-setup)
- [Rule Configuration](#rule-configuration)
- [Advanced Configuration](#advanced-configuration)
- [Baseline Files](#baseline-files)
- [Exclude Patterns](#exclude-patterns)
- [Real-World Examples](#real-world-examples)

---

## Basic Setup

### Minimal Configuration

Add the plugin to your `build.gradle.kts`:

```kotlin
dependencies {
    detektPlugins("dev.androidbroadcast.rules.koin:detekt-koin4-rules:0.3.0")
}
```

Run with default settings:

```bash
./gradlew detekt
```

All rules run with their default configuration. No `.detekt.yml` file is required.

### Default `.detekt.yml`

Create `.detekt.yml` in your project root to customize rules:

```yaml
koin-rules:
  # All rules use default settings
  active: true
```

This is equivalent to no configuration file.

---

## Rule Configuration

### Service Locator Rules

#### NoKoinComponentInterface

Prevent KoinComponent usage in non-framework classes.

**Default:** Active

```yaml
koin-rules:
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes:
      - 'Application'
      - 'Activity'
      - 'Fragment'
      - 'Service'
      - 'BroadcastReceiver'
```

**Custom Example - Allow in ViewModels:**

```yaml
koin-rules:
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes:
      - 'Application'
      - 'Activity'
      - 'Fragment'
      - 'ViewModel'  # Allow KoinComponent in ViewModels
      - 'AndroidViewModel'
```

**Custom Example - Strict Mode (No Exceptions):**

```yaml
koin-rules:
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes: []  # Empty list - no exceptions allowed
```

#### NoInjectDelegate

Prevent `by inject()` property delegates.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  NoInjectDelegate:
    active: true
```

#### NoGetOutsideModuleDefinition

Prevent `get()` calls outside module definition blocks.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  NoGetOutsideModuleDefinition:
    active: true
```

#### NoGlobalContextAccess

Prevent direct access to `GlobalContext`.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  NoGlobalContextAccess:
    active: true
```

#### NoKoinGetInApplication

Prevent `get()` calls inside `startKoin` configuration.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  NoKoinGetInApplication:
    active: true
```

### Module DSL Rules

#### EmptyModule

Detect empty Koin modules.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  EmptyModule:
    active: true
```

#### SingleForNonSharedDependency

Detect `single {}` definitions for non-shared types.

**Default:** Active

```yaml
koin-rules:
  SingleForNonSharedDependency:
    active: true
    namePatterns:
      - '.*UseCase'
      - '.*Command'
      - '.*Handler'
      - '.*Mapper'
```

**Custom Example - Add Custom Patterns:**

```yaml
koin-rules:
  SingleForNonSharedDependency:
    active: true
    namePatterns:
      - '.*UseCase'
      - '.*Command'
      - '.*Handler'
      - '.*Interactor'
      - '.*Worker'
      - '.*Job'
```

#### MissingScopedDependencyQualifier

Detect scoped dependencies without qualifiers.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  MissingScopedDependencyQualifier:
    active: true
```

#### DeprecatedKoinApi

Detect deprecated Koin API usage.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  DeprecatedKoinApi:
    active: true
```

#### ModuleIncludesOrganization

Enforce module organization patterns.

**Default:** Inactive (style rule)

```yaml
koin-rules:
  ModuleIncludesOrganization:
    active: true
    maxIncludesWithDefinitions: 5
```

**Custom Example - Strict Organization:**

```yaml
koin-rules:
  ModuleIncludesOrganization:
    active: true
    maxIncludesWithDefinitions: 3  # Stricter limit
```

### Scope Management Rules

#### MissingScopeClose

Detect unclosed Koin scopes.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  MissingScopeClose:
    active: true
```

#### ScopedDependencyOutsideScopeBlock

Detect scoped dependencies defined outside `scope {}` blocks.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  ScopedDependencyOutsideScopeBlock:
    active: true
```

#### FactoryInScopeBlock

Detect `factory {}` inside `scope {}` blocks.

**Default:** Inactive (style rule)

```yaml
koin-rules:
  FactoryInScopeBlock:
    active: true
```

#### KtorRequestScopeMisuse

Detect misuse of Koin scopes in Ktor request handling.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  KtorRequestScopeMisuse:
    active: true
```

### Platform Rules

#### KoinViewModelOutsideComposable

Detect `koinViewModel()` calls outside Composable functions.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  KoinViewModelOutsideComposable:
    active: true
```

#### KoinInjectInPreview

Detect Koin injection in `@Preview` composables.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  KoinInjectInPreview:
    active: true
```

#### RememberKoinModulesLeak

Detect `rememberKoinModules()` usage that may leak.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  RememberKoinModulesLeak:
    active: true
```

#### KtorApplicationKoinInit

Enforce Koin initialization in Ktor applications.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  KtorApplicationKoinInit:
    active: true
```

#### KtorRouteScopeMisuse

Detect incorrect scope usage in Ktor routes.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  KtorRouteScopeMisuse:
    active: true
```

#### AndroidContextNotFromKoin

Detect Android Context obtained outside Koin.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  AndroidContextNotFromKoin:
    active: true
```

#### ActivityFragmentKoinScope

Detect incorrect scope usage in Android Activities/Fragments.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  ActivityFragmentKoinScope:
    active: true
```

### Architecture Rules

#### LayerBoundaryViolation

Enforce architectural layer boundaries (inactive by default).

**Default:** Inactive (requires configuration)

```yaml
koin-rules:
  LayerBoundaryViolation:
    active: true
    restrictedLayers:
      - 'domain'
      - 'core'
```

**Custom Example - Multi-Module Project:**

```yaml
koin-rules:
  LayerBoundaryViolation:
    active: true
    restrictedLayers:
      - 'domain'
      - 'core'
      - 'business'
      - 'usecase'
```

#### PlatformImportRestriction

Restrict platform-specific imports in certain modules.

**Default:** Inactive (requires configuration)

```yaml
koin-rules:
  PlatformImportRestriction:
    active: true
    platformPackages:
      - 'android'
      - 'androidx'
      - 'com.google.android'
```

**Custom Example - Include iOS Platforms:**

```yaml
koin-rules:
  PlatformImportRestriction:
    active: true
    platformPackages:
      - 'android'
      - 'androidx'
      - 'com.google.android'
      - 'platform.UIKit'
      - 'platform.Foundation'
```

#### CircularModuleDependency

Detect circular dependencies between Koin modules.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  CircularModuleDependency:
    active: true
```

### Koin Annotations Rules

#### MixingDslAndAnnotations

Detect mixing DSL and annotation-based module definitions.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  MixingDslAndAnnotations:
    active: true
```

#### MissingModuleAnnotation

Detect classes that should have `@Module` annotation.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  MissingModuleAnnotation:
    active: true
```

#### ConflictingBindings

Detect conflicting Koin bindings.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  ConflictingBindings:
    active: true
```

#### ScopedWithoutQualifier

Detect scoped definitions without qualifiers.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  ScopedWithoutQualifier:
    active: true
```

#### AnnotationProcessorNotConfigured

Detect missing Koin annotation processor configuration.

**Default:** Active, no configuration options.

```yaml
koin-rules:
  AnnotationProcessorNotConfigured:
    active: true
```

---

## Advanced Configuration

### Combining with Other Detekt Rules

```yaml
build:
  maxIssues: 0  # Fail build on any issue
  excludeCorrectable: false

koin-rules:
  active: true
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes:
      - 'Application'
      - 'Activity'

complexity:
  LongMethod:
    active: true
    threshold: 60
```

### Multi-Module Configuration

For multi-module projects, place `.detekt.yml` in the root and configure per-module exclusions:

```yaml
koin-rules:
  LayerBoundaryViolation:
    active: true
    restrictedLayers:
      - 'domain'
      - 'core'

  PlatformImportRestriction:
    active: true
    platformPackages:
      - 'android'
      - 'androidx'

# Apply different rules to different modules via exclusions
console-reports:
  active: true
  exclude:
    - '**/test/**'
    - '**/androidTest/**'
```

### Custom Severity Levels

Override rule severity levels:

```yaml
koin-rules:
  NoKoinComponentInterface:
    active: true
    severity: error  # Fail build on this violation

  SingleForNonSharedDependency:
    active: true
    severity: warning  # Just warn, don't fail

  ModuleIncludesOrganization:
    active: true
    severity: info  # Informational only
```

---

## Baseline Files

Detekt supports baseline files to suppress existing violations while preventing new ones.

### Creating a Baseline

Generate a baseline file for existing violations:

```bash
./gradlew detektBaseline
```

This creates `detekt-baseline.xml` with all current violations.

### Using a Baseline

Configure baseline in `.detekt.yml`:

```yaml
detekt:
  baseline: detekt-baseline.xml
```

Or specify on command line:

```bash
./gradlew detekt --baseline detekt-baseline.xml
```

### Baseline File Structure

Example `detekt-baseline.xml`:

```xml
<?xml version="1.0" ?>
<SmellBaseline>
  <ManuallySuppressedIssues></ManuallySuppressedIssues>
  <CurrentIssues>
    <ID>NoKoinComponentInterface:MyLegacyClass.kt$MyLegacyClass</ID>
    <ID>EmptyModule:AppModule.kt$emptyTestModule</ID>
  </CurrentIssues>
</SmellBaseline>
```

### Baseline Workflow

**Recommended workflow for existing projects:**

1. Add `detekt-rules-koin` to your project
2. Generate baseline: `./gradlew detektBaseline`
3. Configure baseline in `.detekt.yml`
4. Fix violations incrementally
5. Regenerate baseline as violations are fixed
6. Remove baseline when all violations are resolved

**Example - Incremental Cleanup:**

```yaml
# Week 1: Create baseline
detekt:
  baseline: detekt-baseline.xml

# Week 2-4: Fix critical violations, regenerate baseline
# ./gradlew detektBaseline

# Week 5: Remove baseline when clean
detekt:
  # baseline: detekt-baseline.xml  # Commented out - no violations!
```

---

## Exclude Patterns

### Excluding Files

Exclude specific files or directories:

```yaml
build:
  excludes:
    - '**/build/**'
    - '**/generated/**'
    - '**/resources/**'
    - '**/test/**'
```

### Excluding Test Code

```yaml
koin-rules:
  active: true

test-pattern:
  active: true
  exclude:
    - '**/test/**'
    - '**/androidTest/**'
    - '**/*Test.kt'
    - '**/*Spec.kt'
```

### Per-Rule Exclusions

Exclude specific patterns for individual rules:

```yaml
koin-rules:
  NoKoinComponentInterface:
    active: true
    excludes:
      - '**/legacy/**'  # Legacy code - will fix later
      - '**/external/**'
    allowedSuperTypes:
      - 'Application'
```

### Excluding Generated Code

```yaml
build:
  excludes:
    - '**/build/generated/**'
    - '**/*.Generated.kt'
    - '**/Dagger*.kt'
    - '**/Hilt*.kt'
```

---

## Real-World Examples

### Example 1: Android Application (Clean Architecture)

**Project Structure:**
```
app/
  domain/       # Pure Kotlin, no Android/Koin
  data/         # Repository implementations, uses Koin
  presentation/ # ViewModels, uses Koin
```

**Configuration:**

```yaml
koin-rules:
  active: true

  # Service Locator
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes:
      - 'Application'
      - 'Activity'
      - 'Fragment'
      - 'ViewModel'

  NoInjectDelegate:
    active: true

  NoGetOutsideModuleDefinition:
    active: true

  NoGlobalContextAccess:
    active: true

  # Module DSL
  EmptyModule:
    active: true

  SingleForNonSharedDependency:
    active: true
    namePatterns:
      - '.*UseCase'
      - '.*Interactor'
      - '.*Command'

  ModuleIncludesOrganization:
    active: true
    maxIncludesWithDefinitions: 5

  # Architecture
  LayerBoundaryViolation:
    active: true
    restrictedLayers:
      - 'domain'

  PlatformImportRestriction:
    active: true
    platformPackages:
      - 'android'
      - 'androidx'
      - 'com.google.android'

  # Platform
  KoinInjectInPreview:
    active: true

  AndroidContextNotFromKoin:
    active: true

build:
  excludes:
    - '**/build/**'
    - '**/test/**'
    - '**/androidTest/**'
```

### Example 2: Kotlin Multiplatform (KMP) Project

**Project Structure:**
```
common/
  core/       # Shared business logic
  platform/   # Platform-specific code
androidApp/
iosApp/
```

**Configuration:**

```yaml
koin-rules:
  active: true

  # Strict service locator rules for shared code
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes: []  # No exceptions in common code

  NoInjectDelegate:
    active: true

  # Architecture
  LayerBoundaryViolation:
    active: true
    restrictedLayers:
      - 'core'
      - 'domain'

  PlatformImportRestriction:
    active: true
    platformPackages:
      - 'android'
      - 'androidx'
      - 'platform.UIKit'
      - 'platform.Foundation'

  # Module organization
  CircularModuleDependency:
    active: true

  ModuleIncludesOrganization:
    active: true
    maxIncludesWithDefinitions: 3

build:
  excludes:
    - '**/build/**'
    - '**/generated/**'
```

### Example 3: Ktor Backend Service

**Project Structure:**
```
src/
  main/kotlin/
    routes/      # HTTP routes
    services/    # Business logic
    repositories/
```

**Configuration:**

```yaml
koin-rules:
  active: true

  # Service Locator
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes: []  # Pure constructor injection

  NoInjectDelegate:
    active: true

  NoGlobalContextAccess:
    active: true

  # Module DSL
  SingleForNonSharedDependency:
    active: true
    namePatterns:
      - '.*Handler'
      - '.*Command'
      - '.*Processor'

  # Ktor-specific
  KtorApplicationKoinInit:
    active: true

  KtorRouteScopeMisuse:
    active: true

  KtorRequestScopeMisuse:
    active: true

  # Scope Management
  MissingScopeClose:
    active: true

  ScopedDependencyOutsideScopeBlock:
    active: true

build:
  excludes:
    - '**/build/**'
    - '**/test/**'
    - '**/resources/**'
```

### Example 4: Jetpack Compose Application

**Project Structure:**
```
app/
  ui/           # Composables
  viewmodels/   # ViewModels
  data/         # Repositories
```

**Configuration:**

```yaml
koin-rules:
  active: true

  # Service Locator
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes:
      - 'Application'
      - 'ViewModel'
      - 'AndroidViewModel'

  # Compose-specific
  KoinViewModelOutsideComposable:
    active: true

  KoinInjectInPreview:
    active: true

  RememberKoinModulesLeak:
    active: true

  # Module DSL
  EmptyModule:
    active: true

  SingleForNonSharedDependency:
    active: true
    namePatterns:
      - '.*UseCase'
      - '.*Mapper'

  ModuleIncludesOrganization:
    active: true
    maxIncludesWithDefinitions: 5

build:
  excludes:
    - '**/build/**'
    - '**/test/**'
    - '**/*Preview.kt'
```

### Example 5: Legacy Project Migration

**Scenario:** Existing project with many Koin violations, gradual cleanup.

**Phase 1 - Create Baseline:**

```bash
./gradlew detektBaseline
```

**Phase 2 - Enable Critical Rules Only:**

```yaml
koin-rules:
  active: true

  # Critical issues only
  NoGlobalContextAccess:
    active: true

  CircularModuleDependency:
    active: true

  MissingScopeClose:
    active: true

  # Everything else inactive during migration
  NoKoinComponentInterface:
    active: false  # Fix later

  NoInjectDelegate:
    active: false  # Fix later

detekt:
  baseline: detekt-baseline.xml
```

**Phase 3 - Incremental Enablement:**

```yaml
koin-rules:
  active: true

  # Week 1: Enable critical rules
  NoGlobalContextAccess:
    active: true
  CircularModuleDependency:
    active: true

  # Week 2: Enable service locator rules
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes:
      - 'Application'
      - 'Activity'
      - 'Fragment'
      - 'ViewModel'  # Temporary exception

  # Week 3: Enable all rules
  NoInjectDelegate:
    active: true

detekt:
  baseline: detekt-baseline.xml  # Regenerate weekly
```

**Phase 4 - Full Enforcement:**

```yaml
koin-rules:
  active: true  # All rules active with strict config

detekt:
  # baseline removed - all violations fixed!

build:
  maxIssues: 0  # Fail build on any violation
```

---

## Tips & Best Practices

### Start Small

1. Add plugin to one module first
2. Generate baseline
3. Fix new violations only
4. Expand to other modules

### Use CI/CD Integration

```yaml
# GitHub Actions example
- name: Run Detekt
  run: ./gradlew detekt --no-daemon

- name: Upload SARIF
  uses: github/codeql-action/upload-sarif@v2
  with:
    sarif_file: build/reports/detekt/detekt.sarif
```

### Regular Baseline Updates

```bash
# Weekly: Fix violations and update baseline
./gradlew detekt          # Check current violations
# Fix violations
./gradlew detektBaseline  # Update baseline
```

### Monitor Coverage

```bash
# Ensure rules are actually running
./gradlew detekt --debug | grep "koin-rules"
```

### Custom Reports

Configure HTML reports for easier review:

```yaml
output-reports:
  html:
    enabled: true
    destination: build/reports/detekt.html
  xml:
    enabled: true
    destination: build/reports/detekt.xml
```

---

## Troubleshooting

### Rules Not Running

**Problem:** No Koin violations detected despite obvious issues.

**Solution:**
1. Verify plugin is installed: `./gradlew dependencies | grep detekt-rules-koin`
2. Check `koin-rules:` section exists in `.detekt.yml`
3. Ensure `active: true` is set
4. Run with `--debug` flag to see rule execution

### Too Many False Positives

**Problem:** Rule flags valid code as violations.

**Solutions:**
1. Use `excludes:` to exclude specific files/patterns
2. Configure rule-specific options (e.g., `allowedSuperTypes`)
3. Report false positives as GitHub issues
4. Use baseline temporarily

### Performance Issues

**Problem:** Detekt analysis is very slow.

**Solutions:**
1. Exclude test code: `excludes: ['**/test/**']`
2. Exclude generated code: `excludes: ['**/build/generated/**']`
3. Use parallel execution: `./gradlew detekt --parallel`
4. Disable unused rule sets in `.detekt.yml`

### Configuration Not Applied

**Problem:** Configuration changes don't take effect.

**Solutions:**
1. Verify `.detekt.yml` is in project root
2. Use exact rule names (case-sensitive)
3. Clear Gradle cache: `./gradlew clean`
4. Specify config explicitly: `./gradlew detekt --config .detekt.yml`

---

## Further Reading

- [Main Documentation](../README.md)
- [Complete Rules Reference](rules.md)
- [Detekt Configuration](https://detekt.dev/docs/introduction/configurations/)
- [Koin Documentation](https://insert-koin.io/)
