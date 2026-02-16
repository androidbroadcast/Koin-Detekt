# Future Rule Ideas for detekt-koin

This document tracks potential rule ideas discovered during research but not yet implemented.

---

## High Priority (Deferred)

### ScopedViewComponentLeak
**Priority:** LOW → Consider for v0.6.0
**Issues:** [#1144](https://github.com/InsertKoinIO/koin/issues/1144), [#1122](https://github.com/InsertKoinIO/koin/issues/1122), [#149](https://github.com/InsertKoinIO/koin/issues/149)

**Problem:** Injecting UI-lifecycle-dependent objects (RecyclerView.Adapter, View) using fragment scope causes memory leaks because scoped instances hold view references after `onDestroyView`.

**Detection Pattern:**
```kotlin
// Detect: scoped { Adapter() } in Fragment scope
scope<MyFragment> {
    scoped { MyAdapter() }  // ❌ Holds view references
}
```

**Implementation Challenge:** Requires Android platform type detection (Adapter, View subclasses)

---

## Medium Priority (Research Needed)

### CircularGetDependency (Enhancement)
**Priority:** MEDIUM
**Issues:** [#334](https://github.com/InsertKoinIO/koin/issues/334), [#2041](https://github.com/InsertKoinIO/koin/issues/2041)

**Problem:** Extend existing `CircularModuleDependency` to detect **definition-level circular `get()` chains** within module lambda bodies, not just module-level `includes()` cycles.

**Current Coverage:** `CircularModuleDependency` only checks `includes()` cycles

**Enhancement:**
```kotlin
// Detect: circular get() dependencies
module {
    single { A(get<B>()) }
    single { B(get<A>()) }  // ❌ Stack overflow at runtime
}
```

**Implementation Challenge:** Requires semantic analysis to track get<T>() type resolution across definitions

---

## Low Priority (Nice to Have)

### AndroidContextAccessOutsideModuleDefinition
**Priority:** LOW
**Related to:** Existing `AndroidContextNotFromKoin` rule

**Problem:** Direct Android Context access outside Koin module definitions (e.g., `context.getSystemService()` instead of `androidContext().getSystemService()`)

**Note:** May overlap with `AndroidContextNotFromKoin`. Review if additional coverage needed.

---

### MissingAndroidContextInModuleVerification
**Priority:** LOW
**Issue:** [#553](https://github.com/InsertKoinIO/koin/issues/553)

**Problem:** `checkModules()` / `verify()` fails when modules use `androidContext()` because no Android context is available in unit tests.

**Detection Pattern:**
```kotlin
// Detect: androidContext() in module without test mock
@Test
fun `verify modules`() {
    val koin = koinApplication {
        modules(appModule)  // ❌ Fails if appModule uses androidContext()
    }
    koin.checkModules()
}
```

**Solution:** Suggest providing mock context in tests

**Implementation Challenge:** Requires test code analysis, may be better as documentation

---

## Informational (Not Actionable as Rules)

### Stacked Parameters Propagation
**Issue:** [#2268](https://github.com/InsertKoinIO/koin/issues/2268)

**Problem:** Parameters passed via `parametersOf()` implicitly propagate through dependency chains. This is undocumented behavior.

**Why not a rule:** This is a Koin framework behavior, not a user mistake. Better addressed in documentation than static analysis.

---

### KoinApplication Not Started (KMP/iOS)
**Issues:** [#1652](https://github.com/InsertKoinIO/koin/issues/1652), [#2016](https://github.com/InsertKoinIO/koin/issues/2016), [#2022](https://github.com/InsertKoinIO/koin/issues/2022)

**Problem:** On iOS/KMP, developers forget to initialize Koin or initialize it in the wrong place.

**Why not a rule:** Platform-specific initialization patterns vary widely in KMP. Better addressed through platform-specific documentation and compiler errors.

---

### `by inject()` Not Checked by KOIN_CONFIG_CHECK
**Issue:** [#2143](https://github.com/InsertKoinIO/koin/issues/2143)

**Problem:** Koin's compile-time config check only validates constructor-injected dependencies, not `by inject()` delegates.

**Why not a rule:** Already covered by existing `NoInjectDelegate` rule which discourages `by inject()` entirely.

---

## Research Needed (Blocked)

These rule ideas require capabilities beyond current PSI-based analysis:

### 1. Advanced Type Resolution
Rules requiring semantic type analysis (not just text matching):
- **EnumQualifierCollision** (if heuristic approach insufficient)
- **GetConcreteTypeInsteadOfInterface** (requires tracking registered vs requested types)
- **OverrideInIncludedModule** (requires module dependency graph)

**Potential Solutions:**
- Use Kotlin Compiler Plugin API for type resolution
- Integrate with IDEA's semantic model
- Accept limitations and use heuristic approaches

### 2. Test Code Analysis
Rules that need to analyze test files:
- **MissingAndroidContextInModuleVerification**

**Note:** Detekt primarily targets production code. Test-specific rules may belong in separate ruleset.

---

## Rejected Ideas

### Generic Type Parameters Validation
**Considered:** Validate that generic types in `get<List<T>>()` match `single { listOf<T>() }`

**Rejected because:** Type erasure at runtime makes this unreliable. `GenericDefinitionWithoutQualifier` already covers the root issue (use qualifiers for generic types).

---

### ViewModel Factory Pattern Enforcement
**Considered:** Enforce `SavedStateHandle` parameter in ViewModel constructors

**Rejected because:** Too opinionated. Projects may or may not use SavedStateHandle.

---

## Contributing New Ideas

When adding new rule ideas to this document:

1. **Categorize by priority** (High/Medium/Low/Research Needed)
2. **Link GitHub issues** from Koin repository
3. **Describe the problem** and impact
4. **Provide detection pattern** (code examples)
5. **Note implementation challenges** if any
6. **Explain if rejected** and why

---

## Changelog

- **2026-02-16:** Initial document created with 15 deferred/future rule ideas from Koin issues research
