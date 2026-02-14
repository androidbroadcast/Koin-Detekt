# Design: Fix False Positive on Non-Koin get() Methods

**Date:** 2026-02-14
**Status:** Approved
**Target Version:** 0.4.1 (bugfix release)

## Problem Statement

Rules `NoGetOutsideModuleDefinition` and `NoKoinGetInApplication` currently produce false positives on non-Koin methods with names `get()`, `getOrNull()`, and `getAll()`.

**Examples of false positives:**
- `java.util.concurrent.atomic.AtomicReference.get()`
- `kotlinx.coroutines.channels.SendChannel.getOrNull()`
- `Map.get(key)`
- `List.getOrNull(index)`
- Any other non-Koin methods with these names

**Root cause:** Rules check only method name, without verifying it's actually a Koin API call.

## Solution Approach

**Selected approach:** Import-based detection (simple and reliable)

### How it works:

1. **Collect imports** at file level from `org.koin.*` packages
2. **Check method calls** only if the function name is imported from Koin
3. **Ignore all other** methods with the same names

### Why this approach:

✅ **Simple implementation** - no type resolution required
✅ **Reliable** - type resolution not always available in Detekt
✅ **Covers 99.9% of real usage** - Koin functions are always imported in practice
✅ **Fast** - import analysis is lightweight
✅ **Predictable** - same behavior in all projects

### Trade-off:

⚠️ Won't detect edge case: qualified calls without import
```kotlin
// This won't be detected (extremely rare in practice)
val service = org.koin.core.component.get<Service>()
```

## Architecture

### Koin imports to track:

```kotlin
org.koin.core.component.get
org.koin.core.component.getOrNull
org.koin.core.component.getAll
org.koin.core.component.inject
org.koin.core.scope.Scope.get
org.koin.core.scope.Scope.getOrNull
org.koin.core.scope.Scope.getAll
```

### Algorithm:

```kotlin
1. visitImportDirective() → collect imports from org.koin.* packages
2. visitCallExpression() → check:
   - Method name in ["get", "getOrNull", "getAll", "inject"]?
   - Method name in collected koinImports?
   - (existing logic) Not inside definition block?
3. If all conditions true → report violation
```

## Implementation Changes

### Files to modify:

**1. `NoGetOutsideModuleDefinition.kt`**
- Add field: `private val koinImports = mutableSetOf<String>()`
- Add method: `override fun visitImportDirective()` to collect Koin imports
- Modify: `visitCallExpression()` to check `callName in koinImports`

**2. `NoKoinGetInApplication.kt`**
- Apply the same import-checking logic
- Track both `get` and `inject` functions

### Pseudocode:

```kotlin
internal class NoGetOutsideModuleDefinition(config: Config) : Rule(config) {
    private val koinImports = mutableSetOf<String>()

    override fun visitImportDirective(importDirective: KtImportDirective) {
        val importPath = importDirective.importPath?.pathStr
        if (importPath?.startsWith("org.koin.") == true) {
            importDirective.importedName?.asString()?.let { koinImports.add(it) }
        }
        super.visitImportDirective(importDirective)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // NEW: Check if function is imported from Koin
        if (callName !in koinImports) {
            super.visitCallExpression(expression)
            return
        }

        // Existing logic continues...
        if (callName in definitionFunctions) { ... }
        if (callName in setOf("get", "getOrNull", "getAll") && !insideDefinitionBlock) {
            report(...)
        }
        super.visitCallExpression(expression)
    }
}
```

## Testing Strategy

### New tests to add:

**In `NoGetOutsideModuleDefinitionTest.kt`:**

```kotlin
@Test
fun `does not report AtomicReference get()`()

@Test
fun `does not report SendChannel getOrNull()`()

@Test
fun `does not report Map get()`()

@Test
fun `does not report List getOrNull()`()

@Test
fun `still reports Koin get() with import`()
```

### Existing tests requiring changes:

**`NoGetOutsideModuleDefinitionTest.kt` (5 tests):**
- Add `import org.koin.core.component.get` to:
  - `reports get in init block`
  - `reports get in property initializer`
  - `reports get in companion object`
  - `does not report get() inside single block`
  - `does not report get() inside factory block`

**`NoKoinGetInApplicationTest.kt` (10 tests):**
- Add `import org.koin.core.component.get` to 7 tests
- Add `import org.koin.core.component.inject` to 3 tests
- See detailed list in brainstorming notes

## Documentation Updates

### `docs/rules.md`

Update `NoGetOutsideModuleDefinition` section:
- Add note about import-based detection
- Add examples of non-Koin methods that won't be reported
- Clarify that only `org.koin.*` imports are checked

### `CHANGELOG.md`

Add entry for v0.4.1:
```markdown
## [0.4.1] - 2026-02-14

### Fixed
- **NoGetOutsideModuleDefinition**: Fixed false positive on non-Koin `get()` methods
  - Rule now checks only Koin functions imported from `org.koin.*` packages
  - No longer reports `AtomicReference.get()`, `SendChannel.getOrNull()`, `Map.get()`, etc.
- **NoKoinGetInApplication**: Fixed false positive on non-Koin `get()` and `inject()` methods
```

## Backward Compatibility

✅ **Fully backward compatible** - change only reduces false positives:
- Real Koin API violations → continue to be detected
- False positives on non-Koin methods → stop being reported
- No breaking changes for users

## Release Plan

- **Version:** 0.4.1 (patch release - bugfix)
- **Current:** 0.4.0
- **Type:** Bugfix, not breaking change

## Success Criteria

1. ✅ All new tests pass
2. ✅ All existing tests pass (with import additions)
3. ✅ Coverage remains ≥96% line, ≥70% branch
4. ✅ `AtomicReference.get()` no longer reported
5. ✅ `SendChannel.getOrNull()` no longer reported
6. ✅ Koin `get<T>()` with import still reported correctly
7. ✅ Documentation updated
8. ✅ CHANGELOG updated

## Implementation Plan

Next step: Use `writing-plans` skill to create detailed implementation plan with step-by-step tasks.
