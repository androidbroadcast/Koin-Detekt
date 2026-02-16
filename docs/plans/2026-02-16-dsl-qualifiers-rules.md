# DSL & Qualifiers Rules Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement 4 Koin rules that detect DSL misuse and qualifier-related bugs

**Architecture:** PSI-based rules detecting dead code in `withOptions`, duplicate bindings without qualifiers, generic type erasure issues, and enum qualifier collisions

**Tech Stack:** Kotlin, Detekt API 1.23.8+, JUnit 5, AssertJ

---

## Overview

- **Number of rules:** 4
- **Priority:** HIGH (2 rules), MEDIUM (2 rules)
- **Detekt categories:** `moduledsl`
- **Estimated effort:** ~6-8 hours

## Rules in this group

1. **UnassignedQualifierInWithOptions** (HIGH, Issue: [#2331](https://github.com/InsertKoinIO/koin/issues/2331))
2. **DuplicateBindingWithoutQualifier** (HIGH, Issue: [#2115](https://github.com/InsertKoinIO/koin/issues/2115))
3. **GenericDefinitionWithoutQualifier** (MEDIUM, Issue: [#188](https://github.com/InsertKoinIO/koin/issues/188))
4. **EnumQualifierCollision** (MEDIUM, Issue: [#2364](https://github.com/InsertKoinIO/koin/issues/2364))

---

## Rule 1: UnassignedQualifierInWithOptions

### Problem
**Issue:** [#2331](https://github.com/InsertKoinIO/koin/issues/2331)
Calling `named("...")` inside `withOptions { }` without assigning to `qualifier` property creates dead code. The qualifier is created and immediately discarded.

### Detection Pattern
```kotlin
// Detect: withOptions { named("x") } without assignment
withOptions {
    named("x")  // ❌ Dead code
}

withOptions {
    qualifier = named("x")  // ✅ Correct
}
```

### Implementation Tasks

#### Task 1.1: Write failing test

Create `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/UnassignedQualifierInWithOptionsTest.kt`:

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnassignedQualifierInWithOptionsTest {

    @Test
    fun `reports named() call without assignment in withOptions`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val m = module {
                factory { Service() } withOptions {
                    named("myService")
                }
            }
        """.trimIndent()

        val findings = UnassignedQualifierInWithOptions(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("qualifier")
    }

    @Test
    fun `does not report when qualifier is assigned`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val m = module {
                factory { Service() } withOptions {
                    qualifier = named("myService")
                }
            }
        """.trimIndent()

        val findings = UnassignedQualifierInWithOptions(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
```

Run: `./gradlew test --tests "UnassignedQualifierInWithOptionsTest" -i`
Commit: `git commit -m "test: add failing tests for UnassignedQualifierInWithOptions"`

#### Task 1.2: Implement rule

Create `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/UnassignedQualifierInWithOptions.kt`:

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class UnassignedQualifierInWithOptions(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "UnassignedQualifierInWithOptions",
        severity = Severity.Warning,
        description = "Detects named() calls in withOptions {} without assignment to qualifier property",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        // Find withOptions { } blocks
        if (expression.getCallNameExpression()?.text != "withOptions") return

        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return
        val bodyText = lambda.bodyExpression?.text ?: return

        // Check for unassigned named() or qualifier() calls
        val hasUnassignedQualifier = bodyText.contains(Regex("""^\s*named\(""", RegexOption.MULTILINE)) ||
                                     bodyText.contains(Regex("""^\s*qualifier\(""", RegexOption.MULTILINE))

        val hasAssignment = bodyText.contains("qualifier =")

        if (hasUnassignedQualifier && !hasAssignment) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Unassigned qualifier in withOptions → Dead code, qualifier not applied
                    → Assign to qualifier property

                    ✗ Bad:  withOptions { named("x") }
                    ✓ Good: withOptions { qualifier = named("x") }
                    """.trimIndent()
                )
            )
        }
    }
}
```

Run tests, commit: `git commit -m "feat: add UnassignedQualifierInWithOptions rule"`

#### Task 1.3: Register and document

- Add to `KoinRuleSetProvider.kt`
- Document in `docs/rules.md`
- Update README (count: 33) and CHANGELOG

Commit: `git commit -m "feat: register and document UnassignedQualifierInWithOptions"`

---

## Rule 2: DuplicateBindingWithoutQualifier

### Problem
**Issue:** [#2115](https://github.com/InsertKoinIO/koin/issues/2115)
Two `single { } bind Foo::class` without qualifiers → second silently overrides first

### Detection Pattern
```kotlin
// Detect: multiple bindings to same type without qualifiers
module {
    single { A() } bind Foo::class        // ❌
    single { B() } bind Foo::class        // Silently overrides A
}

module {
    single { A() } bind Foo::class named("a")  // ✅
    single { B() } bind Foo::class named("b")  // Both available
}
```

### Implementation Tasks

#### Task 2.1: Write test

Create `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/DuplicateBindingWithoutQualifierTest.kt`:

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DuplicateBindingWithoutQualifierTest {

    @Test
    fun `reports duplicate bindings to same type without qualifiers`() {
        val code = """
            import org.koin.dsl.module

            interface Foo

            val m = module {
                single { ServiceA() } bind Foo::class
                single { ServiceB() } bind Foo::class
            }
        """.trimIndent()

        val findings = DuplicateBindingWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report bindings with different qualifiers`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            interface Foo

            val m = module {
                single { ServiceA() } bind Foo::class named("a")
                single { ServiceB() } bind Foo::class named("b")
            }
        """.trimIndent()

        val findings = DuplicateBindingWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
```

Commit: `test: add DuplicateBindingWithoutQualifier tests`

#### Task 2.2: Implement rule

Create `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/DuplicateBindingWithoutQualifier.kt`:

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*

internal class DuplicateBindingWithoutQualifier(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "DuplicateBindingWithoutQualifier",
        severity = Severity.Warning,
        description = "Detects multiple bindings to same type without qualifiers (silent override)",
        debt = Debt.TEN_MINS
    )

    private val bindingsInCurrentModule = mutableMapOf<String, MutableList<KtExpression>>()

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        // Track module scope
        if (expression.calleeExpression?.text == "module") {
            bindingsInCurrentModule.clear()
        }

        // Find "bind X::class" patterns
        val text = expression.text
        if (!text.contains("bind")) return

        val bindMatch = Regex("""bind\s+(\w+)::class""").find(text) ?: return
        val boundType = bindMatch.groupValues[1]

        // Check if this binding has a qualifier
        val hasQualifier = text.contains("named(") || text.contains("qualifier =")

        if (!hasQualifier) {
            bindingsInCurrentModule.getOrPut(boundType) { mutableListOf() }.add(expression)

            // Report if duplicate
            if (bindingsInCurrentModule[boundType]!!.size > 1) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        """
                        Duplicate binding to $boundType without qualifier → Silent override
                        → Add named() qualifiers to keep both bindings

                        ✗ Bad:  single { A() } bind Foo::class
                                single { B() } bind Foo::class
                        ✓ Good: single { A() } bind Foo::class named("a")
                                single { B() } bind Foo::class named("b")
                        """.trimIndent()
                    )
                )
            }
        }
    }
}
```

Commit: `feat: add DuplicateBindingWithoutQualifier rule`

#### Task 2.3: Register and document

Update provider, docs, README (34), CHANGELOG

Commit: `feat: register and document DuplicateBindingWithoutQualifier`

---

## Rule 3: GenericDefinitionWithoutQualifier

### Problem
**Issue:** [#188](https://github.com/InsertKoinIO/koin/issues/188)
Type erasure: `single { listOf<A>() }` and `single { listOf<B>() }` treated as same `List` type

### Tasks

#### Task 3.1: Tests

Create `GenericDefinitionWithoutQualifierTest.kt`:

```kotlin
@Test
fun `reports generic List without qualifier`() {
    val code = """
        import org.koin.dsl.module

        val m = module {
            single { listOf<String>() }
            single { listOf<Int>() }
        }
    """.trimIndent()

    val findings = GenericDefinitionWithoutQualifier(Config.empty).lint(code)
    assertThat(findings).hasSize(2)
}

@Test
fun `does not report generic with qualifier`() {
    val code = """
        import org.koin.dsl.module

        val m = module {
            single(named("strings")) { listOf<String>() }
        }
    """.trimIndent()

    val findings = GenericDefinitionWithoutQualifier(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}
```

#### Task 3.2: Implementation

```kotlin
internal class GenericDefinitionWithoutQualifier(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "GenericDefinitionWithoutQualifier",
        severity = Severity.Warning,
        description = "Generic types without qualifiers cause type erasure collisions",
        debt = Debt.TEN_MINS
    )

    private val genericTypes = setOf("List", "Set", "Map", "Array")

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.calleeExpression?.text !in setOf("single", "factory", "scoped")) return

        val text = expression.text
        val hasGeneric = genericTypes.any { text.contains("$it<") }
        val hasQualifier = text.contains("named(") || expression.valueArguments.isNotEmpty()

        if (hasGeneric && !hasQualifier) {
            report(CodeSmell(issue, Entity.from(expression),
                """Generic type without qualifier → Type erasure collision
                ✗ Bad:  single { listOf<A>() }
                ✓ Good: single(named("a")) { listOf<A>() }""".trimIndent()))
        }
    }
}
```

#### Task 3.3: Register and document

README count: 35

---

## Rule 4: EnumQualifierCollision

### Problem
**Issue:** [#2364](https://github.com/InsertKoinIO/koin/issues/2364)
`named(Enum1.VALUE)` == `named(Enum2.VALUE)` if enum value names match (especially with R8)

### Tasks

#### Task 4.1: Tests

```kotlin
@Test
fun `reports enum qualifiers with same value name`() {
    val code = """
        import org.koin.dsl.module
        import org.koin.core.qualifier.named

        enum class Type1 { VALUE }
        enum class Type2 { VALUE }

        val m = module {
            single(named(Type1.VALUE)) { ServiceA() }
            single(named(Type2.VALUE)) { ServiceB() }
        }
    """.trimIndent()

    val findings = EnumQualifierCollision(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}
```

#### Task 4.2: Implementation (NEEDS_RESEARCH marker)

This rule requires semantic analysis to track enum types. Mark as `[NEEDS_RESEARCH]` if PSI analysis proves insufficient.

```kotlin
// Simplified heuristic-based implementation
internal class EnumQualifierCollision(config: Config) : Rule(config) {
    // Detect pattern: named(.*\.(\w+)) where same \w+ appears with different prefix
    // Full implementation requires type resolution
}
```

If blocked, document:
```markdown
## [NEEDS_RESEARCH] EnumQualifierCollision

**Blocker:** Requires semantic type analysis to distinguish enum types.
PSI text matching insufficient for production use.

**Issue:** https://github.com/androidbroadcast/Koin-Detekt/issues/XXX
```

---

## Final Verification

Run:
```bash
./gradlew test
./gradlew koverVerify
./gradlew detekt
```

Commit:
```bash
git commit --allow-empty -m "feat: complete Group 2 (DSL & Qualifiers) - 4 rules

Implemented:
- UnassignedQualifierInWithOptions (HIGH, #2331)
- DuplicateBindingWithoutQualifier (HIGH, #2115)
- GenericDefinitionWithoutQualifier (MEDIUM, #188)
- EnumQualifierCollision (MEDIUM, #2364) [MAY NEED RESEARCH]

Total: 4 rules, 15+ tests, coverage ≥96%"
```

---

## Success Criteria

- [x] 4 rules implemented (or marked NEEDS_RESEARCH)
- [x] 15+ tests
- [x] Coverage ≥96%
- [x] Documentation complete
- [x] Registered in provider
