# Detekt-Koin v0.2-v0.4 Expansion Design

**Date:** 2026-02-13
**Author:** Claude Sonnet 4.5
**Status:** Approved
**Target Versions:** v0.2.0, v0.3.0, v0.4.0

---

## Executive Summary

Expand detekt-rules-koin from 14 to 30+ rules across 3 phased releases:
- **v0.2.0:** Platform-specific (Compose/Ktor/Android) + Architecture boundary rules
- **v0.3.0:** Koin Annotations support + Developer Experience foundations
- **v0.4.0:** Auto-fixes + Advanced configuration

**Timeline:** 6-8 weeks total (2-3 weeks per version)
**Test Coverage:** Maintain 98% line / 70% branch
**Approach:** Phased releases with incremental value

---

## Goals & Non-Goals

### Goals
- ✅ Platform-specific rules for Compose, Ktor, Android
- ✅ Architecture boundary validation (Clean Architecture support)
- ✅ Full Koin Annotations (`@Single`, `@Factory`, etc.) support
- ✅ Auto-fixes for top 10 most common violations
- ✅ Enhanced developer experience (suppression, better messages, config)
- ✅ Maintain 98%/70% coverage and zero regressions

### Non-Goals
- ❌ Performance benchmarking (defer to future version)
- ❌ Mutation testing (defer to future version)
- ❌ Property-based testing (nice-to-have, not required)
- ❌ IDE plugin integration (beyond standard Detekt support)
- ❌ Koin 3.x specific features (focus on Koin 4.x)

---

## Architecture Overview

### v0.2.0: Platform & Architecture Rules

**New Package Structure:**
```
src/main/kotlin/io/github/krozov/detekt/koin/
├── moduledsl/         # Existing (5 rules)
├── scope/             # Existing (4 rules)
├── servicelocator/    # Existing (5 rules)
├── platform/          # NEW: Platform-specific rules
│   ├── compose/       # Compose rules (3-4 rules)
│   ├── ktor/          # Ktor rules (2-3 rules)
│   └── android/       # Android rules (2-3 rules)
└── architecture/      # NEW: Architecture boundary rules
    ├── LayerBoundaryViolation.kt
    ├── PlatformImportRestriction.kt
    └── CircularModuleDependency.kt
```

**Integration:**
- Update `KoinRuleSetProvider` to include new rules
- Total rules: 14 → ~25 rules
- Backward compatible: all new rules configurable, some inactive by default

---

### v0.3.0: Koin Annotations + DX Foundations

**New Package Structure:**
```
src/main/kotlin/io/github/krozov/detekt/koin/
├── annotations/       # NEW: Annotation-based Koin support
│   ├── MixingDslAndAnnotations.kt
│   ├── MissingModuleAnnotation.kt
│   ├── ConflictingBindings.kt
│   ├── ScopedWithoutQualifier.kt
│   └── AnnotationProcessorNotConfigured.kt
└── fixes/             # NEW: Auto-fix infrastructure (base only)
    └── KoinQuickFix.kt
```

**Enhancements to All Rules:**
- Suppression support: `@Suppress("RuleName")`
- Enhanced messages with examples
- Improved configuration options

---

### v0.4.0: Advanced DX

**Expansions:**
```
src/main/kotlin/io/github/krozov/detekt/koin/
└── fixes/
    ├── KoinQuickFix.kt              # Base class
    ├── NoKoinComponentFix.kt         # Auto-fix implementation
    ├── NoInjectDelegateFix.kt
    ├── EmptyModuleFix.kt
    ├── DeprecatedKoinApiFix.kt
    └── ... (10+ total fixes)
```

**Features:**
- Auto-fixes for 10+ common violations
- Baseline file support (`.detekt-baseline.xml`)
- Enhanced exclude patterns
- Advanced configuration per rule

---

## Components (Detailed Rules)

### v0.2.0 Components (10 new rules)

#### Platform Rules: Compose (3-4 rules)

**1. KoinViewModelOutsideComposable**
- **Severity:** Warning
- **Active:** Yes
- **Description:** Detects `koinViewModel()` calls outside `@Composable` functions
- **Rationale:** `koinViewModel()` requires Composition context, crashes at runtime if called outside
- **Detection:** Check if containing function has `@Composable` annotation
- **Example:**
  ```kotlin
  // ❌ Bad
  fun MyScreen() {
      val vm = koinViewModel<MyVM>() // Runtime crash!
  }

  // ✅ Good
  @Composable
  fun MyScreen() {
      val vm = koinViewModel<MyVM>()
  }
  ```

**2. KoinInjectInPreview**
- **Severity:** Warning
- **Active:** Yes
- **Description:** Detects `koinInject()` in `@Preview` functions
- **Rationale:** Previews run without Koin context, causes crashes in Preview rendering
- **Detection:** Check if function has `@Preview` annotation and contains `koinInject()`
- **Example:**
  ```kotlin
  // ❌ Bad
  @Preview
  @Composable
  fun MyScreenPreview() {
      val repo = koinInject<Repository>() // Preview crash!
      MyScreen(repo)
  }

  // ✅ Good
  @Preview
  @Composable
  fun MyScreenPreview() {
      MyScreen(FakeRepository()) // Use fake/mock
  }
  ```

**3. RememberKoinModulesLeak**
- **Severity:** Warning
- **Active:** Yes
- **Description:** Detects `remember { loadKoinModules() }` without corresponding unload
- **Rationale:** Memory leak - modules loaded on every recomposition without cleanup
- **Detection:** Find `loadKoinModules` inside `remember` without `DisposableEffect` + `unloadKoinModules`
- **Example:**
  ```kotlin
  // ❌ Bad
  @Composable
  fun FeatureScreen() {
      remember { loadKoinModules(featureModule) } // Leak!
  }

  // ✅ Good
  @Composable
  fun FeatureScreen() {
      DisposableEffect(Unit) {
          loadKoinModules(featureModule)
          onDispose { unloadKoinModules(featureModule) }
      }
  }
  ```

**4. ComposeKoinScopeNotClosed** (Optional, inactive by default)
- **Severity:** Warning
- **Active:** No
- **Description:** Detects `rememberKoinScope()` without `CloseScopeOnDispose`
- **Rationale:** Scope leak if not properly closed
- **Configurable:** Strict mode for teams that want extra safety

---

#### Platform Rules: Ktor (2-3 rules)

**1. KtorApplicationKoinInit**
- **Severity:** Warning
- **Active:** Yes
- **Description:** Detects `install(Koin)` in routes or middleware instead of Application.module
- **Rationale:** Koin should be initialized once at application level, not per-route
- **Detection:** Check if `install(Koin)` is inside `routing {}` or route handlers
- **Example:**
  ```kotlin
  // ❌ Bad
  routing {
      install(Koin) { ... } // Wrong place!
      get("/api") { ... }
  }

  // ✅ Good
  fun Application.module() {
      install(Koin) { ... }
      routing { get("/api") { ... } }
  }
  ```

**2. KtorRouteScopeMisuse**
- **Severity:** Warning
- **Active:** Yes
- **Description:** Detects incorrect usage of `koinScope()` in route handlers
- **Rationale:** Each request should have isolated scope, misuse causes state leaks
- **Note:** Extends existing `KtorRequestScopeMisuse` rule
- **Example:**
  ```kotlin
  // ❌ Bad
  val sharedScope = koinScope() // Shared across requests!
  get("/api") {
      val service = sharedScope.get<Service>()
  }

  // ✅ Good
  get("/api") {
      call.koinScope().get<Service>() // Request-scoped
  }
  ```

**3. KtorDIInRoute** (Optional, style rule)
- **Severity:** Style
- **Active:** No
- **Description:** Recommends extracting DI from route handlers for better organization
- **Rationale:** Code organization, easier testing
- **Configurable:** For teams that prefer clean route handlers

---

#### Platform Rules: Android (2-3 rules)

**1. AndroidContextNotFromKoin**
- **Severity:** Warning
- **Active:** Yes
- **Description:** Detects `androidContext()` / `androidApplication()` called outside `startKoin`
- **Rationale:** These should only be set once at app initialization
- **Detection:** Check if call is inside `Application.onCreate` or `startKoin {}` block
- **Example:**
  ```kotlin
  // ❌ Bad
  class MyModule {
      val module = module {
          androidContext() // Wrong context!
      }
  }

  // ✅ Good
  class MyApp : Application() {
      override fun onCreate() {
          startKoin {
              androidContext(this@MyApp)
              modules(appModule)
          }
      }
  }
  ```

**2. ActivityFragmentKoinScope**
- **Severity:** Warning
- **Active:** Yes
- **Description:** Detects misuse of `activityScope()` / `fragmentScope()`
- **Rationale:** Scopes must match lifecycle, wrong scope causes leaks or crashes
- **Example:**
  ```kotlin
  // ❌ Bad
  class MyFragment : Fragment() {
      val vm by activityScope().inject<VM>() // Should be fragmentScope!
  }

  // ✅ Good
  class MyFragment : Fragment() {
      val vm by fragmentScope().inject<VM>()
  }
  ```

**3. WorkManagerKoinIntegration** (Optional, style rule)
- **Severity:** Style
- **Active:** No
- **Description:** Best practices for Koin + WorkManager integration
- **Rationale:** Common pitfall - workers need special DI setup

---

#### Architecture Rules (3 rules)

**1. LayerBoundaryViolation**
- **Severity:** Warning
- **Active:** No (opt-in via config)
- **Description:** Enforces Clean Architecture by restricting Koin imports in specified layers
- **Configurable:** Yes
- **Configuration:**
  ```yaml
  LayerBoundaryViolation:
    active: true
    restrictedLayers:
      - 'com.example.domain'      # Domain layer - pure Kotlin
      - 'com.example.core'        # Core layer - no DI
    allowedImports:
      - 'org.koin.core.qualifier.Qualifier'  # Only types, not functions
  ```
- **Detection:**
  - Check package of current file against `restrictedLayers`
  - If match, check all imports against allowed/disallowed list
  - Report violation if Koin function imports found
- **Edge Cases:**
  - Star imports (`import org.koin.core.*`) - treated as violation
  - Typealiases - resolve target type and check
  - Companion object imports - check package path
- **Example:**
  ```kotlin
  // ❌ Bad - domain layer with Koin
  package com.example.domain
  import org.koin.core.component.get

  class UseCase {
      val repo = get<Repository>() // Violates Clean Architecture!
  }

  // ✅ Good - domain layer with constructor injection
  package com.example.domain

  class UseCase(
      private val repo: Repository // DI through interface
  )
  ```

**2. PlatformImportRestriction**
- **Severity:** Warning
- **Active:** No (opt-in)
- **Description:** Restricts platform-specific Koin imports to appropriate modules
- **Configurable:** Yes
- **Configuration:**
  ```yaml
  PlatformImportRestriction:
    active: true
    restrictions:
      - import: 'org.koin.android.*'
        allowedPackages: ['com.example.app', 'com.example.presentation']
      - import: 'org.koin.compose.*'
        allowedPackages: ['com.example.ui']
      - import: 'org.koin.ktor.*'
        allowedPackages: ['com.example.server']
  ```
- **Rationale:** Prevents accidental platform dependencies in shared code
- **Example:**
  ```kotlin
  // ❌ Bad - Android-specific in shared module
  package com.example.shared
  import org.koin.android.ext.koin.androidContext

  // ✅ Good - Platform-specific only in platform modules
  package com.example.app.android
  import org.koin.android.ext.koin.androidContext
  ```

**3. CircularModuleDependency**
- **Severity:** Warning
- **Active:** Yes
- **Description:** Detects circular dependencies between Koin modules via `includes()`
- **Detection Strategy:**
  - Build dependency graph from `includes()` calls
  - Find cycles using DFS
  - Report all modules in cycle
- **Limitations:**
  - Works within single file and between files if module names follow convention
  - Cannot detect runtime circular dependencies (requires full project context)
- **Example:**
  ```kotlin
  // ❌ Bad - circular dependency
  val moduleA = module {
      includes(moduleB)
  }

  val moduleB = module {
      includes(moduleA) // Circular!
  }

  // ✅ Good - hierarchical
  val coreModule = module { ... }
  val featureModule = module {
      includes(coreModule)
  }
  ```

---

### v0.3.0 Components (5 new rules)

#### Koin Annotations Rules

**1. MixingDslAndAnnotations**
- **Severity:** Warning
- **Active:** Yes
- **Description:** Detects mixing DSL (`module {}`) and Annotations (`@Module`, `@Single`) in same project
- **Rationale:** Inconsistent approach, harder to maintain
- **Detection:** Check if file contains both `module {` and `@Module` / `@Single` annotations
- **Limitation:** Per-file detection (full project analysis requires custom runner)
- **Example:**
  ```kotlin
  // ❌ Bad - mixing approaches in same file
  @Module
  class AnnotatedModule {
      @Single
      fun provideRepo(): Repository = ...
  }

  val dslModule = module {
      single { ApiService() }
  }

  // ✅ Good - choose one approach
  // Either all DSL or all Annotations
  ```

**2. MissingModuleAnnotation**
- **Severity:** Warning
- **Active:** Yes
- **Description:** Class has `@Single`/`@Factory` but no `@Module` on companion object
- **Rationale:** Annotation processor won't find definitions without `@Module`
- **Example:**
  ```kotlin
  // ❌ Bad
  class MyServices {
      @Single
      fun provideRepo(): Repository = ... // Won't be discovered!
  }

  // ✅ Good
  @Module
  class MyServices {
      @Single
      fun provideRepo(): Repository = ...
  }
  ```

**3. ConflictingBindings**
- **Severity:** Error
- **Active:** Yes
- **Description:** Same type defined in both DSL and Annotations
- **Rationale:** Runtime conflict - which definition wins?
- **Detection:** Track definitions by type, report duplicates
- **Example:**
  ```kotlin
  // ❌ Bad - conflict
  @Module
  class AnnotatedModule {
      @Single
      fun provideRepo(): Repository = RepoImpl()
  }

  val dslModule = module {
      single<Repository> { RepoImpl() } // Conflict!
  }
  ```

**4. ScopedWithoutQualifier**
- **Severity:** Warning
- **Active:** Yes
- **Description:** `@Scoped` without scope name/qualifier
- **Rationale:** Default scope may be unclear, better to be explicit
- **Example:**
  ```kotlin
  // ❌ Bad
  @Scoped
  class MyService // Which scope?

  // ✅ Good
  @Scoped(name = "userScope")
  class MyService
  ```

**5. AnnotationProcessorNotConfigured**
- **Severity:** Error
- **Active:** Yes
- **Description:** Uses `@Single`/`@Factory` but KSP/KAPT not configured
- **Detection:** Check for annotations but no generated code in build directory
- **Fallback:** If cannot determine - show info message, not error
- **Example:**
  ```kotlin
  // build.gradle.kts missing:
  // plugins { id("com.google.devtools.ksp") }
  // dependencies { ksp("io.insert-koin:koin-ksp-compiler") }

  @Single
  class MyService // Won't work - processor not set up!
  ```

---

#### DX Foundations (v0.3.0)

**Suppression Support:**
All rules support:
```kotlin
@Suppress("NoKoinComponentInterface")
class LegacyClass : KoinComponent { ... }

@file:Suppress("KoinInjectInPreview")
```

**Enhanced Messages Format:**
```
[Problem] → [Why it's bad] → [How to fix] + [Code example]

Example:
KoinComponent interface found → Breaks dependency inversion, harder to test
→ Use constructor injection instead

✗ Bad:  class MyRepo : KoinComponent { val api = get<Api>() }
✓ Good: class MyRepo(private val api: Api)
```

**Configuration Improvements:**
- All new rules configurable
- Severity levels via detekt config
- Custom patterns/exclusions support

---

### v0.4.0 Components

#### Auto-Fixes (10+ quick fixes)

**Priority Auto-Fixes:**

1. **NoKoinComponentInterface** → Remove interface, add constructor params
2. **NoInjectDelegate** → Replace `by inject()` with constructor param
3. **EmptyModule** → Remove empty module or add TODO comment
4. **DeprecatedKoinApi** → Replace with modern API
5. **KoinInjectInPreview** → Add `@Preview` parameter or remove inject
6. **MissingModuleAnnotation** → Add `@Module` annotation
7. **ScopedWithoutQualifier** → Add `name = "scopeName"` parameter
8. **AndroidContextNotFromKoin** → Move to `startKoin {}` block
9. **ModuleIncludesOrganization** → Extract to separate module
10. **SingleForNonSharedDependency** → Change to `factory {}`

**Auto-Fix Architecture:**
```kotlin
abstract class KoinQuickFix(
    val description: String,
    val canFix: (PsiElement) -> Boolean
) {
    abstract fun applyFix(element: PsiElement): PsiElement

    protected fun createConstructorParam(
        name: String,
        type: String
    ): KtParameter { ... }

    protected fun removeInterface(
        klass: KtClass,
        interfaceName: String
    ) { ... }
}

class NoKoinComponentFix : KoinQuickFix(
    description = "Remove KoinComponent and use constructor injection",
    canFix = { it is KtClass && it.implementsKoinComponent() }
) {
    override fun applyFix(element: PsiElement): PsiElement {
        val klass = element as KtClass
        val getters = klass.findAllGetCalls()
        val params = getters.map { createParam(it) }

        klass.addConstructorParams(params)
        klass.removeInterface("KoinComponent")
        klass.replaceGettersWithFields()

        return klass
    }
}
```

---

#### Advanced Configuration

**Baseline Support:**
```xml
<!-- .detekt-baseline.xml -->
<baseline>
  <ID>NoKoinComponentInterface:MyLegacyClass.kt:12</ID>
  <!-- Legacy violations ignored -->
</baseline>
```

**Enhanced Exclude Patterns:**
```yaml
koin-rules:
  excludes:
    - '**/test/**'
    - '**/generated/**'
    - '**/build/**'

  NoKoinComponentInterface:
    active: true
    excludePatterns:
      - '.**Application'      # Allow in Application classes
      - '.**Activity'         # Allow in Android Activities
      - '.**Fragment'         # Allow in Android Fragments
    allowedSuperTypes:
      - 'Application'
      - 'Activity'
      - 'Fragment'
```

---

## Testing Strategy

### Coverage Standards
- **Line coverage:** 98% minimum
- **Branch coverage:** 70% minimum
- **Method coverage:** 100% target
- **Approach:** TDD - test first, implementation second

### Test Structure per Version

#### v0.2.0 Testing (+50-60 tests)

**Test Files:**
```
src/test/kotlin/io/github/krozov/detekt/koin/
├── platform/compose/
│   ├── KoinViewModelOutsideComposableTest.kt
│   ├── KoinInjectInPreviewTest.kt
│   └── RememberKoinModulesLeakTest.kt
├── platform/ktor/
│   ├── KtorApplicationKoinInitTest.kt
│   └── KtorRouteScopeMisuseTest.kt
├── platform/android/
│   ├── AndroidContextNotFromKoinTest.kt
│   └── ActivityFragmentKoinScopeTest.kt
└── architecture/
    ├── LayerBoundaryViolationTest.kt
    ├── PlatformImportRestrictionTest.kt
    └── CircularModuleDependencyTest.kt
```

**Test Coverage per Rule:**
Minimum 5 tests:
1. Basic violation detection
2. Valid code (no false positives)
3. Edge case 1 (nested structures, generics, etc.)
4. Edge case 2 (qualified calls, safe navigation, etc.)
5. Configuration test (custom settings)

**Integration Tests:**
Update `KoinRulesIntegrationTest`:
- Verify all 25+ rules load via ServiceLoader
- E2E test with multiple violation types
- Config propagation test

**Expected:** 155 → 205+ tests

---

#### v0.3.0 Testing (+40-50 tests)

**Annotation Rule Tests:**
- Mixing detection across file boundaries
- Annotation correctness (missing `@Module`, etc.)
- Scope validation

**Suppression Tests:**
Every rule tested with:
- `@Suppress` on declaration
- `@file:Suppress` on file
- Verify suppression works, violations not reported

**Message Quality Tests:**
Automated verification that messages contain:
- Problem description
- Rationale
- Fix suggestion
- Code example (optional but recommended)

**Expected:** 205 → 250+ tests

---

#### v0.4.0 Testing (+30-40 tests)

**Quick Fix Tests:**
Each auto-fix:
- Simple case
- Complex case (multiple dependencies, existing constructor)
- Edge case (partial fix, conflicting code)

**Baseline Tests:**
- Violations in baseline ignored
- New violations still reported
- Baseline file format validation

**Performance Tests:**
- Benchmark new rules
- Ensure <10% overhead vs v0.1.0

**Expected:** 250 → 285+ tests

---

### Test Execution

**CI/CD:**
- All tests in GitHub Actions
- Separate jobs per rule category (faster feedback)
- Coverage gates: 98%/70%

**Performance:**
- Parallel execution: `maxParallelForks = CPU/2`
- Configuration cache enabled
- Test duration: <2 minutes total

---

## Error Handling & Edge Cases

### PSI Traversal Safety

**Defensive Programming Pattern:**
```kotlin
override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    // Safe navigation - no crashes on null
    val callName = expression.calleeExpression?.text ?: return
    if (!isKoinCall(callName)) return

    // Safe parent lookup
    val containingFunction = expression.getStrictParentOfType<KtNamedFunction>() ?: return

    // Null-safe annotation check
    val annotations = containingFunction.annotationEntries
        .mapNotNull { it.shortName?.asString() }

    if (isViolation(expression, annotations)) {
        report(CodeSmell(...))
    }
}
```

**Safety Checklist:**
- ✅ All PSI navigation via `?.` or `?:` early return
- ✅ `getStrictParentOfType<T>()` instead of unsafe casting
- ✅ `text` property always nullable for incomplete PSI
- ✅ Exceptions caught and logged, never crash Detekt

---

### Platform-Specific Edge Cases

**Compose:**
- Incremental compilation → Incomplete PSI → Conservative detection
- Multiplatform → `@Composable` from different packages → Check short name
- Inline functions → Analyze call site, not definition

**Ktor:**
- Deep nested DSLs → Limit traversal depth to 5 levels
- Extension vs standalone functions → Check receiver type

**Android:**
- Multiple Android versions → Check only universal patterns
- Obfuscated code → Skip if cannot determine structure

---

### Architecture Rules Edge Cases

**LayerBoundaryViolation:**
```kotlin
// Edge Case 1: Star imports
import org.koin.core.*
// Solution: Treat star import as violation if package restricted

// Edge Case 2: Typealias
typealias MyQualifier = org.koin.core.qualifier.Qualifier
// Solution: Resolve typealias target, check actual type

// Edge Case 3: Companion imports
import com.example.AppModule.Companion.koinFunction
// Solution: Check import path contains restricted package
```

**Graceful Degradation:**
```kotlin
private val restrictedLayers: List<String> = try {
    valueOrDefault("restrictedLayers", emptyList())
} catch (e: Exception) {
    logger.warn("Invalid restrictedLayers config: ${e.message}")
    emptyList() // Rule inactive if config invalid
}
```

---

### Configuration Validation

**Invalid Config Example:**
```yaml
LayerBoundaryViolation:
  active: true
  restrictedLayers: "domain"  # Should be list!
```

**Validation:**
```kotlin
sealed class ValidationResult {
    object Ok : ValidationResult()
    data class Warning(val message: String) : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

private fun validateConfig(config: Config): ValidationResult {
    val layers = config.valueOrNull<Any>("restrictedLayers")
    return when {
        layers == null ->
            ValidationResult.Warning("restrictedLayers not configured")
        layers !is List<*> ->
            ValidationResult.Error("restrictedLayers must be a list")
        layers.isEmpty() ->
            ValidationResult.Warning("restrictedLayers is empty")
        else -> ValidationResult.Ok
    }
}
```

**Error Handling:**
- Config errors → Show in Detekt output
- Runtime errors → Log, don't crash
- Fallback → Rule inactive on error

---

### Backwards Compatibility

**Version Support:**
- Detekt: 1.23.8+ (test on 1.23.8 and 2.0.0 when available)
- Kotlin: 2.0+ (PSI API stable in 2.x)
- Koin: 4.x primary, 3.x compatible (naming patterns same)

**Breaking Changes:**
- Only in major versions (v1.0.0, v2.0.0)
- Deprecation warnings one version before removal

---

## Implementation Plan Overview

### v0.2.0: Platform & Architecture (2-3 weeks)

**Week 1: Platform Rules**
- Day 1-2: Compose (3 rules + tests)
- Day 3-4: Ktor (2-3 rules + tests)
- Day 5: Android (2-3 rules + tests)

**Week 2: Architecture Rules**
- Day 1-2: LayerBoundaryViolation + tests
- Day 3: PlatformImportRestriction + tests
- Day 4: CircularModuleDependency + tests
- Day 5: Integration tests, bug fixes

**Week 3: Polish & Release**
- Day 1-2: Documentation updates
- Day 3: CI/CD verification
- Day 4: Manual testing on real projects
- Day 5: Release v0.2.0

**Deliverables:**
- 10 new rules
- 50+ new tests
- Coverage: 98%/70%
- Updated docs

---

### v0.3.0: Annotations + DX (2-3 weeks)

**Week 1: Annotation Rules**
- Day 1-2: MixingDslAndAnnotations + MissingModuleAnnotation
- Day 3: ConflictingBindings + ScopedWithoutQualifier
- Day 4: AnnotationProcessorNotConfigured
- Day 5: Tests + edge cases

**Week 2: DX Foundations**
- Day 1-2: Suppression support (all rules)
- Day 3-4: Enhanced messages (all rules)
- Day 5: Configuration improvements

**Week 3: Integration & Release**
- Day 1: Integration tests
- Day 2: Backward compatibility testing
- Day 3: Documentation
- Day 4: Real project testing
- Day 5: Release v0.3.0

**Deliverables:**
- 5 annotation rules
- Suppression support (30+ rules)
- Enhanced messages (all rules)
- 45+ new tests
- Coverage: 98%/70%

---

### v0.4.0: Advanced DX (1-2 weeks)

**Week 1: Auto-Fixes**
- Day 1-2: Quick fix infrastructure
- Day 3-4: Top 5 auto-fixes
- Day 5: Tests for auto-fixes

**Week 2: Config & Release**
- Day 1: Baseline support
- Day 2: Exclude patterns
- Day 3: Remaining 5+ auto-fixes
- Day 4: Documentation + migration guide
- Day 5: Release v0.4.0

**Deliverables:**
- 10+ auto-fixes
- Baseline support
- Enhanced exclusions
- 35+ new tests
- Total: 285+ tests, 30+ rules

---

## Success Metrics

### Per Version
- ✅ 100% tests passing
- ✅ 98% line / 70% branch coverage
- ✅ Zero regressions
- ✅ CI/CD green
- ✅ Documentation complete

### Overall (Post v0.4.0)
- 📊 30+ rules (14 → 30+)
- 🧪 285+ tests (155 → 285+)
- 🎯 98.5%+ coverage maintained
- 🚀 10+ auto-fixes
- 📚 Comprehensive docs
- 🏗️ Architecture validation
- 🎨 Platform-specific support
- 📝 Full Koin Annotations support

---

## Risk Mitigation

### Technical Risks
- **PSI API changes:** Test on multiple Kotlin versions
- **Performance:** Benchmarking in v0.4.0
- **False positives:** Extensive edge case testing

### Process Risks
- **Scope creep:** Strict phased adherence
- **Testing debt:** TDD mandatory, coverage gates
- **Documentation lag:** Docs in each sprint

### Mitigation
- Weekly releases for feedback
- Dogfooding on real projects
- Community feedback via GitHub

---

## Appendix: Configuration Examples

### Clean Architecture Setup
```yaml
koin-rules:
  LayerBoundaryViolation:
    active: true
    restrictedLayers:
      - 'com.example.domain'
      - 'com.example.core'
    allowedImports:
      - 'org.koin.core.qualifier.Qualifier'

  PlatformImportRestriction:
    active: true
    restrictions:
      - import: 'org.koin.android.*'
        allowedPackages: ['com.example.app']
      - import: 'org.koin.compose.*'
        allowedPackages: ['com.example.ui']
```

### Compose Project Setup
```yaml
koin-rules:
  KoinViewModelOutsideComposable:
    active: true
  KoinInjectInPreview:
    active: true
  RememberKoinModulesLeak:
    active: true
  ComposeKoinScopeNotClosed:
    active: true  # Strict mode
```

### Ktor Project Setup
```yaml
koin-rules:
  KtorApplicationKoinInit:
    active: true
  KtorRouteScopeMisuse:
    active: true
  KtorDIInRoute:
    active: true
```

---

**End of Design Document**
