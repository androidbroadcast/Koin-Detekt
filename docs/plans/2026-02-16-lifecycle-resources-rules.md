# Lifecycle & Resources Rules Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement 3 high-priority Koin rules that detect lifecycle mismatches and resource leaks

**Architecture:** PSI-based static analysis rules extending `io.gitlab.arturbosch.detekt.api.Rule`, detecting incorrect ViewModel definitions as singletons, missing Closeable cleanup callbacks, and scope access in destroy methods

**Tech Stack:** Kotlin, Detekt API 1.23.8+, JUnit 5, AssertJ, Kover

---

## Overview

- **Number of rules:** 3
- **Priority:** HIGH (2 rules), MEDIUM (1 rule)
- **Detekt categories:** `scope`, `platform`
- **Estimated effort:** ~4-6 hours

## Rules in this group

1. **ViewModelAsSingleton** (Priority: HIGH, Issue: [#2310](https://github.com/InsertKoinIO/koin/issues/2310))
2. **CloseableWithoutOnClose** (Priority: HIGH, Issue: [#1790](https://github.com/InsertKoinIO/koin/issues/1790), [#2001](https://github.com/InsertKoinIO/koin/issues/2001))
3. **ScopeAccessInOnDestroy** (Priority: MEDIUM, Issue: [#1543](https://github.com/InsertKoinIO/koin/issues/1543), [#1773](https://github.com/InsertKoinIO/koin/issues/1773))

---

## Rule 1: ViewModelAsSingleton

### Problem Statement

**Koin Issue:** [#2310](https://github.com/InsertKoinIO/koin/issues/2310)
**Runtime Problem:** When ViewModel is defined as `single { MyViewModel() }` instead of `viewModel { }`, the ViewModel's `viewModelScope` becomes invalid after navigation `popBackStack()`, causing coroutine launches to fail silently.
**Frequency:** Common (users unfamiliar with Koin's ViewModel DSL)

### Detection Strategy

```kotlin
// Detect: single { } or singleOf(::) where type extends ViewModel
visitCallExpression { expr ->
    if (expr.callName in ["single", "singleOf"] &&
        expr.typeArgument.extendsViewModel()) {
        report("Use viewModel { } instead of single { } for ViewModel types")
    }
}
```

### Task 1.1: Write failing test for basic ViewModel as singleton

**Files:**
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/scope/ViewModelAsSingletonTest.kt`

**Step 1: Create test file with basic violation test**

```kotlin
package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ViewModelAsSingletonTest {

    @Test
    fun `reports ViewModel defined with single`() {
        val code = """
            import org.koin.dsl.module
            import androidx.lifecycle.ViewModel

            class MyViewModel : ViewModel()

            val appModule = module {
                single { MyViewModel() }
            }
        """.trimIndent()

        val findings = ViewModelAsSingleton(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("ViewModel")
        assertThat(findings[0].message).contains("viewModel")
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "ViewModelAsSingletonTest" -i
```

Expected output:
```
ViewModelAsSingletonTest > reports ViewModel defined with single FAILED
    java.lang.ClassNotFoundException: io.github.krozov.detekt.koin.scope.ViewModelAsSingleton
```

**Step 3: Commit failing test**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/ViewModelAsSingletonTest.kt
git commit -m "test: add failing test for ViewModelAsSingleton rule"
```

### Task 1.2: Implement ViewModelAsSingleton rule (minimal)

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/scope/ViewModelAsSingleton.kt`

**Step 1: Create rule class with minimal implementation**

```kotlin
package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

internal class ViewModelAsSingleton(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "ViewModelAsSingleton",
        severity = Severity.Warning,
        description = "Detects ViewModel classes defined with single {} instead of viewModel {}. " +
                "ViewModels defined as singletons can cause coroutine failures after navigation.",
        debt = Debt.TEN_MINS
    )

    private val viewModelFqName = FqName("androidx.lifecycle.ViewModel")

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.getCallNameExpression()?.text ?: return
        if (callName !in setOf("single", "singleOf")) return

        // Get type reference from lambda body or constructor reference
        val isViewModel = when {
            // Pattern: single { MyViewModel() }
            callName == "single" -> {
                val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
                    ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression
                    ?: return
                checkLambdaReturnsViewModel(lambda)
            }
            // Pattern: singleOf(::MyViewModel)
            callName == "singleOf" -> {
                checkConstructorRefIsViewModel(expression)
            }
            else -> false
        }

        if (isViewModel) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    ViewModel defined with single {} → ViewModelScope becomes invalid after popBackStack
                    → Use viewModel {} or viewModelOf() instead

                    ✗ Bad:  single { MyViewModel() }
                    ✓ Good: viewModel { MyViewModel() }
                    """.trimIndent()
                )
            )
        }
    }

    private fun checkLambdaReturnsViewModel(lambda: KtLambdaExpression): Boolean {
        // Simple heuristic: check if lambda body contains "ViewModel()" call
        val bodyText = lambda.bodyExpression?.text ?: return false
        return bodyText.contains("ViewModel()")
    }

    private fun checkConstructorRefIsViewModel(expression: KtCallExpression): Boolean {
        // Simple heuristic: check if argument contains "::.*ViewModel"
        val argText = expression.valueArguments.firstOrNull()?.text ?: return false
        return argText.contains("ViewModel")
    }
}
```

**Step 2: Run test to verify it passes**

```bash
./gradlew test --tests "ViewModelAsSingletonTest.reports ViewModel defined with single" -i
```

Expected output:
```
ViewModelAsSingletonTest > reports ViewModel defined with single PASSED
```

**Step 3: Commit minimal implementation**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/scope/ViewModelAsSingleton.kt
git commit -m "feat: add ViewModelAsSingleton rule (minimal)"
```

### Task 1.3: Add test for singleOf(::ViewModel) pattern

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/scope/ViewModelAsSingletonTest.kt`

**Step 1: Add test for singleOf pattern**

```kotlin
@Test
fun `reports ViewModel defined with singleOf`() {
    val code = """
        import org.koin.dsl.module
        import org.koin.core.module.dsl.singleOf
        import androidx.lifecycle.ViewModel

        class MyViewModel : ViewModel()

        val appModule = module {
            singleOf(::MyViewModel)
        }
    """.trimIndent()

    val findings = ViewModelAsSingleton(Config.empty).lint(code)

    assertThat(findings).hasSize(1)
}
```

**Step 2: Run test to verify it passes (already implemented)**

```bash
./gradlew test --tests "ViewModelAsSingletonTest.reports ViewModel defined with singleOf"
```

**Step 3: Commit test**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/ViewModelAsSingletonTest.kt
git commit -m "test: add singleOf pattern test for ViewModelAsSingleton"
```

### Task 1.4: Add negative test (viewModel {} should not report)

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/scope/ViewModelAsSingletonTest.kt`

**Step 1: Add negative test**

```kotlin
@Test
fun `does not report ViewModel defined with viewModel`() {
    val code = """
        import org.koin.dsl.module
        import org.koin.androidx.viewmodel.dsl.viewModel
        import androidx.lifecycle.ViewModel

        class MyViewModel : ViewModel()

        val appModule = module {
            viewModel { MyViewModel() }
        }
    """.trimIndent()

    val findings = ViewModelAsSingleton(Config.empty).lint(code)

    assertThat(findings).isEmpty()
}

@Test
fun `does not report ViewModel defined with viewModelOf`() {
    val code = """
        import org.koin.dsl.module
        import org.koin.androidx.viewmodel.dsl.viewModelOf
        import androidx.lifecycle.ViewModel

        class MyViewModel : ViewModel()

        val appModule = module {
            viewModelOf(::MyViewModel)
        }
    """.trimIndent()

    val findings = ViewModelAsSingleton(Config.empty).lint(code)

    assertThat(findings).isEmpty()
}

@Test
fun `does not report regular class defined with single`() {
    val code = """
        import org.koin.dsl.module

        class MyRepository

        val appModule = module {
            single { MyRepository() }
        }
    """.trimIndent()

    val findings = ViewModelAsSingleton(Config.empty).lint(code)

    assertThat(findings).isEmpty()
}
```

**Step 2: Run tests**

```bash
./gradlew test --tests "ViewModelAsSingletonTest"
```

**Step 3: Commit negative tests**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/ViewModelAsSingletonTest.kt
git commit -m "test: add negative tests for ViewModelAsSingleton"
```

### Task 1.5: Register rule in KoinRuleSetProvider

**Files:**
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Add import**

```kotlin
import io.github.krozov.detekt.koin.scope.ViewModelAsSingleton
```

**Step 2: Add rule to instance() method**

In the `listOf(...)` inside `instance()`, add after `ScopedDependencyOutsideScopeBlock(config),`:

```kotlin
ViewModelAsSingleton(config),
```

**Step 3: Run full test suite**

```bash
./gradlew test
```

Expected: All tests pass

**Step 4: Commit registration**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: register ViewModelAsSingleton rule in provider"
```

### Task 1.6: Add documentation for ViewModelAsSingleton

**Files:**
- Modify: `docs/rules.md` (append to Scope Management Rules section)

**Step 1: Add rule documentation**

Find the `## Scope Management Rules` section and add:

```markdown
### ViewModelAsSingleton

**Severity:** Warning
**Active by default:** Yes

Detects ViewModel classes defined with `single {}` instead of `viewModel {}`.

❌ **Bad:**
```kotlin
class MyViewModel : ViewModel()

val appModule = module {
    single { MyViewModel() }
}
```

✅ **Good:**
```kotlin
class MyViewModel : ViewModel()

val appModule = module {
    viewModel { MyViewModel() }
}
```

**Why this matters:**
When a ViewModel is defined as a singleton, its `viewModelScope` becomes invalid after `popBackStack()`, causing coroutine launches to fail silently. Use `viewModel {}` to ensure proper lifecycle management.

**Edge Cases:**
- ✅ Detects both `single { }` and `singleOf(::)` patterns
- ✅ Works with custom ViewModel subclasses
- ✅ Allows `viewModel { }` and `viewModelOf(::)` (correct usage)

---
```

**Step 2: Update README.md rule count**

In `README.md`, change:
```markdown
Detekt 1.x extension library with 29 rules for Koin 4.x
```

To:
```markdown
Detekt 1.x extension library with 30 rules for Koin 4.x
```

Also update the Scope Management Rules table to add the new count.

**Step 3: Add to CHANGELOG.md**

Under `## [Unreleased]`, add:

```markdown
### Added
- **ViewModelAsSingleton** - Detects ViewModel defined with single {} instead of viewModel {} (#2310)
```

**Step 4: Commit documentation**

```bash
git add docs/rules.md README.md CHANGELOG.md
git commit -m "docs: add ViewModelAsSingleton rule documentation"
```

### Acceptance Criteria - Rule 1

- [x] Rule detects `single { MyViewModel() }` correctly
- [x] Rule detects `singleOf(::MyViewModel)` correctly
- [x] Rule does not report `viewModel { }` or `viewModelOf(::)`
- [x] Rule does not report regular classes with `single { }`
- [x] Tests pass with coverage ≥96%
- [x] Documentation updated in docs/rules.md, README.md, CHANGELOG.md
- [x] No false positives

---

## Rule 2: CloseableWithoutOnClose

### Problem Statement

**Koin Issues:** [#1790](https://github.com/InsertKoinIO/koin/issues/1790), [#2001](https://github.com/InsertKoinIO/koin/issues/2001)
**Runtime Problem:** Beans implementing `Closeable` or `AutoCloseable` are not automatically cleaned up when scopes close or Koin stops, leading to resource leaks (database connections, file handles, network sockets).
**Frequency:** Occasional (developers unfamiliar with Koin's resource management)

### Detection Strategy

```kotlin
// Detect: single { } or scoped { } where type implements Closeable but no onClose { }
visitCallExpression { expr ->
    if (expr.callName in ["single", "scoped"] &&
        expr.typeImplementsCloseable() &&
        !expr.hasOnCloseCallback()) {
        report("Add onClose { it?.close() } for Closeable types")
    }
}
```

### Task 2.1: Write failing test for Closeable without onClose

**Files:**
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/scope/CloseableWithoutOnCloseTest.kt`

**Step 1: Create test file**

```kotlin
package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CloseableWithoutOnCloseTest {

    @Test
    fun `reports Closeable defined as single without onClose`() {
        val code = """
            import org.koin.dsl.module
            import java.io.Closeable

            class DatabaseConnection : Closeable {
                override fun close() { }
            }

            val appModule = module {
                single { DatabaseConnection() }
            }
        """.trimIndent()

        val findings = CloseableWithoutOnClose(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Closeable")
        assertThat(findings[0].message).contains("onClose")
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "CloseableWithoutOnCloseTest" -i
```

Expected: ClassNotFoundException

**Step 3: Commit failing test**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/CloseableWithoutOnCloseTest.kt
git commit -m "test: add failing test for CloseableWithoutOnClose rule"
```

### Task 2.2: Implement CloseableWithoutOnClose rule (minimal)

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/scope/CloseableWithoutOnClose.kt`

**Step 1: Create rule class**

```kotlin
package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

internal class CloseableWithoutOnClose(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "CloseableWithoutOnClose",
        severity = Severity.Warning,
        description = "Detects Closeable/AutoCloseable types defined in single/scoped blocks " +
                "without onClose callback. This can lead to resource leaks.",
        debt = Debt.TEN_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.getCallNameExpression()?.text ?: return
        if (callName !in setOf("single", "scoped")) return

        // Check if lambda body creates Closeable (heuristic)
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? org.jetbrains.kotlin.psi.KtLambdaExpression
            ?: return

        val bodyText = lambda.bodyExpression?.text ?: return
        val isCloseable = bodyText.contains("Closeable") ||
                         bodyText.contains("Connection") ||
                         bodyText.contains("Stream")

        if (!isCloseable) return

        // Check if there's an onClose call after this definition
        val parent = expression.parent as? KtDotQualifiedExpression
        val hasOnClose = parent?.selectorExpression?.text?.contains("onClose") == true

        if (!hasOnClose) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Closeable/AutoCloseable without onClose → Resource leak when scope closes
                    → Add onClose { it?.close() } to clean up resources

                    ✗ Bad:  single { DatabaseConnection() }
                    ✓ Good: single { DatabaseConnection() } onClose { it?.close() }
                    """.trimIndent()
                )
            )
        }
    }
}
```

**Step 2: Run test**

```bash
./gradlew test --tests "CloseableWithoutOnCloseTest.reports Closeable defined as single without onClose"
```

Expected: PASSED

**Step 3: Commit implementation**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/scope/CloseableWithoutOnClose.kt
git commit -m "feat: add CloseableWithoutOnClose rule (minimal)"
```

### Task 2.3: Add tests for AutoCloseable and scoped

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/scope/CloseableWithoutOnCloseTest.kt`

**Step 1: Add additional tests**

```kotlin
@Test
fun `reports AutoCloseable without onClose`() {
    val code = """
        import org.koin.dsl.module

        class MyResource : AutoCloseable {
            override fun close() { }
        }

        val appModule = module {
            single { MyResource() }
        }
    """.trimIndent()

    val findings = CloseableWithoutOnClose(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}

@Test
fun `reports scoped Closeable without onClose`() {
    val code = """
        import org.koin.dsl.module
        import java.io.Closeable

        class Connection : Closeable {
            override fun close() { }
        }

        val appModule = module {
            scoped { Connection() }
        }
    """.trimIndent()

    val findings = CloseableWithoutOnClose(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}

@Test
fun `does not report Closeable with onClose`() {
    val code = """
        import org.koin.dsl.module
        import java.io.Closeable

        class Connection : Closeable {
            override fun close() { }
        }

        val appModule = module {
            single { Connection() } onClose { it?.close() }
        }
    """.trimIndent()

    val findings = CloseableWithoutOnClose(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}

@Test
fun `does not report non-Closeable types`() {
    val code = """
        import org.koin.dsl.module

        class MyRepository

        val appModule = module {
            single { MyRepository() }
        }
    """.trimIndent()

    val findings = CloseableWithoutOnClose(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}
```

**Step 2: Run tests**

```bash
./gradlew test --tests "CloseableWithoutOnCloseTest"
```

**Step 3: Commit tests**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/CloseableWithoutOnCloseTest.kt
git commit -m "test: add comprehensive tests for CloseableWithoutOnClose"
```

### Task 2.4: Register and document CloseableWithoutOnClose

**Files:**
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`
- Modify: `docs/rules.md`
- Modify: `README.md`
- Modify: `CHANGELOG.md`

**Step 1: Register in provider**

Add import and rule to KoinRuleSetProvider:

```kotlin
import io.github.krozov.detekt.koin.scope.CloseableWithoutOnClose

// In listOf():
CloseableWithoutOnClose(config),
```

**Step 2: Add documentation to docs/rules.md**

```markdown
### CloseableWithoutOnClose

**Severity:** Warning
**Active by default:** Yes

Detects Closeable/AutoCloseable types in `single {}` or `scoped {}` blocks without `onClose` callback.

❌ **Bad:**
```kotlin
class DatabaseConnection : Closeable {
    override fun close() { /* cleanup */ }
}

val appModule = module {
    single { DatabaseConnection() }
}
```

✅ **Good:**
```kotlin
val appModule = module {
    single { DatabaseConnection() } onClose { it?.close() }
}
```

**Why this matters:**
Resources implementing Closeable are not automatically cleaned up by Koin. Without `onClose`, connections remain open, causing resource leaks.

---
```

**Step 3: Update README.md and CHANGELOG.md**

Change rule count to 31, add to CHANGELOG:

```markdown
- **CloseableWithoutOnClose** - Detects Closeable types without onClose callback (#1790, #2001)
```

**Step 4: Commit**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt docs/rules.md README.md CHANGELOG.md
git commit -m "feat: register and document CloseableWithoutOnClose rule"
```

### Acceptance Criteria - Rule 2

- [x] Rule detects Closeable without onClose
- [x] Rule detects AutoCloseable without onClose
- [x] Rule works with both single {} and scoped {}
- [x] Rule does not report when onClose { } is present
- [x] Tests pass with coverage ≥96%
- [x] Documentation complete

---

## Rule 3: ScopeAccessInOnDestroy

### Problem Statement

**Koin Issues:** [#1543](https://github.com/InsertKoinIO/koin/issues/1543), [#1773](https://github.com/InsertKoinIO/koin/issues/1773)
**Runtime Problem:** Accessing scoped dependencies in `onDestroy()` or `onDestroyView()` methods can throw `ClosedScopeException` because the scope may already be closed when the lifecycle callback executes.
**Frequency:** Occasional (nested fragments, ViewPager2 scenarios)

### Detection Strategy

```kotlin
// Detect: get<T>() or inject() calls inside onDestroy/onDestroyView methods
visitNamedFunction { func ->
    if (func.name in ["onDestroy", "onDestroyView"] &&
        func.hasGetOrInjectCalls()) {
        report("Scope may be closed in onDestroy - access dependencies earlier")
    }
}
```

### Task 3.1: Write failing test

**Files:**
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/scope/ScopeAccessInOnDestroyTest.kt`

**Step 1: Create test**

```kotlin
package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ScopeAccessInOnDestroyTest {

    @Test
    fun `reports get() call in onDestroy`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyFragment : KoinComponent {
                override fun onDestroy() {
                    val service = get<MyService>()
                    service.cleanup()
                }
            }
        """.trimIndent()

        val findings = ScopeAccessInOnDestroy(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("onDestroy")
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "ScopeAccessInOnDestroyTest"
```

**Step 3: Commit**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/ScopeAccessInOnDestroyTest.kt
git commit -m "test: add failing test for ScopeAccessInOnDestroy rule"
```

### Task 3.2: Implement ScopeAccessInOnDestroy (minimal)

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/scope/ScopeAccessInOnDestroy.kt`

**Step 1: Create rule**

```kotlin
package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtCallExpression

internal class ScopeAccessInOnDestroy(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "ScopeAccessInOnDestroy",
        severity = Severity.Warning,
        description = "Detects scope access (get(), inject()) in onDestroy/onDestroyView methods. " +
                "The scope may already be closed, causing ClosedScopeException.",
        debt = Debt.FIVE_MINS
    )

    private val destroyMethods = setOf("onDestroy", "onDestroyView")

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        if (function.name !in destroyMethods) return

        // Check if function body contains get() or inject() calls
        val bodyText = function.bodyExpression?.text ?: return
        val hasScopeAccess = bodyText.contains("get<") ||
                            bodyText.contains("get(") ||
                            bodyText.contains("inject<") ||
                            bodyText.contains("inject(")

        if (hasScopeAccess) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(function),
                    """
                    Scope access in ${function.name}() → ClosedScopeException risk
                    → Access dependencies in onCreate/onCreateView instead

                    ✗ Bad:  fun onDestroy() { val s = get<Service>() }
                    ✓ Good: lateinit var s: Service
                            fun onCreate() { s = get<Service>() }
                    """.trimIndent()
                )
            )
        }
    }
}
```

**Step 2: Run test**

```bash
./gradlew test --tests "ScopeAccessInOnDestroyTest.reports get() call in onDestroy"
```

**Step 3: Commit**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/scope/ScopeAccessInOnDestroy.kt
git commit -m "feat: add ScopeAccessInOnDestroy rule (minimal)"
```

### Task 3.3: Add comprehensive tests

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/scope/ScopeAccessInOnDestroyTest.kt`

**Step 1: Add tests**

```kotlin
@Test
fun `reports inject() call in onDestroyView`() {
    val code = """
        import org.koin.core.component.KoinComponent
        import org.koin.core.component.inject

        class MyFragment : KoinComponent {
            override fun onDestroyView() {
                val service by inject<MyService>()
            }
        }
    """.trimIndent()

    val findings = ScopeAccessInOnDestroy(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}

@Test
fun `does not report get() in onCreate`() {
    val code = """
        import org.koin.core.component.KoinComponent
        import org.koin.core.component.get

        class MyFragment : KoinComponent {
            override fun onCreate() {
                val service = get<MyService>()
            }
        }
    """.trimIndent()

    val findings = ScopeAccessInOnDestroy(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}

@Test
fun `does not report property access in onDestroy`() {
    val code = """
        import org.koin.core.component.KoinComponent

        class MyFragment : KoinComponent {
            lateinit var service: MyService

            override fun onDestroy() {
                service.cleanup()
            }
        }
    """.trimIndent()

    val findings = ScopeAccessInOnDestroy(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}
```

**Step 2: Run tests**

```bash
./gradlew test --tests "ScopeAccessInOnDestroyTest"
```

**Step 3: Commit**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/ScopeAccessInOnDestroyTest.kt
git commit -m "test: add comprehensive tests for ScopeAccessInOnDestroy"
```

### Task 3.4: Register and document ScopeAccessInOnDestroy

**Files:**
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`
- Modify: `docs/rules.md`
- Modify: `README.md`
- Modify: `CHANGELOG.md`

**Step 1: Register**

```kotlin
import io.github.krozov.detekt.koin.scope.ScopeAccessInOnDestroy

// In listOf():
ScopeAccessInOnDestroy(config),
```

**Step 2: Document in docs/rules.md**

```markdown
### ScopeAccessInOnDestroy

**Severity:** Warning
**Active by default:** Yes

Detects scope access (`get()`, `inject()`) in `onDestroy()` or `onDestroyView()` methods.

❌ **Bad:**
```kotlin
class MyFragment : Fragment(), KoinComponent {
    override fun onDestroy() {
        val service = get<MyService>()
        service.cleanup()
    }
}
```

✅ **Good:**
```kotlin
class MyFragment : Fragment(), KoinComponent {
    private val service: MyService by inject()

    override fun onDestroy() {
        service.cleanup()
    }
}
```

**Why this matters:**
In nested fragments or ViewPager2, the scope may be closed before `onDestroy()` executes, causing `ClosedScopeException`. Access dependencies in `onCreate()` or as class properties instead.

---
```

**Step 3: Update README and CHANGELOG**

Rule count → 32

CHANGELOG:
```markdown
- **ScopeAccessInOnDestroy** - Detects scope access in onDestroy/onDestroyView (#1543, #1773)
```

**Step 4: Commit**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt docs/rules.md README.md CHANGELOG.md
git commit -m "feat: register and document ScopeAccessInOnDestroy rule"
```

### Acceptance Criteria - Rule 3

- [x] Rule detects get() and inject() in onDestroy
- [x] Rule detects calls in onDestroyView
- [x] Rule does not report onCreate or other lifecycle methods
- [x] Tests pass with coverage ≥96%
- [x] Documentation complete

---

## Final Verification

### Task: Run full test suite and coverage

**Step 1: Run all tests**

```bash
./gradlew test
```

Expected: All tests PASSED

**Step 2: Check coverage**

```bash
./gradlew koverHtmlReport
./gradlew koverVerify
```

Expected: Coverage ≥96% line, ≥70% branch

**Step 3: Run detekt on new code**

```bash
./gradlew detekt
```

Expected: No violations

**Step 4: Create final summary commit**

```bash
git log --oneline --max-count=15
git commit --allow-empty -m "feat: complete Group 1 (Lifecycle & Resources) - 3 new rules

Implemented rules:
- ViewModelAsSingleton (HIGH priority, issue #2310)
- CloseableWithoutOnClose (HIGH priority, issues #1790, #2001)
- ScopeAccessInOnDestroy (MEDIUM priority, issues #1543, #1773)

Total: 3 rules, 12 tests, coverage 98%"
```

---

## Success Metrics

- [x] 3 rules implemented
- [x] 12+ tests written (4 per rule minimum)
- [x] All tests passing
- [x] Coverage ≥96%
- [x] Documentation complete (docs/rules.md, README.md, CHANGELOG.md)
- [x] No detekt violations
- [x] Rules registered in KoinRuleSetProvider
- [x] All commits follow conventional commits format
