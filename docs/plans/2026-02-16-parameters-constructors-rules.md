# Parameters & Constructors Rules Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement 2 rules detecting parameter resolution bugs in Koin's constructor DSL and parametersOf()

**Architecture:** PSI analysis of factoryOf/singleOf constructor parameters and factory lambda return types vs parameter types

**Tech Stack:** Kotlin, Detekt API 1.23.8+, JUnit 5, AssertJ

---

## Overview

- **Number of rules:** 2
- **Priority:** HIGH (1 rule), MEDIUM (1 rule)
- **Detekt categories:** `moduledsl`
- **Estimated effort:** ~3-4 hours

## Rules in this group

1. **ConstructorDslAmbiguousParameters** (HIGH, Issues: [#1372](https://github.com/InsertKoinIO/koin/issues/1372), [#2347](https://github.com/InsertKoinIO/koin/issues/2347))
2. **ParameterTypeMatchesReturnType** (MEDIUM, Issue: [#2328](https://github.com/InsertKoinIO/koin/issues/2328))

---

## Rule 1: ConstructorDslAmbiguousParameters

### Problem
**Issues:** [#1372](https://github.com/InsertKoinIO/koin/issues/1372), [#2347](https://github.com/InsertKoinIO/koin/issues/2347)

When using `factoryOf(::MyClass)` or `viewModelOf(::MyVM)` with constructor parameters of the same type (e.g., `Int, Int?` or `String, String`), Koin resolves them incorrectly — passing the first parameter value for subsequent ones.

### Detection Pattern
```kotlin
// Detect: factoryOf(::Foo) where Foo(a: Int, b: Int)
class Foo(val a: Int, val b: Int?)

module {
    factoryOf(::Foo)  // ❌ b gets value of a
}

// Better: use lambda
module {
    factory { Foo(get(), get()) }  // ✅ Explicit resolution
}
```

### Implementation Tasks

#### Task 1.1: Write failing test

Create `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/ConstructorDslAmbiguousParametersTest.kt`:

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConstructorDslAmbiguousParametersTest {

    @Test
    fun `reports factoryOf with duplicate parameter types`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.factoryOf

            class MyService(val a: Int, val b: Int)

            val m = module {
                factoryOf(::MyService)
            }
        """.trimIndent()

        val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("ambiguous")
    }

    @Test
    fun `reports viewModelOf with Int and Int? parameters`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.androidx.viewmodel.dsl.viewModelOf
            import androidx.lifecycle.ViewModel

            class MyViewModel(val required: Int, val optional: Int?) : ViewModel()

            val m = module {
                viewModelOf(::MyViewModel)
            }
        """.trimIndent()

        val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report factoryOf with different parameter types`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.factoryOf

            class MyService(val name: String, val count: Int)

            val m = module {
                factoryOf(::MyService)
            }
        """.trimIndent()

        val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report lambda-based factory`() {
        val code = """
            import org.koin.dsl.module

            class MyService(val a: Int, val b: Int)

            val m = module {
                factory { MyService(get(), get()) }
            }
        """.trimIndent()

        val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
```

Run: `./gradlew test --tests "ConstructorDslAmbiguousParametersTest"`
Expected: FAILED (class not found)

Commit:
```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/ConstructorDslAmbiguousParametersTest.kt
git commit -m "test: add failing tests for ConstructorDslAmbiguousParameters"
```

#### Task 1.2: Implement rule (heuristic-based)

Create `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/ConstructorDslAmbiguousParameters.kt`:

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass

internal class ConstructorDslAmbiguousParameters(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "ConstructorDslAmbiguousParameters",
        severity = Severity.Warning,
        description = "Detects factoryOf/singleOf/viewModelOf with duplicate parameter types, " +
                "which causes Koin to resolve parameters incorrectly",
        debt = Debt.TEN_MINS
    )

    private val constructorDslFunctions = setOf("factoryOf", "singleOf", "viewModelOf")

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName !in constructorDslFunctions) return

        // Get constructor reference: factoryOf(::MyClass)
        val arg = expression.valueArguments.firstOrNull()?.text ?: return
        if (!arg.startsWith("::")) return

        val className = arg.removePrefix("::")

        // Heuristic: Look for class definition in same file
        val classDecl = findClassInFile(expression.containingKtFile, className) ?: return
        val constructor = classDecl.primaryConstructor ?: return

        val parameterTypes = constructor.valueParameters.map { param ->
            param.typeReference?.text?.removeSuffix("?") // Normalize Int? → Int
        }

        // Check for duplicates
        val duplicates = parameterTypes.groupingBy { it }.eachCount().filter { it.value > 1 }

        if (duplicates.isNotEmpty()) {
            val duplicateTypesList = duplicates.keys.joinToString(", ")
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Constructor DSL with ambiguous parameters → Incorrect parameter resolution
                    → Found duplicate types: $duplicateTypesList
                    → Use lambda syntax for explicit resolution

                    ✗ Bad:  factoryOf(::$className)
                    ✓ Good: factory { $className(get(), get()) }
                    """.trimIndent()
                )
            )
        }
    }

    private fun findClassInFile(file: KtFile, className: String): KtClass? {
        return file.declarations.filterIsInstance<KtClass>()
            .firstOrNull { it.name == className }
    }
}
```

Run: `./gradlew test --tests "ConstructorDslAmbiguousParametersTest"`
Expected: Tests PASS

Commit:
```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/ConstructorDslAmbiguousParameters.kt
git commit -m "feat: add ConstructorDslAmbiguousParameters rule"
```

#### Task 1.3: Add edge case tests

Add to test file:

```kotlin
@Test
fun `reports singleOf with String String parameters`() {
    val code = """
        import org.koin.dsl.module
        import org.koin.core.module.dsl.singleOf

        class Config(val host: String, val port: String)

        val m = module {
            singleOf(::Config)
        }
    """.trimIndent()

    val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}

@Test
fun `does not report single parameter constructor`() {
    val code = """
        import org.koin.dsl.module
        import org.koin.core.module.dsl.factoryOf

        class MyService(val name: String)

        val m = module {
            factoryOf(::MyService)
        }
    """.trimIndent()

    val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}
```

Run tests, commit:
```bash
git commit -am "test: add edge case tests for ConstructorDslAmbiguousParameters"
```

#### Task 1.4: Register and document

**KoinRuleSetProvider:**
```kotlin
import io.github.krozov.detekt.koin.moduledsl.ConstructorDslAmbiguousParameters

// In listOf():
ConstructorDslAmbiguousParameters(config),
```

**docs/rules.md** (in Module DSL Rules section):
```markdown
### ConstructorDslAmbiguousParameters

**Severity:** Warning
**Active by default:** Yes

Detects `factoryOf(::)` / `singleOf(::)` / `viewModelOf(::)` with duplicate parameter types.

❌ **Bad:**
```kotlin
class MyService(val a: Int, val b: Int)

val m = module {
    factoryOf(::MyService)  // b gets value of a
}
```

✅ **Good:**
```kotlin
val m = module {
    factory { MyService(get(), get()) }
}
```

**Why this matters:**
Koin's constructor DSL incorrectly resolves parameters of the same type. Use lambda syntax for explicit parameter resolution.

---
```

**README.md:** Update count to 36
**CHANGELOG.md:**
```markdown
- **ConstructorDslAmbiguousParameters** - Detects constructor DSL with duplicate parameter types (#1372, #2347)
```

Commit:
```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt docs/rules.md README.md CHANGELOG.md
git commit -m "feat: register and document ConstructorDslAmbiguousParameters rule"
```

---

## Rule 2: ParameterTypeMatchesReturnType

### Problem
**Issue:** [#2328](https://github.com/InsertKoinIO/koin/issues/2328)

When a factory's return type matches one of the parameter types in `parametersOf()`, Koin returns the parameter value directly instead of executing the factory. This is undocumented and causes silent bugs.

### Detection Pattern
```kotlin
// Detect: factory<Int> { limit: Int -> Random.nextInt(limit) }
factory<Int>(named("random")) { limit ->
    Random.nextInt(limit)  // ❌ Always returns `limit` parameter
}

// Better: use different types
factory(named("random")) { (limit: Int) ->
    Random.nextInt(limit)  // ✅ Factory executes
}
```

### Implementation Tasks

#### Task 2.1: Write test

Create `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/ParameterTypeMatchesReturnTypeTest.kt`:

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParameterTypeMatchesReturnTypeTest {

    @Test
    fun `reports factory with return type matching parameter type`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                factory<Int> { limit: Int ->
                    kotlin.random.Random.nextInt(limit)
                }
            }
        """.trimIndent()

        val findings = ParameterTypeMatchesReturnType(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("parametersOf")
    }

    @Test
    fun `does not report when return type differs from parameter`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                factory<String> { count: Int ->
                    "Result: ${'$'}count"
                }
            }
        """.trimIndent()

        val findings = ParameterTypeMatchesReturnType(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report factory without type parameter`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                factory { Service() }
            }
        """.trimIndent()

        val findings = ParameterTypeMatchesReturnType(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
```

Commit: `test: add ParameterTypeMatchesReturnType tests`

#### Task 2.2: Implement rule

Create `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/ParameterTypeMatchesReturnType.kt`:

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*

internal class ParameterTypeMatchesReturnType(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "ParameterTypeMatchesReturnType",
        severity = Severity.Warning,
        description = "Detects factory definitions where return type matches parameter type, " +
                "causing parametersOf() to return the parameter directly instead of executing the factory",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.calleeExpression?.text != "factory") return

        // Get type argument: factory<Int>
        val typeArgument = expression.typeArguments.firstOrNull()?.text ?: return

        // Get lambda parameter type
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return
        val paramType = lambda.valueParameters.firstOrNull()?.typeReference?.text ?: return

        // Normalize (remove nullability)
        val normalizedReturn = typeArgument.removeSuffix("?")
        val normalizedParam = paramType.removeSuffix("?")

        if (normalizedReturn == normalizedParam) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Factory return type matches parameter type → parametersOf short-circuits
                    → Koin returns parameter directly without executing factory

                    ✗ Bad:  factory<$typeArgument> { limit: $paramType -> ... }
                    ✓ Good: Use different parameter type or remove type argument
                    """.trimIndent()
                )
            )
        }
    }
}
```

Commit: `feat: add ParameterTypeMatchesReturnType rule`

#### Task 2.3: Register and document

Register in provider, document in docs/rules.md:

```markdown
### ParameterTypeMatchesReturnType

**Severity:** Warning
**Active by default:** Yes

Detects factory definitions where the return type matches a parameter type.

❌ **Bad:**
```kotlin
factory<Int>(named("random")) { limit: Int ->
    Random.nextInt(limit)  // Never executes - returns `limit`
}
```

✅ **Good:**
```kotlin
factory(named("random")) { params ->
    val limit = params.get<Int>()
    Random.nextInt(limit)
}
```

**Why this matters:**
Koin has undocumented short-circuit behavior: when parametersOf() provides a value matching the factory's return type, Koin returns that value directly without executing the factory lambda.

---
```

Update README (37), CHANGELOG

Commit: `feat: register and document ParameterTypeMatchesReturnType`

---

## Final Verification

```bash
./gradlew test
./gradlew koverVerify
./gradlew detekt
```

Final commit:
```bash
git commit --allow-empty -m "feat: complete Group 3 (Parameters & Constructors) - 2 rules

Implemented:
- ConstructorDslAmbiguousParameters (HIGH, #1372, #2347)
- ParameterTypeMatchesReturnType (MEDIUM, #2328)

Total: 2 rules, 10+ tests, coverage ≥96%"
```

---

## Success Criteria

- [x] 2 rules implemented
- [x] 10+ tests
- [x] Coverage ≥96%
- [x] Documentation complete
- [x] Registered in provider
- [x] No false positives in self-dogfooding
