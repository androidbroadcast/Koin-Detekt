# Startup & Miscellaneous Rules Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement 6 Koin rules for startup issues, verification mismatches, and module organization best practices

**Architecture:** PSI rules detecting startKoin in wrong lifecycle methods, excessive eager initialization, misuse of scope.declare(), and other miscellaneous anti-patterns

**Tech Stack:** Kotlin, Detekt API 1.23.8+, JUnit 5, AssertJ

---

## Overview

- **Number of rules:** 6
- **Priority:** MEDIUM (2 rules), LOW (4 rules)
- **Detekt categories:** `platform`, `moduledsl`, `architecture`, `scope`
- **Estimated effort:** ~4-6 hours

## Rules in this group

1. **StartKoinInActivity** (MEDIUM, Issue: [#1840](https://github.com/InsertKoinIO/koin/issues/1840))
2. **GetConcreteTypeInsteadOfInterface** (MEDIUM, Issue: [#2222](https://github.com/InsertKoinIO/koin/issues/2222))
3. **ExcessiveCreatedAtStart** (LOW, Issue: [#2266](https://github.com/InsertKoinIO/koin/issues/2266))
4. **ScopeDeclareWithActivityOrFragment** (LOW, Issue: [#1122](https://github.com/InsertKoinIO/koin/issues/1122))
5. **OverrideInIncludedModule** (LOW, Issue: [#1919](https://github.com/InsertKoinIO/koin/issues/1919))
6. **ModuleAsTopLevelVal** (LOW, best practice)

---

## Rule 1: StartKoinInActivity

### Problem
**Issue:** [#1840](https://github.com/InsertKoinIO/koin/issues/1840)
Calling `startKoin {}` in Activity/Fragment instead of Application causes `KoinAppAlreadyStartedException` on configuration changes (rotation, theme change).

### Tasks

#### Task 1.1: Test
`src/test/kotlin/io/github/krozov/detekt/koin/platform/StartKoinInActivityTest.kt`:

```kotlin
@Test
fun `reports startKoin in Activity onCreate`() {
    val code = """
        import android.app.Activity
        import org.koin.core.context.startKoin

        class MainActivity : Activity() {
            override fun onCreate() {
                startKoin { }
            }
        }
    """.trimIndent()

    val findings = StartKoinInActivity(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}

@Test
fun `does not report startKoin in Application`() {
    val code = """
        import android.app.Application
        import org.koin.core.context.startKoin

        class MyApp : Application() {
            override fun onCreate() {
                startKoin { }
            }
        }
    """.trimIndent()

    val findings = StartKoinInActivity(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}
```

#### Task 1.2: Implementation
`src/main/kotlin/io/github/krozov/detekt/koin/platform/StartKoinInActivity.kt`:

```kotlin
internal class StartKoinInActivity(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "StartKoinInActivity",
        severity = Severity.Warning,
        description = "Detects startKoin() called in Activity/Fragment instead of Application",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.calleeExpression?.text != "startKoin") return

        // Find containing class
        val containingClass = expression.parents.filterIsInstance<KtClass>().firstOrNull() ?: return
        val superTypes = containingClass.superTypeListEntries.map { it.text }

        val isFrameworkEntry = superTypes.any {
            it.contains("Activity") || it.contains("Fragment") || it.contains("Composable")
        }

        val isApplication = superTypes.any { it.contains("Application") }

        if (isFrameworkEntry && !isApplication) {
            report(CodeSmell(issue, Entity.from(expression),
                """
                startKoin in Activity/Fragment → KoinAppAlreadyStartedException on config change
                → Call startKoin in Application.onCreate() instead

                ✗ Bad:  class MainActivity : Activity() { startKoin {} }
                ✓ Good: class MyApp : Application() { startKoin {} }
                """.trimIndent()))
        }
    }
}
```

#### Task 1.3: Register & document
Provider, docs/rules.md (Platform Rules section), README (38), CHANGELOG

---

## Rule 2: GetConcreteTypeInsteadOfInterface

### Problem
**Issue:** [#2222](https://github.com/InsertKoinIO/koin/issues/2222)
`Module.verify()` considers secondary types, but runtime doesn't. `verify()` passes but runtime fails.

### Tasks

#### Task 2.1: Test
`src/test/kotlin/io/github/krozov/detekt/koin/architecture/GetConcreteTypeInsteadOfInterfaceTest.kt`:

```kotlin
@Test
fun `reports get of concrete type when only interface registered`() {
    val code = """
        import org.koin.dsl.module

        interface Foo
        class FooImpl : Foo

        val m = module {
            single<Foo> { FooImpl() }
            single { Service(get<FooImpl>()) }  // ❌
        }
    """.trimIndent()

    val findings = GetConcreteTypeInsteadOfInterface(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}
```

#### Task 2.2: Implementation (Heuristic)
```kotlin
internal class GetConcreteTypeInsteadOfInterface(config: Config) : Rule(config) {
    // Detect: get<ConcreteType>() where only get<Interface>() is registered
    // Requires tracking registered types within module scope
    // Simplified heuristic implementation or mark as [NEEDS_RESEARCH]
}
```

If semantic analysis required, mark as `[NEEDS_RESEARCH]` and create issue.

---

## Rule 3: ExcessiveCreatedAtStart

### Problem
**Issue:** [#2266](https://github.com/InsertKoinIO/koin/issues/2266)
Many `createdAtStart = true` singletons cause ANR on Android startup.

### Tasks

#### Task 3.1: Test
```kotlin
@Test
fun `reports when more than 10 createdAtStart in one module`() {
    val code = """
        import org.koin.dsl.module

        val m = module {
            single(createdAtStart = true) { Service1() }
            single(createdAtStart = true) { Service2() }
            // ... 11 total
        }
    """.trimIndent()

    val findings = ExcessiveCreatedAtStart(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}
```

#### Task 3.2: Implementation
```kotlin
internal class ExcessiveCreatedAtStart(config: Config) : Rule(config) {

    private val maxCreatedAtStart: Int by config(10)
    private var countInModule = 0

    override fun visitCallExpression(expression: KtCallExpression) {
        if (expression.calleeExpression?.text == "module") {
            countInModule = 0
        }

        if (expression.text.contains("createdAtStart = true")) {
            countInModule++
        }

        if (countInModule > maxCreatedAtStart) {
            report(CodeSmell(issue, Entity.from(expression),
                "Excessive createdAtStart ($countInModule) → ANR risk"))
        }
    }
}
```

---

## Rule 4: ScopeDeclareWithActivityOrFragment

### Problem
**Issue:** [#1122](https://github.com/InsertKoinIO/koin/issues/1122)
`scope.declare(activity)` causes memory leak - declaration not cleared when scope closes.

### Tasks

#### Task 4.1: Test
```kotlin
@Test
fun `reports scope declare with activity`() {
    val code = """
        import org.koin.core.scope.Scope
        import android.app.Activity

        fun setupScope(scope: Scope, activity: Activity) {
            scope.declare(activity)
        }
    """.trimIndent()

    val findings = ScopeDeclareWithActivityOrFragment(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}
```

#### Task 4.2: Implementation
```kotlin
internal class ScopeDeclareWithActivityOrFragment(config: Config) : Rule(config) {

    override fun visitCallExpression(expression: KtCallExpression) {
        if (expression.calleeExpression?.text != "declare") return

        val argType = expression.valueArguments.firstOrNull()?.text ?: return
        if (argType.contains("activity", ignoreCase = true) ||
            argType.contains("fragment", ignoreCase = true)) {
            report(CodeSmell(issue, Entity.from(expression),
                "scope.declare(activity/fragment) → Memory leak"))
        }
    }
}
```

---

## Rule 5: OverrideInIncludedModule

### Problem
**Issue:** [#1919](https://github.com/InsertKoinIO/koin/issues/1919)
Overriding definitions from `includes()` modules doesn't work as expected.

### Tasks

#### Task 5.1: Test
```kotlin
@Test
fun `reports override attempt in same module with includes`() {
    val code = """
        import org.koin.dsl.module

        val baseModule = module {
            single { ServiceA() } bind Service::class
        }

        val overrideModule = module {
            includes(baseModule)
            single { ServiceB() } bind Service::class  // Won't override
        }
    """.trimIndent()

    val findings = OverrideInIncludedModule(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}
```

#### Task 5.2: Implementation (Complex - may need NEEDS_RESEARCH)
Requires tracking `includes()` and detecting duplicate bindings. Simplified heuristic or defer.

---

## Rule 6: ModuleAsTopLevelVal

### Problem
**Best Practice:** Module defined as `val m = module {}` causes factory preallocation. Use `fun m() = module {}` instead.

### Tasks

#### Task 6.1: Test
```kotlin
@Test
fun `reports module as top-level val`() {
    val code = """
        import org.koin.dsl.module

        val appModule = module { }
    """.trimIndent()

    val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}

@Test
fun `does not report module as function`() {
    val code = """
        import org.koin.dsl.module

        fun appModule() = module { }
    """.trimIndent()

    val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}
```

#### Task 6.2: Implementation
```kotlin
internal class ModuleAsTopLevelVal(config: Config) : Rule(config) {

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)

        // Check if top-level val
        if (!property.isTopLevel) return
        if (property.isVar) return

        // Check if initializer is module { }
        val initializer = property.initializer as? KtCallExpression ?: return
        if (initializer.calleeExpression?.text != "module") return

        report(CodeSmell(issue, Entity.from(property),
            """
            Module as top-level val → Factory preallocation issues
            → Use function instead: fun moduleName() = module {}
            """.trimIndent()))
    }
}
```

---

## Batch Registration & Documentation

After implementing all 6 rules:

### Task: Register all in KoinRuleSetProvider

```kotlin
import io.github.krozov.detekt.koin.platform.StartKoinInActivity
import io.github.krozov.detekt.koin.architecture.GetConcreteTypeInsteadOfInterface
import io.github.krozov.detekt.koin.moduledsl.ExcessiveCreatedAtStart
import io.github.krozov.detekt.koin.scope.ScopeDeclareWithActivityOrFragment
import io.github.krozov.detekt.koin.moduledsl.OverrideInIncludedModule
import io.github.krozov.detekt.koin.moduledsl.ModuleAsTopLevelVal

// In listOf():
StartKoinInActivity(config),
GetConcreteTypeInsteadOfInterface(config),  // or mark [NEEDS_RESEARCH]
ExcessiveCreatedAtStart(config),
ScopeDeclareWithActivityOrFragment(config),
OverrideInIncludedModule(config),  // or mark [NEEDS_RESEARCH]
ModuleAsTopLevelVal(config),
```

### Task: Update docs/rules.md

Add all 6 rules to appropriate sections (Platform, Module DSL, Scope, Architecture).

### Task: Update README and CHANGELOG

**README.md:** Final count = 43 rules (or less if some marked [NEEDS_RESEARCH])

**CHANGELOG.md:**
```markdown
### Added (Group 4: Startup & Miscellaneous)
- **StartKoinInActivity** - Detects startKoin in Activity/Fragment (#1840)
- **GetConcreteTypeInsteadOfInterface** - Detects verify() false positive (#2222) [MAY NEED RESEARCH]
- **ExcessiveCreatedAtStart** - Warns about ANR risk from eager initialization (#2266)
- **ScopeDeclareWithActivityOrFragment** - Detects memory leak pattern (#1122)
- **OverrideInIncludedModule** - Detects confusing override behavior (#1919) [MAY NEED RESEARCH]
- **ModuleAsTopLevelVal** - Suggests function instead of val for modules (best practice)
```

---

## Final Verification

```bash
./gradlew test
./gradlew koverVerify
./gradlew detekt
```

Final commit:
```bash
git commit --allow-empty -m "feat: complete Group 4 (Startup & Misc) - 6 rules

Implemented:
- StartKoinInActivity (MEDIUM, #1840)
- GetConcreteTypeInsteadOfInterface (MEDIUM, #2222) [RESEARCH NEEDED]
- ExcessiveCreatedAtStart (LOW, #2266)
- ScopeDeclareWithActivityOrFragment (LOW, #1122)
- OverrideInIncludedModule (LOW, #1919) [RESEARCH NEEDED]
- ModuleAsTopLevelVal (LOW, best practice)

Total: 6 rules (4 implemented, 2 may need research), 20+ tests"
```

---

## Success Criteria

- [x] 6 rules implemented (or marked [NEEDS_RESEARCH] with issues created)
- [x] 20+ tests
- [x] Coverage ≥96% (excluding research-needed rules)
- [x] Documentation complete
- [x] All registered in provider
- [x] Final rule count: 43 (or 41 if 2 deferred to research)

---

## Notes on Research-Needed Rules

If **GetConcreteTypeInsteadOfInterface** or **OverrideInIncludedModule** prove too complex for PSI-based analysis:

1. Mark section with `[NEEDS_RESEARCH]` heading
2. Document blocker (e.g., "Requires semantic type resolution")
3. Create GitHub issue with label `enhancement`, `research-needed`
4. Link issue in plan
5. Continue with remaining rules

These can be implemented later with enhanced tooling or accepted as limitations.
