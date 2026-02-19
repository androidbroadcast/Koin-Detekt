# README + Docs v1.0.0 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Clean up README for v1.0.0 release and sync docs with actual 51 rules in code.

**Architecture:** Three files to update independently — README.md (remove verbose table, keep concise), docs/rules.md (add 5 undocumented rules), docs/configuration.md (version bump + new config section). No code changes.

**Tech Stack:** Markdown, detekt-koin4-rules v1.0.0

---

### Task 1: Update README.md

**Files:**
- Modify: `README.md`

**Context:** README currently has full tables listing all rules per category (43 rows). This is verbose and gets out of sync with code. We replace it with a compact category summary + link to docs/rules.md. Also update version in the installation snippet.

**Step 1: Open and verify current README**

Read `README.md`. Confirm it has:
- Badge row (lines 3–6)
- One-liner description mentioning "51 rules" (line 8)
- `## Installation` with version `0.4.0` (lines 10–16)
- `## Rules` section with 6 category tables (lines 18–93)
- Config example (lines 95–114)
- Requirements, License at bottom

**Step 2: Replace the Rules section**

Find this block in `README.md` (from `## Rules` to `📖 [Complete Rule Documentation](docs/rules.md)`):

```markdown
## Rules

### Service Locator (5)
...
### Koin Annotations (12)
...
📖 [Complete Rule Documentation](docs/rules.md)
```

Replace the entire `## Rules` section with:

```markdown
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
```

**Step 3: Update installation version**

Change:
```kotlin
detektPlugins("dev.androidbroadcast.rules.koin:detekt-koin4-rules:0.4.0")
```
To:
```kotlin
detektPlugins("dev.androidbroadcast.rules.koin:detekt-koin4-rules:1.0.0")
```

**Step 4: Verify README looks correct**

Read the updated file. Confirm:
- Installation shows `1.0.0`
- Rules section shows the compact table (6 rows + link)
- No category-level tables remain
- Configuration example, Requirements, License are untouched

**Step 5: Commit**

```bash
git add README.md
git commit -m "docs: simplify rules section in README, update to v1.0.0"
```

---

### Task 2: Add 5 missing rules to docs/rules.md

**Files:**
- Modify: `docs/rules.md`

**Context:** The following rules exist in code but have no docs entry. Source code for each was already read during brainstorming and is reproduced below for reference.

Rules to add:
1. `ExcessiveCreatedAtStart` — Module DSL section (after `EnumQualifierCollision`)
2. `ModuleAsTopLevelVal` — Module DSL section (after `ExcessiveCreatedAtStart`)
3. `ScopeDeclareWithActivityOrFragment` — Scope Management section (after `KtorRequestScopeMisuse`)
4. `GetConcreteTypeInsteadOfInterface` — Architecture section (after `CircularModuleDependency`)
5. `StartKoinInActivity` — Platform section, under Android Rules (after `AndroidContextNotFromKoin`)

**Step 1: Update Module DSL header count**

Find in `docs/rules.md`:
```markdown
## Module DSL Rules
```
The section header doesn't include a count, so no change needed here. But find `ConstructorDslAmbiguousParameters` and `ParameterTypeMatchesReturnType` — they are already documented in rules.md (verified during brainstorming). Good.

**Step 2: Add ExcessiveCreatedAtStart after EnumQualifierCollision**

Find the line `**Related Issue:** [Koin#2364](https://github.com/InsertKoinIO/koin/issues/2364)` (end of EnumQualifierCollision) and append after the `---` separator:

```markdown
### ExcessiveCreatedAtStart

**Severity:** Warning
**Active by default:** Yes
**Configurable:** Yes

Detects modules with too many `createdAtStart = true` definitions. Eager initialization of many singletons at startup causes ANR (Application Not Responding) on Android, especially on lower-end devices.

❌ **Bad:**
```kotlin
val appModule = module {
    single(createdAtStart = true) { Service1() }
    single(createdAtStart = true) { Service2() }
    // ... 11+ eager definitions
    single(createdAtStart = true) { Service11() }
}
```

✅ **Good:**
```kotlin
val appModule = module {
    single(createdAtStart = true) { LoggingService() }  // Critical
    single(createdAtStart = true) { CrashReporter() }   // Critical
    single { DatabaseService() }   // Lazy — OK
    single { NetworkService() }    // Lazy — OK
}
```

**Configuration:**
```yaml
ExcessiveCreatedAtStart:
  active: true
  maxCreatedAtStart: 10
```

**Edge Cases:**
- ✅ Counts `createdAtStart = true` across all definition types: `single`, `factory`, `scoped`
- ✅ Threshold is per-module (not per-file)
- ✅ Default threshold is 10 (configurable)

**Related Issue:** [Koin#2266](https://github.com/InsertKoinIO/koin/issues/2266)

---
```

**Step 3: Add ModuleAsTopLevelVal after ExcessiveCreatedAtStart**

Append after the ExcessiveCreatedAtStart entry:

```markdown
### ModuleAsTopLevelVal

**Severity:** Code Smell
**Active by default:** Yes

Detects Koin modules defined as top-level `val` instead of functions. Val declarations preallocate all factory lambdas at initialization time, even if never used. Functions defer creation until needed.

❌ **Bad:**
```kotlin
val appModule = module {
    single { Service() }
    factory { Repository() }
}
```

✅ **Good:**
```kotlin
fun appModule() = module {
    single { Service() }
    factory { Repository() }
}
```

**Edge Cases:**
- ✅ Only detects top-level `val` (not inside class/object)
- ✅ Only flags `val`, not `var`
- ✅ Only triggers when the initializer is a `module {}` call

---
```

**Step 4: Add ScopeDeclareWithActivityOrFragment in Scope Management**

Find the end of `KtorRequestScopeMisuse` entry (its last `---` separator) and append:

```markdown
### ScopeDeclareWithActivityOrFragment

**Severity:** Warning
**Active by default:** Yes

Detects `scope.declare()` called with Activity or Fragment instances. The declared instance is never automatically cleared when the scope closes, causing the Activity/Fragment to leak in memory.

❌ **Bad:**
```kotlin
class MainActivity : AppCompatActivity() {
    fun setupScope() {
        val scope = getKoin().createScope("my_scope", named("activity"))
        scope.declare(this) // Memory leak: Activity never released
    }
}
```

✅ **Good:**
```kotlin
class MainActivity : AppCompatActivity() {
    fun setupScope() {
        val scope = androidScope() // Handles lifecycle automatically
    }
}
```

**Edge Cases:**
- ✅ Detects `scope.declare(activity)`, `scope.declare(fragment)`
- ✅ Detects `scope.declare(this)` when called inside Activity/Fragment class
- ✅ Uses heuristic name matching ("activity", "fragment") and supertype checks

**Related Issue:** [Koin#1122](https://github.com/InsertKoinIO/koin/issues/1122)

---
```

**Step 5: Add GetConcreteTypeInsteadOfInterface in Architecture**

Find the end of `CircularModuleDependency` entry and append before the `## Koin Annotations Rules` header:

```markdown
### GetConcreteTypeInsteadOfInterface

**Severity:** Warning
**Active by default:** Yes

Detects `get<ConcreteImpl>()` when only an interface is registered in the module. Koin's `verify()` passes because it considers secondary bound types, but runtime resolution fails with `NoBeanDefFoundException`. Always request the registered type (interface), not the concrete implementation.

❌ **Bad:**
```kotlin
interface Service
class ServiceImpl : Service

val module = module {
    single<Service> { ServiceImpl() }         // Only Service is registered
    factory { Consumer(get<ServiceImpl>()) }  // Runtime: NoBeanDefFoundException!
}
```

✅ **Good:**
```kotlin
interface Service
class ServiceImpl : Service

val module = module {
    single<Service> { ServiceImpl() }
    factory { Consumer(get<Service>()) } // Request the registered type
}
```

**Edge Cases:**
- ✅ Uses heuristic detection: tracks `single<Interface> { ConcreteImpl() }` bindings
- ✅ Detects `*Impl` / `*Implementation` naming patterns
- ✅ Works within a single file (cross-module dependencies require semantic analysis)
- ✅ Does not report when the concrete type is explicitly registered

**Related Issue:** [Koin#2222](https://github.com/InsertKoinIO/koin/issues/2222)

---
```

**Step 6: Add StartKoinInActivity in Platform > Android Rules**

Find `#### AndroidContextNotFromKoin` section (under `### Android Rules`), find its trailing `---`, and append:

```markdown
#### StartKoinInActivity

**Severity:** Warning
**Active by default:** Yes

Detects `startKoin {}` called inside an Activity, Fragment, or `@Composable` function. Calling `startKoin` in these places causes `KoinAppAlreadyStartedException` on configuration changes (screen rotation, theme switch) because Koin is already running and cannot be started twice.

❌ **Bad:**
```kotlin
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        startKoin { modules(appModule) } // Crashes on rotation!
    }
}
```

✅ **Good:**
```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin { modules(appModule) } // Survives config changes
    }
}
```

**Edge Cases:**
- ✅ Detects `startKoin` in Activity and Fragment subclasses
- ✅ Detects `startKoin` inside `@Composable` functions
- ✅ Does not flag `startKoin` in Application subclasses

**Related Issue:** [Koin#1840](https://github.com/InsertKoinIO/koin/issues/1840)

---
```

**Step 7: Update the header count in docs/rules.md**

Find the first line:
```markdown
Complete reference for all 51 Detekt rules for Koin.
```
The count is already 51 — verify it matches after adding 5 rules. If the original said a different number, update it to 51.

**Step 8: Verify docs/rules.md**

Read the file and confirm:
- ExcessiveCreatedAtStart appears in Module DSL section
- ModuleAsTopLevelVal appears in Module DSL section
- ScopeDeclareWithActivityOrFragment appears in Scope Management section
- GetConcreteTypeInsteadOfInterface appears in Architecture section
- StartKoinInActivity appears in Platform > Android Rules section

**Step 9: Commit**

```bash
git add docs/rules.md
git commit -m "docs: add documentation for 5 undocumented rules"
```

---

### Task 3: Update docs/configuration.md

**Files:**
- Modify: `docs/configuration.md`

**Context:** configuration.md uses version `0.3.0` in the installation snippet. Also missing configuration entry for `ExcessiveCreatedAtStart` (the only new configurable rule).

**Step 1: Update installation version**

Find in `docs/configuration.md`:
```kotlin
detektPlugins("dev.androidbroadcast.rules.koin:detekt-koin4-rules:0.3.0")
```
Replace with:
```kotlin
detektPlugins("dev.androidbroadcast.rules.koin:detekt-koin4-rules:1.0.0")
```

**Step 2: Add ExcessiveCreatedAtStart configuration entry**

Find the `#### EmptyModule` subsection under `### Module DSL Rules`. Insert a new entry after `#### SingleForNonSharedDependency` section (to keep alphabetical order broken by thematic grouping):

```markdown
#### ExcessiveCreatedAtStart

Warn when too many eager singletons risk ANR on startup.

**Default:** Active

```yaml
koin-rules:
  ExcessiveCreatedAtStart:
    active: true
    maxCreatedAtStart: 10
```

**Custom Example - Stricter threshold:**

```yaml
koin-rules:
  ExcessiveCreatedAtStart:
    active: true
    maxCreatedAtStart: 5  # More aggressive — only 5 eager inits allowed
```
```

**Step 3: Verify configuration.md**

Read the file and confirm:
- Version shows `1.0.0` in Basic Setup
- `ExcessiveCreatedAtStart` section appears under Module DSL Rules
- All existing sections are intact

**Step 4: Commit**

```bash
git add docs/configuration.md
git commit -m "docs: update version to 1.0.0, add ExcessiveCreatedAtStart config"
```

---

### Task 4: Final check

**Step 1: Verify cross-doc consistency**

Check that these numbers match across all docs:
- README.md category table totals = actual rule counts per category
- rules.md header says 51 rules
- Installation version = 1.0.0 in both README.md and configuration.md

**Step 2: Verify no broken links**

In README.md confirm:
- `[Complete Rule Documentation](docs/rules.md)` → file exists ✓
- `[Configuration Guide](docs/configuration.md)` → file exists ✓

**Step 3: Final commit if needed**

If any small fixes were made:
```bash
git add README.md docs/rules.md docs/configuration.md
git commit -m "docs: fix cross-doc consistency for v1.0.0"
```
