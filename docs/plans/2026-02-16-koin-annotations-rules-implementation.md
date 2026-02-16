# Koin Annotations Rules Expansion — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add 7 new annotation rules and improve 5 existing ones to catch KSP validation gaps in Koin Annotations usage.

**Architecture:** All rules live in `src/main/kotlin/io/github/krozov/detekt/koin/annotations/`. Each rule extends `Rule(config)`, uses PSI visitors (`visitClass`, `visitNamedFunction`, `visitAnnotationEntry`), and reports `CodeSmell`. Tests use `lint()` with inline code strings and AssertJ assertions.

**Tech Stack:** Kotlin PSI, Detekt API (`Rule`, `CodeSmell`, `Entity`, `Issue`), JUnit 5, AssertJ

---

## Task 1: `SingleAnnotationOnObject`

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/SingleAnnotationOnObject.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/SingleAnnotationOnObjectTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SingleAnnotationOnObjectTest {

    @Test
    fun `reports Single on object declaration`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            object MySingleton
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("object declaration")
    }

    @Test
    fun `reports Factory on object declaration`() {
        val code = """
            import org.koin.core.annotation.Factory

            @Factory
            object MyFactory
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows Single on regular class`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows object without Koin annotations`() {
        val code = """
            object PlainObject
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports KoinViewModel on object`() {
        val code = """
            import org.koin.android.annotation.KoinViewModel

            @KoinViewModel
            object BadViewModel
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows companion object without report`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService {
                companion object
            }
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "io.github.krozov.detekt.koin.annotations.SingleAnnotationOnObjectTest"`
Expected: FAIL — class `SingleAnnotationOnObject` does not exist

**Step 3: Write minimal implementation**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtObjectDeclaration

/**
 * Detects Koin definition annotations (`@Single`, `@Factory`, etc.) on Kotlin `object` declarations.
 *
 * Objects are language-level singletons. Annotating them with `@Single` generates invalid code
 * like `single { MyObject() }` — calling a non-existent constructor.
 *
 * <noncompliant>
 * @Single
 * object MySingleton // Generates invalid: single { MySingleton() }
 * </noncompliant>
 *
 * <compliant>
 * @Single
 * class MySingleton // Correct: single { MySingleton() }
 * </compliant>
 */
public class SingleAnnotationOnObject(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "SingleAnnotationOnObject",
        severity = Severity.Warning,
        description = "Koin annotation on object declaration generates invalid constructor call",
        debt = Debt.FIVE_MINS
    )

    private val koinDefinitionAnnotations = setOf(
        "Single", "Factory", "Scoped", "KoinViewModel", "KoinWorker"
    )

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        super.visitObjectDeclaration(declaration)

        if (declaration.isCompanion()) return

        val annotations = declaration.annotationEntries.mapNotNull { it.shortName?.asString() }
        val koinAnnotation = annotations.firstOrNull { it in koinDefinitionAnnotations } ?: return

        report(
            CodeSmell(
                issue,
                Entity.from(declaration),
                """
                @$koinAnnotation on object declaration generates invalid constructor call
                → Objects are already singletons — remove the annotation or convert to a class

                ✗ Bad:  @$koinAnnotation object ${declaration.name}
                ✓ Good: @$koinAnnotation class ${declaration.name}
                """.trimIndent()
            )
        )
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "io.github.krozov.detekt.koin.annotations.SingleAnnotationOnObjectTest"`
Expected: PASS

**Step 5: Register in KoinRuleSetProvider**

Add import and rule to the provider list:
```kotlin
import io.github.krozov.detekt.koin.annotations.SingleAnnotationOnObject
// In the listOf(...):
SingleAnnotationOnObject(config),
```

**Step 6: Commit**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/annotations/SingleAnnotationOnObject.kt \
        src/test/kotlin/io/github/krozov/detekt/koin/annotations/SingleAnnotationOnObjectTest.kt \
        src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: add SingleAnnotationOnObject rule"
```

---

## Task 2: `TooManyInjectedParams`

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/TooManyInjectedParams.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/TooManyInjectedParamsTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TooManyInjectedParamsTest {

    @Test
    fun `reports class with 6 InjectedParam parameters`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(
                @InjectedParam val a: String,
                @InjectedParam val b: Int,
                @InjectedParam val c: Long,
                @InjectedParam val d: Float,
                @InjectedParam val e: Double,
                @InjectedParam val f: Boolean
            )
        """.trimIndent()

        val findings = TooManyInjectedParams(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("6")
        assertThat(findings[0].message).contains("5")
    }

    @Test
    fun `allows class with 5 InjectedParam parameters`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(
                @InjectedParam val a: String,
                @InjectedParam val b: Int,
                @InjectedParam val c: Long,
                @InjectedParam val d: Float,
                @InjectedParam val e: Double
            )
        """.trimIndent()

        val findings = TooManyInjectedParams(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows mix of InjectedParam and regular params`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(
                @InjectedParam val a: String,
                @InjectedParam val b: Int,
                val regularParam: Long,
                val anotherRegular: Float
            )
        """.trimIndent()

        val findings = TooManyInjectedParams(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows class without InjectedParam`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService(val a: String, val b: Int)
        """.trimIndent()

        val findings = TooManyInjectedParams(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "io.github.krozov.detekt.koin.annotations.TooManyInjectedParamsTest"`
Expected: FAIL

**Step 3: Write minimal implementation**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass

/**
 * Detects classes with more than 5 `@InjectedParam` parameters.
 *
 * `ParametersHolder` only supports destructuring up to `component5()`.
 * Exceeding this limit causes a confusing compile error.
 *
 * <noncompliant>
 * @Single
 * class MyService(
 *     @InjectedParam val a: String,
 *     @InjectedParam val b: Int,
 *     @InjectedParam val c: Long,
 *     @InjectedParam val d: Float,
 *     @InjectedParam val e: Double,
 *     @InjectedParam val f: Boolean // 6th — too many!
 * )
 * </noncompliant>
 *
 * <compliant>
 * @Single
 * class MyService(
 *     @InjectedParam val params: MyServiceParams // Use wrapper
 * )
 * </compliant>
 */
public class TooManyInjectedParams(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "TooManyInjectedParams",
        severity = Severity.Warning,
        description = "Too many @InjectedParam parameters — ParametersHolder supports max 5",
        debt = Debt.TEN_MINS
    )

    @Suppress("MemberVisibilityCanBePrivate")
    internal val maxInjectedParams = valueOrDefault("maxInjectedParams", 5)

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val primaryConstructor = klass.primaryConstructor ?: return
        val injectedParamCount = primaryConstructor.valueParameters.count { param ->
            param.annotationEntries.any { it.shortName?.asString() == "InjectedParam" }
        }

        if (injectedParamCount > maxInjectedParams) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    Class has $injectedParamCount @InjectedParam parameters, but ParametersHolder supports max $maxInjectedParams
                    → Reduce parameters or use a wrapper class

                    ✗ Bad:  class ${klass.name}(@InjectedParam a, @InjectedParam b, ... $injectedParamCount params)
                    ✓ Good: class ${klass.name}(@InjectedParam params: ${klass.name}Params)
                    """.trimIndent()
                )
            )
        }
    }
}
```

**Step 4: Run test, register in provider, commit**

Same pattern as Task 1.

---

## Task 3: `InvalidNamedQualifierCharacters`

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/InvalidNamedQualifierCharacters.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/InvalidNamedQualifierCharactersTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InvalidNamedQualifierCharactersTest {

    @Test
    fun `reports Named with hyphen`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named("ricky-morty")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("ricky-morty")
    }

    @Test
    fun `reports Named with spaces`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named("my service")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows Named with valid characters`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named("myService")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows Named with underscores and dots`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named("my_service.impl")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows Named with only digits and letters`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named("service123")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports Named on function parameter`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single

            @Module
            class MyModule {
                @Single
                fun provideService(@Named("bad-name") dep: Dependency): MyService = MyService(dep)
            }
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
```

**Step 2: Run test to verify it fails**

**Step 3: Write minimal implementation**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Detects `@Named` values containing characters invalid in generated Kotlin identifiers.
 *
 * Hyphens, spaces, and special characters in `@Named` values cause KSP code generation failures.
 *
 * <noncompliant>
 * @Named("ricky-morty")
 * @Single
 * class MyService
 * </noncompliant>
 *
 * <compliant>
 * @Named("rickyMorty")
 * @Single
 * class MyService
 * </compliant>
 */
public class InvalidNamedQualifierCharacters(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "InvalidNamedQualifierCharacters",
        severity = Severity.Warning,
        description = "@Named value contains characters invalid in generated Kotlin identifiers",
        debt = Debt.FIVE_MINS
    )

    private val validPattern = Regex("^[a-zA-Z][a-zA-Z0-9_.]*$")

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        super.visitAnnotationEntry(annotationEntry)

        if (annotationEntry.shortName?.asString() != "Named") return

        val args = annotationEntry.valueArgumentList?.arguments ?: return
        val firstArg = args.firstOrNull() ?: return

        val stringExpr = firstArg.getArgumentExpression() as? KtStringTemplateExpression ?: return
        val namedValue = stringExpr.entries.joinToString("") { it.text }

        if (namedValue.isNotEmpty() && !validPattern.matches(namedValue)) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(annotationEntry),
                    """
                    @Named("$namedValue") contains invalid characters for generated Kotlin identifiers
                    → Use only letters, digits, underscores, and dots

                    ✗ Bad:  @Named("$namedValue")
                    ✓ Good: @Named("${namedValue.replace(Regex("[^a-zA-Z0-9_.]"), "_")}")
                    """.trimIndent()
                )
            )
        }
    }
}
```

**Step 4: Run test, register in provider, commit**

---

## Task 4: `KoinAnnotationOnExtensionFunction`

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/KoinAnnotationOnExtensionFunction.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/KoinAnnotationOnExtensionFunctionTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoinAnnotationOnExtensionFunctionTest {

    @Test
    fun `reports Single on extension function`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single
            import org.koin.core.scope.Scope

            @Module
            class MyModule {
                @Single
                fun Scope.provideDatastore(): DataStore = DataStore()
            }
        """.trimIndent()

        val findings = KoinAnnotationOnExtensionFunction(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("extension function")
    }

    @Test
    fun `reports Factory on extension function`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Factory

            @Module
            class MyModule {
                @Factory
                fun String.toService(): MyService = MyService(this)
            }
        """.trimIndent()

        val findings = KoinAnnotationOnExtensionFunction(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows regular function with annotation`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single

            @Module
            class MyModule {
                @Single
                fun provideService(): MyService = MyService()
            }
        """.trimIndent()

        val findings = KoinAnnotationOnExtensionFunction(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows extension function without Koin annotations`() {
        val code = """
            fun String.toUpperCase(): String = this.uppercase()
        """.trimIndent()

        val findings = KoinAnnotationOnExtensionFunction(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
```

**Step 3: Write minimal implementation**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Detects Koin definition annotations on extension functions.
 *
 * KSP code generator ignores the receiver parameter on extension functions,
 * producing invalid generated code.
 *
 * <noncompliant>
 * @Module
 * class MyModule {
 *     @Single
 *     fun Scope.provideDatastore(): DataStore = DataStore()
 * }
 * </noncompliant>
 *
 * <compliant>
 * @Module
 * class MyModule {
 *     @Single
 *     fun provideDatastore(scope: Scope): DataStore = DataStore()
 * }
 * </compliant>
 */
public class KoinAnnotationOnExtensionFunction(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "KoinAnnotationOnExtensionFunction",
        severity = Severity.Warning,
        description = "Koin annotation on extension function — KSP ignores receiver parameter",
        debt = Debt.FIVE_MINS
    )

    private val koinDefinitionAnnotations = setOf(
        "Single", "Factory", "Scoped", "KoinViewModel", "KoinWorker"
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        if (function.receiverTypeReference == null) return

        val annotations = function.annotationEntries.mapNotNull { it.shortName?.asString() }
        val koinAnnotation = annotations.firstOrNull { it in koinDefinitionAnnotations } ?: return

        val receiverType = function.receiverTypeReference?.text ?: "Unknown"

        report(
            CodeSmell(
                issue,
                Entity.from(function),
                """
                @$koinAnnotation on extension function — KSP ignores receiver '$receiverType'
                → Convert to regular function with explicit parameter

                ✗ Bad:  @$koinAnnotation fun $receiverType.${function.name}()
                ✓ Good: @$koinAnnotation fun ${function.name}(${receiverType.lowercase()}: $receiverType)
                """.trimIndent()
            )
        )
    }
}
```

**Step 4: Run test, register in provider, commit**

---

## Task 5: `ViewModelAnnotatedAsSingle`

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/ViewModelAnnotatedAsSingle.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/ViewModelAnnotatedAsSingleTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ViewModelAnnotatedAsSingleTest {

    @Test
    fun `reports ViewModel with Single annotation`() {
        val code = """
            import org.koin.core.annotation.Single
            import androidx.lifecycle.ViewModel

            @Single
            class MyViewModel : ViewModel()
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("@KoinViewModel")
    }

    @Test
    fun `reports AndroidViewModel with Single annotation`() {
        val code = """
            import org.koin.core.annotation.Single
            import androidx.lifecycle.AndroidViewModel
            import android.app.Application

            @Single
            class MyViewModel(app: Application) : AndroidViewModel(app)
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports ViewModel with Factory annotation`() {
        val code = """
            import org.koin.core.annotation.Factory
            import androidx.lifecycle.ViewModel

            @Factory
            class MyViewModel : ViewModel()
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows ViewModel with KoinViewModel annotation`() {
        val code = """
            import org.koin.android.annotation.KoinViewModel
            import androidx.lifecycle.ViewModel

            @KoinViewModel
            class MyViewModel : ViewModel()
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows regular class with Single annotation`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports indirect ViewModel subclass`() {
        val code = """
            import org.koin.core.annotation.Single
            import androidx.lifecycle.ViewModel

            abstract class BaseViewModel : ViewModel()

            @Single
            class MyViewModel : BaseViewModel()
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
```

**Step 3: Write minimal implementation**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass

/**
 * Detects ViewModel classes annotated with `@Single` or `@Factory` instead of `@KoinViewModel`.
 *
 * ViewModels as singletons cause coroutine scope issues: `viewModelScope` is cancelled
 * on navigation but the singleton persists, leading to `CancellationException`.
 *
 * <noncompliant>
 * @Single
 * class MyViewModel : ViewModel() // Coroutine failures after navigation!
 * </noncompliant>
 *
 * <compliant>
 * @KoinViewModel
 * class MyViewModel : ViewModel()
 * </compliant>
 */
public class ViewModelAnnotatedAsSingle(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ViewModelAnnotatedAsSingle",
        severity = Severity.Warning,
        description = "ViewModel should use @KoinViewModel, not @Single/@Factory",
        debt = Debt.FIVE_MINS
    )

    private val wrongAnnotations = setOf("Single", "Factory")
    private val viewModelTypes = setOf("ViewModel", "AndroidViewModel")

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val annotations = klass.annotationEntries.mapNotNull { it.shortName?.asString() }
        val wrongAnnotation = annotations.firstOrNull { it in wrongAnnotations } ?: return

        // Check supertypes for ViewModel
        val supertypes = klass.superTypeListEntries.mapNotNull { entry ->
            entry.typeReference?.text?.substringBefore("(")?.substringAfterLast(".")?.trim()
        }

        if (supertypes.any { it in viewModelTypes }) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    ViewModel annotated with @$wrongAnnotation instead of @KoinViewModel
                    → ViewModels need lifecycle-aware scoping, not singleton/factory

                    ✗ Bad:  @$wrongAnnotation class ${klass.name} : ViewModel()
                    ✓ Good: @KoinViewModel class ${klass.name} : ViewModel()
                    """.trimIndent()
                )
            )
        }
    }
}
```

**Note:** The "indirect ViewModel subclass" test (extending `BaseViewModel`) relies on PSI text matching of supertypes. This catches direct references only (`ViewModel`, `AndroidViewModel`). For indirect subclasses like `BaseViewModel`, PSI-only analysis cannot resolve the full type hierarchy. The test for indirect subclasses may need to be adjusted — either removed or implemented with type resolution if available. Document this limitation.

**Step 4: Run test, register in provider, commit**

---

## Task 6: `AnnotatedClassImplementsNestedInterface`

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/AnnotatedClassImplementsNestedInterface.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/AnnotatedClassImplementsNestedInterfaceTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AnnotatedClassImplementsNestedInterfaceTest {

    @Test
    fun `reports Single class implementing nested interface`() {
        val code = """
            import org.koin.core.annotation.Single

            class Parent {
                interface ChildInterface
            }

            @Single
            class MyImpl : Parent.ChildInterface
        """.trimIndent()

        val findings = AnnotatedClassImplementsNestedInterface(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("nested")
    }

    @Test
    fun `reports Factory class implementing sealed interface member`() {
        val code = """
            import org.koin.core.annotation.Factory

            sealed interface Transformer {
                interface TextTransformer
            }

            @Factory
            class MyTransformer : Transformer.TextTransformer
        """.trimIndent()

        val findings = AnnotatedClassImplementsNestedInterface(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows class implementing top-level interface`() {
        val code = """
            import org.koin.core.annotation.Single

            interface MyInterface

            @Single
            class MyImpl : MyInterface
        """.trimIndent()

        val findings = AnnotatedClassImplementsNestedInterface(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows class without Koin annotations implementing nested interface`() {
        val code = """
            class Parent {
                interface ChildInterface
            }

            class MyImpl : Parent.ChildInterface
        """.trimIndent()

        val findings = AnnotatedClassImplementsNestedInterface(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
```

**Step 3: Write minimal implementation**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass

/**
 * Detects Koin-annotated classes that implement nested/inner interfaces.
 *
 * KSP code generator drops the parent qualifier in `bind()` call, generating
 * `bind(ChildInterface::class)` instead of `bind(Parent.ChildInterface::class)`.
 *
 * <noncompliant>
 * @Single
 * class MyImpl : Parent.ChildInterface // KSP generates bind(ChildInterface::class) — wrong!
 * </noncompliant>
 *
 * <compliant>
 * // Extract to top-level:
 * interface ChildInterface
 *
 * @Single
 * class MyImpl : ChildInterface
 * </compliant>
 */
public class AnnotatedClassImplementsNestedInterface(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "AnnotatedClassImplementsNestedInterface",
        severity = Severity.Warning,
        description = "Koin-annotated class implements nested interface — KSP may drop parent qualifier",
        debt = Debt.TEN_MINS
    )

    private val koinDefinitionAnnotations = setOf(
        "Single", "Factory", "Scoped", "KoinViewModel", "KoinWorker"
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val annotations = klass.annotationEntries.mapNotNull { it.shortName?.asString() }
        if (annotations.none { it in koinDefinitionAnnotations }) return

        klass.superTypeListEntries.forEach { entry ->
            val typeText = entry.typeReference?.text ?: return@forEach
            // Nested interface reference contains a dot: Parent.ChildInterface
            if ("." in typeText.substringBefore("<")) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(entry),
                        """
                        Implements nested interface '$typeText' — KSP may drop parent qualifier in bind()
                        → Extract interface to top-level declaration to avoid resolution failure

                        ✗ Bad:  @Single class ${klass.name} : $typeText
                        ✓ Good: Extract ${typeText.substringAfterLast(".")} to top-level, then @Single class ${klass.name} : ${typeText.substringAfterLast(".")}
                        """.trimIndent()
                    )
                )
            }
        }
    }
}
```

**Step 4: Run test, register in provider, commit**

---

## Task 7: `InjectedParamWithNestedGenericType`

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/InjectedParamWithNestedGenericType.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/InjectedParamWithNestedGenericTypeTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InjectedParamWithNestedGenericTypeTest {

    @Test
    fun `reports InjectedParam with nested generic type`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(@InjectedParam val items: List<List<String>>)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("nested generic")
    }

    @Test
    fun `reports InjectedParam with Map of Lists`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(@InjectedParam val map: Map<String, List<Int>>)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports InjectedParam with star projection`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(@InjectedParam val items: List<*>)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows InjectedParam with simple generic type`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(@InjectedParam val items: List<String>)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows InjectedParam with non-generic type`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(@InjectedParam val name: String)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows regular param with nested generic type`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService(val items: List<List<String>>)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
```

**Step 3: Write minimal implementation**

```kotlin
package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Detects `@InjectedParam` with nested generic types or star projections.
 *
 * KSP code generator has a known bug where nested type arguments are dropped,
 * e.g. `List<List<String>>` generates `List<kotlin.collections.List>`.
 *
 * <noncompliant>
 * @Single
 * class MyService(@InjectedParam val items: List<List<String>>) // KSP bug!
 * </noncompliant>
 *
 * <compliant>
 * typealias StringList = List<String>
 *
 * @Single
 * class MyService(@InjectedParam val items: List<StringList>) // Workaround
 * </compliant>
 */
public class InjectedParamWithNestedGenericType(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "InjectedParamWithNestedGenericType",
        severity = Severity.Warning,
        description = "@InjectedParam with nested generic type — KSP generates incorrect code",
        debt = Debt.TEN_MINS
    )

    override fun visitParameter(parameter: KtParameter) {
        super.visitParameter(parameter)

        val hasInjectedParam = parameter.annotationEntries.any {
            it.shortName?.asString() == "InjectedParam"
        }
        if (!hasInjectedParam) return

        val typeRef = parameter.typeReference ?: return

        if (hasNestedGenerics(typeRef) || hasStarProjection(typeRef)) {
            val typeText = typeRef.text
            report(
                CodeSmell(
                    issue,
                    Entity.from(parameter),
                    """
                    @InjectedParam with nested generic type '$typeText' — KSP may generate incorrect code
                    → Use a typealias or wrapper class to flatten the type

                    ✗ Bad:  @InjectedParam val param: $typeText
                    ✓ Good: typealias FlatType = ...; @InjectedParam val param: FlatType
                    """.trimIndent()
                )
            )
        }
    }

    private fun hasNestedGenerics(typeRef: KtTypeReference): Boolean {
        val typeElement = typeRef.typeElement ?: return false
        val typeArgList = typeElement.children.filterIsInstance<KtTypeArgumentList>().firstOrNull()
            ?: return false

        return typeArgList.arguments.any { projection ->
            val innerTypeRef = projection.typeReference ?: return@any false
            val innerTypeElement = innerTypeRef.typeElement ?: return@any false
            innerTypeElement.children.any { it is KtTypeArgumentList }
        }
    }

    private fun hasStarProjection(typeRef: KtTypeReference): Boolean {
        val typeElement = typeRef.typeElement ?: return false
        val typeArgList = typeElement.children.filterIsInstance<KtTypeArgumentList>().firstOrNull()
            ?: return false

        return typeArgList.arguments.any { projection ->
            projection.projectionKind == KtProjectionKind.STAR
        }
    }
}
```

**Note:** Import `org.jetbrains.kotlin.psi.KtProjectionKind` — if this enum is not available in the detekt API, check the actual PSI API. Alternative: check `projection.text == "*"`.

**Step 4: Run test, register in provider, commit**

---

## Task 8: Improve `ScopedWithoutQualifier`

**Files:**
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/ScopedWithoutQualifier.kt`
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/ScopedWithoutQualifierTest.kt`

**Step 1: Add new failing tests**

Add to existing test file:

```kotlin
@Test
fun `reports Scoped without Scope annotation on class`() {
    val code = """
        import org.koin.core.annotation.Scoped

        @Scoped(name = "userScope")
        class MyService // Has qualifier but no @Scope!
    """.trimIndent()

    val findings = ScopedWithoutQualifier(Config.empty).lint(code)

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("@Scope")
}

@Test
fun `allows Scoped with Scope annotation`() {
    val code = """
        import org.koin.core.annotation.Scope
        import org.koin.core.annotation.Scoped

        @Scope(name = "userScope")
        @Scoped
        class MyService
    """.trimIndent()

    val findings = ScopedWithoutQualifier(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}

@Test
fun `allows Scoped with ActivityScope archetype`() {
    val code = """
        import org.koin.core.annotation.Scoped
        import org.koin.android.annotation.ActivityScope

        @ActivityScope
        @Scoped
        class MyService
    """.trimIndent()

    val findings = ScopedWithoutQualifier(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}
```

**Step 2: Run tests to verify new ones fail**

**Step 3: Update implementation**

Replace the `visitClass` method in `ScopedWithoutQualifier`:

```kotlin
private val scopeArchetypes = setOf(
    "Scope", "ViewModelScope", "ActivityScope", "ActivityRetainedScope", "FragmentScope"
)

override fun visitClass(klass: KtClass) {
    super.visitClass(klass)

    val annotations = klass.annotationEntries.mapNotNull { it.shortName?.asString() }
    val hasScopedAnnotation = "Scoped" in annotations
    if (!hasScopedAnnotation) return

    val hasScopeAnnotation = annotations.any { it in scopeArchetypes }

    if (!hasScopeAnnotation) {
        val scopedAnnotation = klass.annotationEntries.find {
            it.shortName?.asString() == "Scoped"
        }!!
        report(
            CodeSmell(
                issue,
                Entity.from(scopedAnnotation),
                """
                @Scoped without @Scope annotation → Undefined scope boundary at runtime
                → Add @Scope, @ActivityScope, @FragmentScope, or similar to define scope

                ✗ Bad:  @Scoped class ${klass.name}
                ✓ Good: @Scope(name = "myScope") @Scoped class ${klass.name}
                """.trimIndent()
            )
        )
    }
}
```

**Step 4: Run tests, commit**

---

## Task 9: Improve `MissingModuleAnnotation`

**Files:**
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/MissingModuleAnnotation.kt`
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/MissingModuleAnnotationTest.kt`

**Step 1: Add new failing tests**

```kotlin
@Test
fun `reports Module without ComponentScan or includes or definitions`() {
    val code = """
        import org.koin.core.annotation.Module

        @Module
        class EmptyModule // No @ComponentScan, no includes, no definitions!
    """.trimIndent()

    val findings = MissingModuleAnnotation(Config.empty).lint(code)

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("empty")
}

@Test
fun `allows Module with ComponentScan`() {
    val code = """
        import org.koin.core.annotation.Module
        import org.koin.core.annotation.ComponentScan

        @Module
        @ComponentScan
        class MyModule
    """.trimIndent()

    val findings = MissingModuleAnnotation(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}

@Test
fun `allows Module with includes parameter`() {
    val code = """
        import org.koin.core.annotation.Module

        @Module(includes = [OtherModule::class])
        class MyModule
    """.trimIndent()

    val findings = MissingModuleAnnotation(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}

@Test
fun `allows Module with internal definitions`() {
    val code = """
        import org.koin.core.annotation.Module
        import org.koin.core.annotation.Single

        @Module
        class MyModule {
            @Single
            fun provideRepo(): Repository = RepositoryImpl()
        }
    """.trimIndent()

    val findings = MissingModuleAnnotation(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}
```

**Step 3: Update implementation** — add empty module detection to the existing `visitClass`:

After the existing `!hasModuleAnnotation` check, add:

```kotlin
if (hasModuleAnnotation) {
    val hasComponentScan = classAnnotations.any { it == "ComponentScan" }
    val hasIncludes = klass.annotationEntries.any { entry ->
        entry.shortName?.asString() == "Module" &&
            entry.valueArgumentList?.arguments?.any { arg ->
                arg.getArgumentName()?.asName?.asString() == "includes"
            } == true
    }
    val hasKoinDefinitions = klass.declarations.any { declaration ->
        val annotations = declaration.annotationEntries.mapNotNull { it.shortName?.asString() }
        annotations.any { it in setOf("Single", "Factory", "Scoped", "KoinViewModel", "KoinWorker") }
    }

    if (!hasComponentScan && !hasIncludes && !hasKoinDefinitions) {
        report(
            CodeSmell(
                issue,
                Entity.from(klass),
                """
                @Module without @ComponentScan, includes, or definitions → Module will be empty
                → Add @ComponentScan or include other modules or add provider functions

                ✗ Bad:  @Module class ${klass.name ?: "EmptyModule"}
                ✓ Good: @Module @ComponentScan class ${klass.name ?: "EmptyModule"}
                """.trimIndent()
            )
        )
    }
}
```

**Step 4: Run tests, commit**

---

## Task 10: Improve `ConflictingBindings`

**Files:**
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/ConflictingBindings.kt`
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/ConflictingBindingsTest.kt`

**Step 1: Add new failing tests**

```kotlin
@Test
fun `reports duplicate definition annotations on same class`() {
    val code = """
        import org.koin.core.annotation.Single
        import org.koin.core.annotation.Factory

        @Single
        @Factory
        class MyService
    """.trimIndent()

    val findings = ConflictingBindings(Config.empty).lint(code)

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("@Single")
    assertThat(findings[0].message).contains("@Factory")
}

@Test
fun `allows single definition annotation`() {
    val code = """
        import org.koin.core.annotation.Single

        @Single
        class MyService
    """.trimIndent()

    val findings = ConflictingBindings(Config.empty).lint(code)
    assertThat(findings).isEmpty()
}
```

**Step 3: Update implementation** — add duplicate annotation detection in `visitKtFile`:

Add a new visitor method:

```kotlin
override fun visitClass(klass: KtClass) {
    super.visitClass(klass)

    val koinAnnotations = klass.annotationEntries
        .mapNotNull { it.shortName?.asString() }
        .filter { it in setOf("Single", "Factory", "Scoped", "KoinViewModel", "KoinWorker") }

    if (koinAnnotations.size > 1) {
        report(
            CodeSmell(
                issue,
                Entity.from(klass),
                """
                Multiple Koin definition annotations: ${koinAnnotations.joinToString(", ") { "@$it" }}
                → KSP picks first annotation; behavior is undefined. Choose one.

                ✗ Bad:  ${koinAnnotations.joinToString(" ") { "@$it" }} class ${klass.name}
                ✓ Good: @${koinAnnotations.first()} class ${klass.name}
                """.trimIndent()
            )
        )
    }
}
```

**Step 4: Run tests, commit**

---

## Task 11: Improve `MixingDslAndAnnotations`

**Files:**
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/MixingDslAndAnnotations.kt`
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/MixingDslAndAnnotationsTest.kt`

**Step 1: Add new failing tests**

```kotlin
@Test
fun `reports mixing DSL with Configuration annotation`() {
    val code = """
        import org.koin.core.annotation.Configuration
        import org.koin.dsl.module

        @Configuration
        class AppConfig

        val dslModule = module {
            single { ApiService() }
        }
    """.trimIndent()

    val findings = MixingDslAndAnnotations(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}

@Test
fun `reports mixing DSL with KoinApplication annotation`() {
    val code = """
        import org.koin.core.annotation.KoinApplication
        import org.koin.dsl.module

        @KoinApplication
        class MyApp

        val dslModule = module {
            single { ApiService() }
        }
    """.trimIndent()

    val findings = MixingDslAndAnnotations(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}

@Test
fun `reports mixing DSL with KoinViewModel annotation`() {
    val code = """
        import org.koin.android.annotation.KoinViewModel
        import org.koin.dsl.module

        @KoinViewModel
        class MyViewModel

        val dslModule = module {
            single { ApiService() }
        }
    """.trimIndent()

    val findings = MixingDslAndAnnotations(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}
```

**Step 3: Update implementation** — expand the annotation set:

Change line 68 in `MixingDslAndAnnotations.kt`:

```kotlin
// Old:
if ("Module" in annotations || "Single" in annotations || "Factory" in annotations) {

// New:
val koinAnnotations = setOf(
    "Module", "Single", "Factory", "Scoped",
    "Configuration", "KoinApplication", "ComponentScan",
    "KoinViewModel", "KoinWorker"
)
if (annotations.any { it in koinAnnotations }) {
```

**Step 4: Run tests, commit**

---

## Task 12: Improve `AnnotationProcessorNotConfigured`

**Files:**
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/annotations/AnnotationProcessorNotConfigured.kt`
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/annotations/AnnotationProcessorNotConfiguredTest.kt`

**Step 1: Add new failing tests**

```kotlin
@Test
fun `reports KoinViewModel without processor`() {
    val code = """
        import org.koin.android.annotation.KoinViewModel

        @KoinViewModel
        class MyViewModel
    """.trimIndent()

    val findings = AnnotationProcessorNotConfigured(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}

@Test
fun `reports ComponentScan without processor`() {
    val code = """
        import org.koin.core.annotation.Module
        import org.koin.core.annotation.ComponentScan

        @Module
        @ComponentScan
        class MyModule
    """.trimIndent()

    val findings = AnnotationProcessorNotConfigured(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}
```

**Step 3: Update implementation** — expand annotation detection set:

```kotlin
// Old:
if (annotations.any { it in setOf("Single", "Factory", "Scoped", "Module") }) {

// New:
val koinAnnotations = setOf(
    "Single", "Factory", "Scoped", "Module",
    "KoinViewModel", "KoinWorker", "ComponentScan",
    "Configuration", "KoinApplication"
)
if (annotations.any { it in koinAnnotations }) {
```

**Step 4: Run tests, commit**

---

## Task 13: Final Integration

**Files:**
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt` (verify all 7 new rules registered)
- Modify: `docs/rules.md` (add documentation for 7 new rules)
- Modify: `README.md` (update rule count: 29 → 36)
- Modify: `CHANGELOG.md` (add to Unreleased section)

**Step 1: Verify all rules are registered in KoinRuleSetProvider**

The final annotation section should be:
```kotlin
// Annotation rules
MixingDslAndAnnotations(config),
MissingModuleAnnotation(config),
ConflictingBindings(config),
ScopedWithoutQualifier(config),
AnnotationProcessorNotConfigured(config),
// New annotation rules
SingleAnnotationOnObject(config),
TooManyInjectedParams(config),
InvalidNamedQualifierCharacters(config),
KoinAnnotationOnExtensionFunction(config),
ViewModelAnnotatedAsSingle(config),
AnnotatedClassImplementsNestedInterface(config),
InjectedParamWithNestedGenericType(config),
```

**Step 2: Run full verification**

```bash
./gradlew test              # All tests must pass
./gradlew koverVerify       # Coverage ≥96%/70%
./gradlew detekt            # No violations
```

**Step 3: Update documentation**

Add 7 new rule descriptions to `docs/rules.md` following existing format.
Update rule count in `README.md` from 29 to 36.
Add entries to `CHANGELOG.md` under Unreleased.

**Step 4: Final commit**

```bash
git add docs/ README.md CHANGELOG.md
git commit -m "docs: add documentation for 7 new annotation rules"
```

---

## Execution Order Summary

| Task | Rule | Type | Complexity |
|------|------|------|------------|
| 1 | `SingleAnnotationOnObject` | NEW | EASY |
| 2 | `TooManyInjectedParams` | NEW | EASY |
| 3 | `InvalidNamedQualifierCharacters` | NEW | EASY |
| 4 | `KoinAnnotationOnExtensionFunction` | NEW | EASY |
| 5 | `ViewModelAnnotatedAsSingle` | NEW | EASY |
| 6 | `AnnotatedClassImplementsNestedInterface` | NEW | MEDIUM |
| 7 | `InjectedParamWithNestedGenericType` | NEW | MEDIUM |
| 8 | Improve `ScopedWithoutQualifier` | IMPROVE | EASY |
| 9 | Improve `MissingModuleAnnotation` | IMPROVE | EASY |
| 10 | Improve `ConflictingBindings` | IMPROVE | EASY |
| 11 | Improve `MixingDslAndAnnotations` | IMPROVE | EASY |
| 12 | Improve `AnnotationProcessorNotConfigured` | IMPROVE | EASY |
| 13 | Final Integration & Documentation | DOCS | EASY |

**Independent tasks (can parallelize):** Tasks 1-7 are independent of each other. Tasks 8-12 are independent of each other. Task 13 depends on all prior tasks.
