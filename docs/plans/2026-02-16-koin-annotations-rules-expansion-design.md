# Koin Annotations Rules Expansion Design

**Date:** 2026-02-16
**Status:** Approved
**Author:** Claude Code
**Research Base:** 150+ GitHub issues from InsertKoinIO/koin-annotations and InsertKoinIO/koin

---

## Executive Summary

The Koin KSP processor validates **dependency graph completeness** and **cycle detection**, but does NOT validate annotation semantic correctness, type hierarchy constraints, or annotation pairing requirements. This creates a significant gap where incorrect annotation usage passes compilation but fails at runtime.

This design adds **7 new annotation rules** and **improves 5 existing annotation rules** to fill these KSP gaps, bringing the annotations category from 5 to 12 rules.

**Scope:** 7 new rules + 5 improvements
**Category:** `annotations/`
**Expected Impact:** Prevent runtime crashes, silent bugs, and invalid KSP code generation

---

## Part 1: New Rules (7)

### Rule 1: `SingleAnnotationOnObject`

**Priority:** HIGH | **Complexity:** EASY
**Koin Issue:** [koin-annotations#72](https://github.com/InsertKoinIO/koin-annotations/issues/72)

**Problem:** `@Single` on a Kotlin `object` declaration generates `single { MyObject() }` — calling a non-existent constructor. Objects are language-level singletons; annotating them is redundant and produces invalid code.

**Detection:**
- PSI: `visitClass` → check `isObject() && hasKoinDefinitionAnnotation()`
- Annotations to check: `@Single`, `@Factory`, `@Scoped`, `@KoinViewModel`, `@KoinWorker`

**Message:** "@Single/@Factory annotation on Kotlin object declaration generates invalid constructor call. Objects are already singletons — remove the annotation or convert to a class."

**Severity:** Warning | **Default:** Active

---

### Rule 2: `TooManyInjectedParams`

**Priority:** HIGH | **Complexity:** EASY
**Koin Issue:** [koin#2141](https://github.com/InsertKoinIO/koin/issues/2141)

**Problem:** `ParametersHolder` only supports destructuring up to `component5()`. Classes with >5 `@InjectedParam` parameters fail at compile time with a confusing "requires operator function component6()" error.

**Detection:**
- PSI: `visitClass` → count primary constructor parameters with `@InjectedParam`
- Configurable threshold (default: 5)

**Message:** "Class has N @InjectedParam parameters, but ParametersHolder only supports destructuring up to 5. Reduce the number of injected parameters or use a wrapper class."

**Severity:** Warning | **Default:** Active
**Configuration:** `maxInjectedParams` (default: 5)

---

### Rule 3: `InvalidNamedQualifierCharacters`

**Priority:** HIGH | **Complexity:** EASY
**Koin Issue:** [koin-annotations#245](https://github.com/InsertKoinIO/koin-annotations/issues/245)

**Problem:** `@Named("ricky-morty")` — hyphens and special characters in named values cause KSP code generation failures because the value is used in generated Kotlin identifiers.

**Detection:**
- PSI: `visitAnnotationEntry` → match `@Named` → extract string value → validate against `^[a-zA-Z][a-zA-Z0-9_.]*$`

**Message:** "@Named value 'X' contains characters that are invalid in generated Kotlin identifiers. Use only letters, digits, underscores, and dots."

**Severity:** Warning | **Default:** Active

---

### Rule 4: `KoinAnnotationOnExtensionFunction`

**Priority:** HIGH | **Complexity:** EASY
**Koin Issues:** [koin-annotations#277](https://github.com/InsertKoinIO/koin-annotations/issues/277), [koin-annotations#274](https://github.com/InsertKoinIO/koin-annotations/issues/274)

**Problem:** `@Single fun Scope.provideDatastore(): DataStore` — KSP code generator ignores the receiver parameter, producing invalid generated code. Extension functions are not supported as Koin definition providers.

**Detection:**
- PSI: `visitNamedFunction` → check `receiverTypeReference != null && hasKoinDefinitionAnnotation()`
- Only within `@Module`-annotated classes

**Message:** "Koin definition annotation on extension function is not supported. KSP ignores the receiver parameter, producing invalid code. Convert to a regular function with an explicit parameter."

**Severity:** Warning | **Default:** Active

---

### Rule 5: `ViewModelAnnotatedAsSingle`

**Priority:** HIGH | **Complexity:** EASY
**Koin Issues:** [koin#2310](https://github.com/InsertKoinIO/koin/issues/2310), [koin-annotations#35](https://github.com/InsertKoinIO/koin-annotations/issues/35)

**Problem:** ViewModel classes annotated with `@Single` instead of `@KoinViewModel` cause coroutine scope issues after navigation — the ViewModel's `viewModelScope` is cancelled but the singleton instance persists, leading to `CancellationException` on subsequent use.

**Detection:**
- PSI: `visitClass` → check supertype chain for `ViewModel`/`AndroidViewModel` && `hasAnnotation("Single")`
- Also check for `@Factory` (should be `@KoinViewModel`)

**Message:** "ViewModel class annotated with @Single instead of @KoinViewModel. ViewModels should be scoped to navigation components, not declared as singletons. Use @KoinViewModel for proper lifecycle management."

**Severity:** Warning | **Default:** Active

---

### Rule 6: `AnnotatedClassImplementsNestedInterface`

**Priority:** HIGH | **Complexity:** MEDIUM
**Koin Issues:** [koin-annotations#17](https://github.com/InsertKoinIO/koin-annotations/issues/17), [koin-annotations#44](https://github.com/InsertKoinIO/koin-annotations/issues/44)

**Problem:** When a class annotated with `@Single` implements a nested/inner interface (e.g., `ParentClass.ChildInterface`), the KSP code generator drops the parent class qualifier in the `bind()` call, generating `bind(ChildInterface::class)` instead of `bind(ParentClass.ChildInterface::class)`. This causes resolution failures at runtime.

**Detection:**
- PSI: `visitClass` → for each supertype, check if the type reference contains a dot-qualified expression (nested type) && class has Koin definition annotation
- Check `binds` parameter too — if explicit binds reference a nested type

**Message:** "Class with Koin annotation implements nested interface 'Parent.Child'. KSP may generate incorrect bind() call without parent qualifier, causing runtime resolution failure. Consider extracting the interface to a top-level declaration."

**Severity:** Warning | **Default:** Active

---

### Rule 7: `InjectedParamWithNestedGenericType`

**Priority:** HIGH | **Complexity:** MEDIUM
**Koin Issues:** [koin-annotations#314](https://github.com/InsertKoinIO/koin-annotations/issues/314), [koin-annotations#240](https://github.com/InsertKoinIO/koin-annotations/issues/240), [koin-annotations#298](https://github.com/InsertKoinIO/koin-annotations/issues/298), [koin-annotations#272](https://github.com/InsertKoinIO/koin-annotations/issues/272)

**Problem:** `@InjectedParam` with nested generic types (e.g., `List<List<String>>`, `Map<String, List<Int>>`) generates incorrect code — inner type arguments are dropped. Also, star projections (`List<*>`) reproduce the same bug.

**Detection:**
- PSI: `visitParameter` → `hasAnnotation("InjectedParam")` → check if type reference has nested type arguments (depth > 1) or star projections

**Message:** "@InjectedParam with nested generic type 'X' may generate incorrect code due to a known KSP bug. Consider using a typealias or wrapper class to flatten the type."

**Severity:** Warning | **Default:** Active

---

## Part 2: Improvements to Existing Rules (5)

### Improvement 1: `ScopedWithoutQualifier` → Enhanced scope validation

**Current:** Detects `@Scoped` without explicit scope name/qualifier.
**Enhancement:** Also detect:
- `@Scoped` without any `@Scope` annotation on the same class (issues #93, #262)
- `@Scope` annotation present but no `@Scoped` definitions exist (empty scope warning)
- `@Scoped(binds=[...])` where `binds` has no effect (issue #262)

---

### Improvement 2: `MissingModuleAnnotation` → Empty module detection

**Current:** Detects `@Single`/`@Factory`/`@Scoped` without `@Module` on containing class.
**Enhancement:** Also detect:
- `@Module` class without `@ComponentScan` AND without `includes` AND without any `@Single`/`@Factory`/`@Scoped` functions inside — this module will be empty and useless
- Warn about potential orphaned definitions

---

### Improvement 3: `ConflictingBindings` → Duplicate annotations

**Current:** Detects same type defined in both DSL and Annotations.
**Enhancement:** Also detect:
- Duplicate definition annotations on same class (e.g., both `@Single` and `@Factory`) — KSP takes first, behavior is undefined
- `@Single(binds=[X::class])` where X is not a supertype of the annotated class — invalid binding

---

### Improvement 4: `MixingDslAndAnnotations` → Broader annotation detection

**Current:** Detects mixing of `module {}` DSL and `@Module`/`@Single`/`@Factory` in same file.
**Enhancement:** Also recognize:
- `@Configuration` as an annotation marker
- `@KoinApplication` as an annotation marker
- `@ComponentScan` as an annotation marker
- `@KoinViewModel`, `@KoinWorker` as definition annotations

---

### Improvement 5: `AnnotationProcessorNotConfigured` → Better KSP detection

**Current:** Warns when Koin annotations are used but processor may not be configured.
**Enhancement:**
- Improve heuristic to reduce false positives
- Check for `koin-ksp-compiler` in dependencies more reliably
- Consider `KOIN_CONFIG_CHECK` option status

---

## Part 3: GitHub Issues for Future Rules (~13)

Issues to create for rules NOT included in this implementation:

| # | Rule Name | Category | Priority | Source Issues |
|---|-----------|----------|----------|---------------|
| 1 | `QualifierObfuscationRisk` | annotations | HIGH | koin-annotations#328, koin#2135 |
| 2 | `SingleBindsGenericInterfaces` | annotations | MEDIUM | koin-annotations#321 |
| 3 | `ComponentScanPackageMismatch` | annotations | HIGH | koin-annotations#236, #249 |
| 4 | `AnnotatedClassOutsideModule` | annotations | MEDIUM | koin-annotations#325, #324 |
| 5 | `AnnotatedClassUsesExperimentalApi` | annotations | MEDIUM | koin-annotations#228 |
| 6 | `ScopedBindsHasNoEffect` | annotations | MEDIUM | koin-annotations#262 |
| 7 | `KoinAnnotationOnClassWithNestedDeclarations` | annotations | MEDIUM | koin-annotations#36, #309 |
| 8 | `MissingKoinStopInTest` | testing | MEDIUM | koin-annotations#323, koin#1557 |
| 9 | `ExpectModuleWithoutActual` | KMP | MEDIUM | koin-annotations#332, #238 |
| 10 | `InjectedParamAnnotationOrder` | annotations | MEDIUM | koin-annotations#315 |
| 11 | `SingleOnAbstractClass` | annotations | HIGH | KSP gap analysis |
| 12 | `KoinViewModelOnNonViewModel` | annotations | HIGH | KSP gap analysis |
| 13 | `KoinWorkerOnNonWorker` | annotations | HIGH | KSP gap analysis |

---

## Architecture

### File Structure

```
src/main/kotlin/io/github/krozov/detekt/koin/annotations/
├── ConflictingBindings.kt              (IMPROVED)
├── MissingModuleAnnotation.kt          (IMPROVED)
├── MixingDslAndAnnotations.kt          (IMPROVED)
├── ScopedWithoutQualifier.kt           (IMPROVED)
├── AnnotationProcessorNotConfigured.kt (IMPROVED)
├── SingleAnnotationOnObject.kt         (NEW)
├── TooManyInjectedParams.kt            (NEW)
├── InvalidNamedQualifierCharacters.kt  (NEW)
├── KoinAnnotationOnExtensionFunction.kt(NEW)
├── ViewModelAnnotatedAsSingle.kt       (NEW)
├── AnnotatedClassImplementsNestedInterface.kt (NEW)
└── InjectedParamWithNestedGenericType.kt      (NEW)
```

### Test Structure

```
src/test/kotlin/io/github/krozov/detekt/koin/annotations/
├── ConflictingBindingsTest.kt              (EXTENDED)
├── MissingModuleAnnotationTest.kt          (EXTENDED)
├── MixingDslAndAnnotationsTest.kt          (EXTENDED)
├── ScopedWithoutQualifierTest.kt           (EXTENDED)
├── AnnotationProcessorNotConfiguredTest.kt (EXTENDED)
├── SingleAnnotationOnObjectTest.kt         (NEW)
├── TooManyInjectedParamsTest.kt            (NEW)
├── InvalidNamedQualifierCharactersTest.kt  (NEW)
├── KoinAnnotationOnExtensionFunctionTest.kt(NEW)
├── ViewModelAnnotatedAsSingleTest.kt       (NEW)
├── AnnotatedClassImplementsNestedInterfaceTest.kt (NEW)
└── InjectedParamWithNestedGenericTypeTest.kt      (NEW)
```

### Registration

All new rules registered in `KoinRuleSetProvider.kt` under existing annotations section.

---

## Quality Requirements

- Line coverage: ≥96%
- Branch coverage: ≥70%
- Tests per rule: ≥3 (happy path violation, valid code, edge cases)
- No false positives in self-dogfooding
- Documentation updated (docs/rules.md, README.md, CHANGELOG.md)

---

## Research Sources

### GitHub Issues Analyzed
- InsertKoinIO/koin-annotations: 32 open + 50 recent closed issues
- InsertKoinIO/koin: annotation-related issues filtered from 200+ issues

### Documentation Reviewed
- [Koin Annotations Inventory](https://insert-koin.io/docs/reference/koin-annotations/annotations-inventory/)
- [Koin Annotations Definitions](https://insert-koin.io/docs/reference/koin-annotations/definitions/)
- [Compile-Time Safety with Koin Annotations](https://medium.com/@kerry.bisset/compile-time-safety-with-koin-annotations)
- [Achieving Compile-Time Safety in Koin](https://carrion.dev/en/posts/koin-compile-safety/)
- DeepWiki analysis of InsertKoinIO/koin-annotations KSP processor internals
