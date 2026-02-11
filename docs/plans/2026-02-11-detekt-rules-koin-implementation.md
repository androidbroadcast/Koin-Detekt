# detekt-rules-koin Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a Detekt 1.x extension library with 14 rules for Koin 4.x to enforce best practices and catch anti-patterns via static analysis.

**Architecture:** Single Gradle module with rules organized by package (servicelocator, moduledsl, scope). Pure syntactic analysis via Kotlin PSI—no Koin runtime dependency. Rules implement Detekt's Rule API and are discovered via ServiceLoader.

**Tech Stack:** Kotlin 2.0.21, Detekt 1.23.8, Gradle 8.x, JUnit 5, AssertJ

---

## Task 1: Project Setup and Infrastructure

**Files:**
- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `.gitignore`
- Create: `LICENSE`

**Step 1: Initialize Gradle wrapper**

Run:
```bash
gradle wrapper --gradle-version=8.5 --distribution-type=bin
```

Expected: Creates gradle wrapper files

**Step 2: Create settings.gradle.kts**

```kotlin
rootProject.name = "detekt-rules-koin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

**Step 3: Create gradle.properties**

```properties
kotlin.code.style=official
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
```

**Step 4: Create build.gradle.kts with dependencies**

```kotlin
plugins {
    kotlin("jvm") version "2.0.21"
    id("maven-publish")
}

group = "io.github.krozov"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:1.23.8")

    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:1.23.8")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}
```

**Step 5: Create .gitignore**

```
.gradle/
build/
.idea/
*.iml
*.ipr
*.iws
.DS_Store
/out/
```

**Step 6: Create LICENSE (Apache 2.0)**

```
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   Copyright 2026 Kirill Rozov

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

**Step 7: Create directory structure**

Run:
```bash
mkdir -p src/main/kotlin/io/github/krozov/detekt/koin/{servicelocator,moduledsl,scope,util}
mkdir -p src/main/resources/{config,META-INF/services}
mkdir -p src/test/kotlin/io/github/krozov/detekt/koin/{servicelocator,moduledsl,scope}
```

**Step 8: Verify project builds**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 9: Commit initial setup**

```bash
git add .
git commit -m "chore: initialize project structure and gradle setup"
```

---

## Task 2: RuleSetProvider and ServiceLoader Registration

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`
- Create: `src/main/resources/META-INF/services/io.gitlab.arturbosch.detekt.api.RuleSetProvider`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProviderTest.kt`

**Step 1: Write failing test for RuleSetProvider**

File: `src/test/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProviderTest.kt`

```kotlin
package io.github.krozov.detekt.koin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoinRuleSetProviderTest {

    @Test
    fun `should have correct rule set id`() {
        val provider = KoinRuleSetProvider()
        assertThat(provider.ruleSetId).isEqualTo("koin-rules")
    }

    @Test
    fun `should provide rules`() {
        val provider = KoinRuleSetProvider()
        val config = io.gitlab.arturbosch.detekt.api.Config.empty
        val ruleSet = provider.instance(config)

        assertThat(ruleSet.rules).isEmpty() // Will add rules later
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests KoinRuleSetProviderTest`
Expected: FAIL - "Unresolved reference: KoinRuleSetProvider"

**Step 3: Implement KoinRuleSetProvider**

File: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

```kotlin
package io.github.krozov.detekt.koin

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class KoinRuleSetProvider : RuleSetProvider {

    override val ruleSetId: String = "koin-rules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                // Rules will be added here
            )
        )
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests KoinRuleSetProviderTest`
Expected: PASS (both tests)

**Step 5: Create ServiceLoader registration file**

File: `src/main/resources/META-INF/services/io.gitlab.arturbosch.detekt.api.RuleSetProvider`

```
io.github.krozov.detekt.koin.KoinRuleSetProvider
```

**Step 6: Verify ServiceLoader discovery**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 7: Commit RuleSetProvider**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git add src/main/resources/META-INF/services/
git add src/test/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProviderTest.kt
git commit -m "feat: add KoinRuleSetProvider and ServiceLoader registration"
```

---

## Task 3: Service Locator Rule 1 - NoGetOutsideModuleDefinition

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinition.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinitionTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write failing test**

File: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinitionTest.kt`

```kotlin
package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class NoGetOutsideModuleDefinitionTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports get() call outside module definition`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository : KoinComponent {
                val api = get<ApiService>()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("get()")
    }

    @Test
    fun `does not report get() inside single block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { MyRepository(get()) }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report get() inside factory block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                factory { MyUseCase(get()) }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests NoGetOutsideModuleDefinitionTest`
Expected: FAIL - "Unresolved reference: NoGetOutsideModuleDefinition"

**Step 3: Implement NoGetOutsideModuleDefinition rule**

File: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinition.kt`

```kotlin
package io.github.krozov.detekt.koin.servicelocator

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

class NoGetOutsideModuleDefinition(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "NoGetOutsideModuleDefinition",
        severity = Severity.Warning,
        description = "Detects get() calls outside Koin module definition blocks. " +
                "Use constructor injection instead of service locator pattern.",
        debt = Debt.TWENTY_MINS
    )

    private var insideDefinitionBlock = false
    private val definitionFunctions = setOf("single", "factory", "scoped", "viewModel", "worker")

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Track entering definition blocks
        if (callName in definitionFunctions) {
            val wasInside = insideDefinitionBlock
            insideDefinitionBlock = true
            super.visitCallExpression(expression)
            insideDefinitionBlock = wasInside
            return
        }

        // Check for get() calls
        if (callName in setOf("get", "getOrNull", "getAll") && !insideDefinitionBlock) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Avoid using $callName() outside module definitions. Use constructor injection instead."
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests NoGetOutsideModuleDefinitionTest`
Expected: PASS (all 3 tests)

**Step 5: Register rule in RuleSetProvider**

File: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

```kotlin
package io.github.krozov.detekt.koin

import io.github.krozov.detekt.koin.servicelocator.NoGetOutsideModuleDefinition
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class KoinRuleSetProvider : RuleSetProvider {

    override val ruleSetId: String = "koin-rules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                NoGetOutsideModuleDefinition(config),
            )
        )
    }
}
```

**Step 6: Verify all tests pass**

Run: `./gradlew test`
Expected: All tests PASS

**Step 7: Commit rule**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinition.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinitionTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: add NoGetOutsideModuleDefinition rule"
```

---

## Task 4: Service Locator Rule 2 - NoInjectDelegate

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoInjectDelegate.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoInjectDelegateTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write failing test**

File: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoInjectDelegateTest.kt`

```kotlin
package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class NoInjectDelegateTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports inject() delegate usage`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyService : KoinComponent {
                val repo: Repository by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("inject()")
    }

    @Test
    fun `does not report constructor injection`() {
        val code = """
            class MyService(private val repo: Repository)
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report regular by lazy`() {
        val code = """
            class MyService {
                val value: String by lazy { "test" }
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests NoInjectDelegateTest`
Expected: FAIL

**Step 3: Implement NoInjectDelegate rule**

File: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoInjectDelegate.kt`

```kotlin
package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

class NoInjectDelegate(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "NoInjectDelegate",
        severity = Severity.Warning,
        description = "Detects 'by inject()' property delegate usage. " +
                "This is a service locator pattern. Use constructor injection instead.",
        debt = Debt.TWENTY_MINS
    )

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)

        val delegate = property.delegate ?: return
        val delegateCall = delegate.expression as? org.jetbrains.kotlin.psi.KtCallExpression ?: return
        val callName = delegateCall.getCallNameExpression()?.text

        if (callName == "inject") {
            report(
                CodeSmell(
                    issue,
                    Entity.from(property),
                    "Avoid using 'by inject()' delegate. Use constructor injection instead."
                )
            )
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests NoInjectDelegateTest`
Expected: PASS (all 3 tests)

**Step 5: Register rule in RuleSetProvider**

Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

Add to listOf():
```kotlin
NoInjectDelegate(config),
```

**Step 6: Verify all tests pass**

Run: `./gradlew test`
Expected: All tests PASS

**Step 7: Commit rule**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoInjectDelegate.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoInjectDelegateTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: add NoInjectDelegate rule"
```

---

## Task 5: Service Locator Rule 3 - NoKoinComponentInterface

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinComponentInterface.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinComponentInterfaceTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write failing test**

File: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinComponentInterfaceTest.kt`

```kotlin
package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class NoKoinComponentInterfaceTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports KoinComponent in regular class`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class UserRepository : KoinComponent {
                fun getData() = get<ApiService>()
            }
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("KoinComponent")
    }

    @Test
    fun `reports KoinScopeComponent in regular class`() {
        val code = """
            import org.koin.core.component.KoinScopeComponent

            class MyService : KoinScopeComponent {
                override val scope = getKoin().createScope()
            }
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report when extending allowed super type`() {
        val config = io.gitlab.arturbosch.detekt.test.TestConfig(
            "allowedSuperTypes" to listOf("Application", "Activity")
        )

        val code = """
            import org.koin.core.component.KoinComponent

            class MainActivity : Activity(), KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(config)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report class without KoinComponent`() {
        val code = """
            class MyService(private val repo: Repository)
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests NoKoinComponentInterfaceTest`
Expected: FAIL

**Step 3: Implement NoKoinComponentInterface rule**

File: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinComponentInterface.kt`

```kotlin
package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass

class NoKoinComponentInterface(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "NoKoinComponentInterface",
        severity = Severity.Warning,
        description = "Detects KoinComponent or KoinScopeComponent implementation in classes " +
                "that are not framework entry points. Use constructor injection instead.",
        debt = Debt.TWENTY_MINS
    )

    private val allowedSuperTypes: List<String> by config(
        listOf(
            "Application",
            "Activity",
            "Fragment",
            "Service",
            "BroadcastReceiver",
            "ViewModel"
        )
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val superTypes = klass.superTypeListEntries.mapNotNull { it.text }
        val hasKoinComponent = superTypes.any {
            it.contains("KoinComponent") || it.contains("KoinScopeComponent")
        }

        if (!hasKoinComponent) return

        // Check if class extends an allowed super type
        val hasAllowedSuperType = superTypes.any { superType ->
            allowedSuperTypes.any { allowed -> superType.contains(allowed) }
        }

        if (!hasAllowedSuperType) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    "Class '${klass.name}' implements KoinComponent but is not a framework entry point. " +
                            "Use constructor injection instead."
                )
            )
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests NoKoinComponentInterfaceTest`
Expected: PASS (all 4 tests)

**Step 5: Register rule in RuleSetProvider**

Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

Add to listOf():
```kotlin
NoKoinComponentInterface(config),
```

**Step 6: Verify all tests pass**

Run: `./gradlew test`
Expected: All tests PASS

**Step 7: Commit rule**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinComponentInterface.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinComponentInterfaceTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: add NoKoinComponentInterface rule with configurable allowed super types"
```

---

## Task 6: Service Locator Rules 4-5 (NoGlobalContextAccess, NoKoinGetInApplication)

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGlobalContextAccess.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGlobalContextAccessTest.kt`
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplication.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplicationTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write test for NoGlobalContextAccess**

File: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGlobalContextAccessTest.kt`

```kotlin
package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class NoGlobalContextAccessTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports GlobalContext get() access`() {
        val code = """
            import org.koin.core.context.GlobalContext

            fun getService() {
                val koin = GlobalContext.get()
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("GlobalContext")
    }

    @Test
    fun `reports GlobalContext getKoinApplicationOrNull`() {
        val code = """
            import org.koin.core.context.GlobalContext

            val app = GlobalContext.getKoinApplicationOrNull()
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report startKoin usage`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    modules(appModule)
                }
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }
}
```

**Step 2: Implement NoGlobalContextAccess**

File: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGlobalContextAccess.kt`

```kotlin
package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class NoGlobalContextAccess(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "NoGlobalContextAccess",
        severity = Severity.Warning,
        description = "Detects direct access to GlobalContext.get() or KoinPlatformTools. " +
                "This is the most egregious service locator variant.",
        debt = Debt.TWENTY_MINS
    )

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val receiverText = (expression.receiverExpression as? KtNameReferenceExpression)?.text

        if (receiverText in setOf("GlobalContext", "KoinPlatformTools")) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Avoid direct access to $receiverText. Use dependency injection instead."
                )
            )
        }
    }
}
```

**Step 3: Write test for NoKoinGetInApplication**

File: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplicationTest.kt`

```kotlin
package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class NoKoinGetInApplicationTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports get() inside startKoin block`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    val service = get<MyService>()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("startKoin")
    }

    @Test
    fun `does not report modules() inside startKoin`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    modules(appModule)
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }
}
```

**Step 4: Implement NoKoinGetInApplication**

File: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplication.kt`

```kotlin
package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

class NoKoinGetInApplication(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "NoKoinGetInApplication",
        severity = Severity.Warning,
        description = "Detects get() or inject() calls inside startKoin or koinConfiguration blocks. " +
                "Use modules() to define dependencies instead.",
        debt = Debt.TEN_MINS
    )

    private var insideStartKoinBlock = false

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Track entering startKoin/koinConfiguration blocks
        if (callName in setOf("startKoin", "koinConfiguration")) {
            val wasInside = insideStartKoinBlock
            insideStartKoinBlock = true
            super.visitCallExpression(expression)
            insideStartKoinBlock = wasInside
            return
        }

        // Check for get()/inject() inside application blocks
        if (callName in setOf("get", "inject") && insideStartKoinBlock) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Avoid using $callName() inside startKoin/koinConfiguration blocks. " +
                            "Use modules() to define dependencies instead."
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
```

**Step 5: Run tests**

Run: `./gradlew test --tests NoGlobalContextAccessTest`
Run: `./gradlew test --tests NoKoinGetInApplicationTest`
Expected: All tests PASS

**Step 6: Register rules in RuleSetProvider**

Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

Add to listOf():
```kotlin
NoGlobalContextAccess(config),
NoKoinGetInApplication(config),
```

**Step 7: Verify all tests pass**

Run: `./gradlew test`
Expected: All tests PASS

**Step 8: Commit rules**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGlobalContextAccess.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGlobalContextAccessTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplication.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplicationTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: add NoGlobalContextAccess and NoKoinGetInApplication rules"
```

---

## Task 7: Module DSL Rule 6 - EmptyModule

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/EmptyModule.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/EmptyModuleTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write failing test**

File: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/EmptyModuleTest.kt`

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class EmptyModuleTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports empty module`() {
        val code = """
            import org.koin.dsl.module

            val emptyModule = module { }
        """.trimIndent()

        val findings = EmptyModule(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("empty")
    }

    @Test
    fun `does not report module with includes`() {
        val code = """
            import org.koin.dsl.module

            val featureModule = module {
                includes(networkModule)
            }
        """.trimIndent()

        val findings = EmptyModule(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report module with definitions`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { MyService() }
            }
        """.trimIndent()

        val findings = EmptyModule(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests EmptyModuleTest`
Expected: FAIL

**Step 3: Implement EmptyModule rule**

File: `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/EmptyModule.kt`

```kotlin
package io.github.krozov.detekt.koin.moduledsl

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

class EmptyModule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "EmptyModule",
        severity = Severity.Warning,
        description = "Detects Koin modules without any definitions or includes(). " +
                "Empty modules should be removed.",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.getCallNameExpression()?.text
        if (callName != "module") return

        // Get the lambda argument
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression
            ?: return

        val bodyExpression = lambda.bodyExpression
        val statements = bodyExpression?.statements ?: emptyList()

        if (statements.isEmpty()) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Module is empty. Remove it or add definitions/includes()."
                )
            )
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests EmptyModuleTest`
Expected: PASS (all 3 tests)

**Step 5: Register rule in RuleSetProvider**

Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

Add to listOf():
```kotlin
EmptyModule(config),
```

**Step 6: Verify all tests pass**

Run: `./gradlew test`
Expected: All tests PASS

**Step 7: Commit rule**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/EmptyModule.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/EmptyModuleTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: add EmptyModule rule"
```

---

## Task 8: Module DSL Rule 7 - SingleForNonSharedDependency

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/SingleForNonSharedDependency.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/SingleForNonSharedDependencyTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write failing test**

File: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/SingleForNonSharedDependencyTest.kt`

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class SingleForNonSharedDependencyTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports single for UseCase`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { GetUserUseCase(get()) }
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("UseCase")
    }

    @Test
    fun `reports singleOf for Mapper`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf

            val myModule = module {
                singleOf(::UserMapper)
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Mapper")
    }

    @Test
    fun `does not report factory for UseCase`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                factory { GetUserUseCase(get()) }
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `respects custom name patterns`() {
        val config = TestConfig(
            "namePatterns" to listOf(".*Command", ".*Handler")
        )

        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { ProcessOrderCommand() }
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(config)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests SingleForNonSharedDependencyTest`
Expected: FAIL

**Step 3: Implement SingleForNonSharedDependency rule**

File: `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/SingleForNonSharedDependency.kt`

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

class SingleForNonSharedDependency(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "SingleForNonSharedDependency",
        severity = Severity.Warning,
        description = "Detects single/singleOf for types that should not be singletons by naming convention. " +
                "Use factory/factoryOf instead.",
        debt = Debt.TEN_MINS
    )

    private val namePatterns: List<String> by config(
        listOf(".*UseCase", ".*Interactor", ".*Mapper")
    )

    private val patterns by lazy {
        namePatterns.map { it.toRegex() }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.getCallNameExpression()?.text
        if (callName !in setOf("single", "singleOf")) return

        val typeName = extractTypeName(expression) ?: return

        val matchedPattern = patterns.firstOrNull { it.matches(typeName) }
        if (matchedPattern != null) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Type '$typeName' matches pattern '${matchedPattern.pattern}' and should not be a singleton. " +
                            "Use factory/factoryOf instead."
                )
            )
        }
    }

    private fun extractTypeName(expression: KtCallExpression): String? {
        // For single { Type() }
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression

        if (lambda != null) {
            val bodyExpr = lambda.bodyExpression?.statements?.firstOrNull() as? KtCallExpression
            return bodyExpr?.getCallNameExpression()?.text
        }

        // For singleOf(::Type)
        val arg = expression.valueArguments.firstOrNull()?.getArgumentExpression()?.text
        return arg?.removePrefix("::")
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests SingleForNonSharedDependencyTest`
Expected: PASS (all 4 tests)

**Step 5: Register rule in RuleSetProvider**

Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

Add to listOf():
```kotlin
SingleForNonSharedDependency(config),
```

**Step 6: Verify all tests pass**

Run: `./gradlew test`
Expected: All tests PASS

**Step 7: Commit rule**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/SingleForNonSharedDependency.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/SingleForNonSharedDependencyTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: add SingleForNonSharedDependency rule with configurable patterns"
```

---

## Task 9: Module DSL Rules 8-9 (MissingScopedDependencyQualifier, DeprecatedKoinApi)

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/MissingScopedDependencyQualifier.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/MissingScopedDependencyQualifierTest.kt`
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/DeprecatedKoinApi.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/DeprecatedKoinApiTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write test for MissingScopedDependencyQualifier**

File: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/MissingScopedDependencyQualifierTest.kt`

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class MissingScopedDependencyQualifierTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports duplicate type definitions without qualifier`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { HttpClient() }
                single { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("duplicate")
    }

    @Test
    fun `does not report when qualifiers are used`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val myModule = module {
                single(named("cio")) { HttpClient() }
                single(named("okhttp")) { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report different types`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { ApiService() }
                single { DatabaseService() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }
}
```

**Step 2: Implement MissingScopedDependencyQualifier**

File: `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/MissingScopedDependencyQualifier.kt`

```kotlin
package io.github.krozov.detekt.koin.moduledsl

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

class MissingScopedDependencyQualifier(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "MissingScopedDependencyQualifier",
        severity = Severity.Warning,
        description = "Detects multiple definitions of the same type in one module without named() qualifier. " +
                "This leads to runtime DefinitionOverrideException.",
        debt = Debt.TEN_MINS
    )

    private val definitionsByModule = mutableMapOf<KtCallExpression, MutableList<TypeDefinition>>()

    data class TypeDefinition(
        val type: String,
        val hasQualifier: Boolean,
        val expression: KtCallExpression
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Track module definitions
        if (callName == "module") {
            definitionsByModule[expression] = mutableListOf()
            super.visitCallExpression(expression)
            checkForDuplicates(expression)
            return
        }

        // Track definitions inside module
        if (callName in setOf("single", "factory", "scoped", "viewModel", "worker")) {
            val moduleCall = findParentModule(expression)
            if (moduleCall != null) {
                val typeName = extractTypeName(expression)
                val hasQualifier = hasQualifierArgument(expression)

                if (typeName != null) {
                    definitionsByModule[moduleCall]?.add(
                        TypeDefinition(typeName, hasQualifier, expression)
                    )
                }
            }
        }

        super.visitCallExpression(expression)
    }

    private fun checkForDuplicates(moduleCall: KtCallExpression) {
        val definitions = definitionsByModule[moduleCall] ?: return

        val grouped = definitions.groupBy { it.type }
        grouped.forEach { (type, defs) ->
            if (defs.size > 1 && defs.any { !it.hasQualifier }) {
                defs.first { !it.hasQualifier }.expression.let { expr ->
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(expr),
                            "Multiple definitions of type '$type' found without qualifiers. " +
                                    "Use named() to distinguish them."
                        )
                    )
                }
            }
        }
    }

    private fun findParentModule(expression: KtCallExpression): KtCallExpression? {
        var parent = expression.parent
        while (parent != null) {
            if (parent is KtCallExpression && parent.getCallNameExpression()?.text == "module") {
                return parent
            }
            parent = parent.parent
        }
        return null
    }

    private fun extractTypeName(expression: KtCallExpression): String? {
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression

        if (lambda != null) {
            val bodyExpr = lambda.bodyExpression?.statements?.firstOrNull() as? KtCallExpression
            return bodyExpr?.getCallNameExpression()?.text
        }

        val arg = expression.valueArguments.firstOrNull()?.getArgumentExpression()?.text
        return arg?.removePrefix("::")
    }

    private fun hasQualifierArgument(expression: KtCallExpression): Boolean {
        return expression.valueArguments.any { arg ->
            val argText = arg.getArgumentExpression()?.text ?: ""
            argText.contains("named(") || argText.contains("qualifier(")
        }
    }
}
```

**Step 3: Write test for DeprecatedKoinApi**

File: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/DeprecatedKoinApiTest.kt`

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class DeprecatedKoinApiTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports checkModules usage`() {
        val code = """
            import org.koin.test.check.checkModules

            fun test() {
                checkModules { }
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("verify()")
    }

    @Test
    fun `reports koinNavViewModel usage`() {
        val code = """
            val vm = koinNavViewModel<MyViewModel>()
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("koinViewModel()")
    }

    @Test
    fun `does not report current API`() {
        val code = """
            import org.koin.test.verify.verify

            fun test() {
                verify { }
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }
}
```

**Step 4: Implement DeprecatedKoinApi**

File: `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/DeprecatedKoinApi.kt`

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

class DeprecatedKoinApi(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "DeprecatedKoinApi",
        severity = Severity.Warning,
        description = "Detects usage of APIs deprecated in Koin 4.x with suggested replacements.",
        debt = Debt.FIVE_MINS
    )

    private val deprecations = mapOf(
        "checkModules" to "verify()",
        "KoinContext" to "KoinApplication",
        "KoinAndroidContext" to "KoinApplication",
        "koinNavViewModel" to "koinViewModel()",
        "stateViewModel" to "viewModel()"
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.getCallNameExpression()?.text ?: return
        val replacement = deprecations[callName]

        if (replacement != null) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "'$callName' is deprecated in Koin 4.x. Use '$replacement' instead."
                )
            )
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        // Check for Application.koin (Ktor)
        val selectorText = expression.selectorExpression?.text
        if (selectorText == "koin") {
            val receiverType = (expression.receiverExpression as? KtNameReferenceExpression)?.text
            if (receiverType == "Application") {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        "'Application.koin' is deprecated in Koin 4.x (Ktor). Use 'Application.koinModules()' instead."
                    )
                )
            }
        }
    }
}
```

**Step 5: Run tests**

Run: `./gradlew test --tests MissingScopedDependencyQualifierTest`
Run: `./gradlew test --tests DeprecatedKoinApiTest`
Expected: All tests PASS

**Step 6: Register rules in RuleSetProvider**

Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

Add to listOf():
```kotlin
MissingScopedDependencyQualifier(config),
DeprecatedKoinApi(config),
```

**Step 7: Verify all tests pass**

Run: `./gradlew test`
Expected: All tests PASS

**Step 8: Commit rules**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/MissingScopedDependencyQualifier.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/MissingScopedDependencyQualifierTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/DeprecatedKoinApi.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/DeprecatedKoinApiTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: add MissingScopedDependencyQualifier and DeprecatedKoinApi rules"
```

---

## Task 10: Module DSL Rule 10 - ModuleIncludesOrganization

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/ModuleIncludesOrganization.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/ModuleIncludesOrganizationTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write failing test**

File: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/ModuleIncludesOrganizationTest.kt`

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class ModuleIncludesOrganizationTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports module with too many includes and definitions`() {
        val code = """
            import org.koin.dsl.module

            val appModule = module {
                includes(networkModule, dbModule, featureAModule, featureBModule)
                single { AppConfig() }
                factory { Logger() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("God Module")
    }

    @Test
    fun `does not report module with few includes and definitions`() {
        val code = """
            import org.koin.dsl.module

            val appModule = module {
                includes(networkModule, dbModule)
                single { AppConfig() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report module with only includes`() {
        val code = """
            import org.koin.dsl.module

            val appModule = module {
                includes(networkModule, dbModule, featureAModule, featureBModule, featureCModule)
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `respects custom threshold`() {
        val config = TestConfig("maxIncludesWithDefinitions" to 5)

        val code = """
            import org.koin.dsl.module

            val appModule = module {
                includes(a, b, c, d, e, f)
                single { Config() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(config)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests ModuleIncludesOrganizationTest`
Expected: FAIL

**Step 3: Implement ModuleIncludesOrganization rule**

File: `src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/ModuleIncludesOrganization.kt`

```kotlin
package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

class ModuleIncludesOrganization(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "ModuleIncludesOrganization",
        severity = Severity.Style,
        description = "Detects modules that mix many includes() with direct definitions. " +
                "Consider extracting definitions to separate modules.",
        debt = Debt.TEN_MINS
    )

    private val maxIncludesWithDefinitions: Int by config(3)

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text
        if (callName != "module") {
            super.visitCallExpression(expression)
            return
        }

        // Get lambda body
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? org.jetbrains.kotlin.psi.KtLambdaExpression
            ?: return

        val statements = lambda.bodyExpression?.statements ?: emptyList()

        var includesCount = 0
        var hasDefinitions = false

        statements.forEach { statement ->
            if (statement is KtCallExpression) {
                val stmtCallName = statement.getCallNameExpression()?.text
                when (stmtCallName) {
                    "includes" -> {
                        // Count arguments in includes()
                        includesCount += statement.valueArguments.size
                    }
                    in setOf("single", "factory", "scoped", "viewModel", "worker") -> {
                        hasDefinitions = true
                    }
                }
            }
        }

        if (hasDefinitions && includesCount > maxIncludesWithDefinitions) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Module has $includesCount includes() and direct definitions. " +
                            "This is a sign of a \"God Module\". Consider extracting definitions to a separate module."
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests ModuleIncludesOrganizationTest`
Expected: PASS (all 4 tests)

**Step 5: Register rule in RuleSetProvider**

Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

Add to listOf():
```kotlin
ModuleIncludesOrganization(config),
```

**Step 6: Verify all tests pass**

Run: `./gradlew test`
Expected: All tests PASS

**Step 7: Commit rule**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/moduledsl/ModuleIncludesOrganization.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/ModuleIncludesOrganizationTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: add ModuleIncludesOrganization rule with configurable threshold"
```

---

## Task 11: Scope Management Rules 11-12 (MissingScopeClose, ScopedDependencyOutsideScopeBlock)

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/scope/MissingScopeClose.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/scope/MissingScopeCloseTest.kt`
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/scope/ScopedDependencyOutsideScopeBlock.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/scope/ScopedDependencyOutsideScopeBlockTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write test for MissingScopeClose**

File: `src/test/kotlin/io/github/krozov/detekt/koin/scope/MissingScopeCloseTest.kt`

```kotlin
package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class MissingScopeCloseTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports class with createScope but no close`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class SessionManager : KoinComponent {
                val scope = getKoin().createScope("session")
                fun getService() = scope.get<SessionService>()
            }
        """.trimIndent()

        val findings = MissingScopeClose(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("close()")
    }

    @Test
    fun `does not report class with createScope and close`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class SessionManager : KoinComponent {
                val scope = getKoin().createScope("session")
                fun getService() = scope.get<SessionService>()
                fun destroy() { scope.close() }
            }
        """.trimIndent()

        val findings = MissingScopeClose(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports getOrCreateScope without close`() {
        val code = """
            class Manager {
                val scope = koin.getOrCreateScope("id")
            }
        """.trimIndent()

        val findings = MissingScopeClose(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
    }
}
```

**Step 2: Implement MissingScopeClose**

File: `src/main/kotlin/io/github/krozov/detekt/koin/scope/MissingScopeClose.kt`

```kotlin
package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

class MissingScopeClose(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "MissingScopeClose",
        severity = Severity.Warning,
        description = "Detects classes that create or obtain a Scope but never call scope.close(). " +
                "This can lead to memory leaks.",
        debt = Debt.TEN_MINS
    )

    private val classesWithScopeCreation = mutableSetOf<KtClass>()
    private val classesWithScopeClose = mutableSetOf<KtClass>()

    override fun visitClass(klass: KtClass) {
        classesWithScopeCreation.clear()
        classesWithScopeClose.clear()

        super.visitClass(klass)

        if (klass in classesWithScopeCreation && klass !in classesWithScopeClose) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    "Class '${klass.name}' creates a Scope but never calls close(). This may cause memory leaks."
                )
            )
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val selectorCall = expression.selectorExpression as? org.jetbrains.kotlin.psi.KtCallExpression
        val callName = selectorCall?.getCallNameExpression()?.text

        val containingClass = expression.parents.filterIsInstance<KtClass>().firstOrNull()

        when (callName) {
            "createScope", "getOrCreateScope" -> {
                containingClass?.let { classesWithScopeCreation.add(it) }
            }
            "close" -> {
                // Check if receiver is 'scope'
                val receiverText = expression.receiverExpression.text
                if (receiverText.contains("scope")) {
                    containingClass?.let { classesWithScopeClose.add(it) }
                }
            }
        }
    }

    private val org.jetbrains.kotlin.psi.KtElement.parents: Sequence<org.jetbrains.kotlin.psi.PsiElement>
        get() = generateSequence(parent) { it.parent }
}
```

**Step 3: Write test for ScopedDependencyOutsideScopeBlock**

File: `src/test/kotlin/io/github/krozov/detekt/koin/scope/ScopedDependencyOutsideScopeBlockTest.kt`

```kotlin
package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class ScopedDependencyOutsideScopeBlockTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports scoped outside scope block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                scoped { UserSession() }
            }
        """.trimIndent()

        val findings = ScopedDependencyOutsideScopeBlock(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("scope")
    }

    @Test
    fun `does not report scoped inside scope block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                scope<MainActivity> {
                    scoped { UserSession() }
                }
            }
        """.trimIndent()

        val findings = ScopedDependencyOutsideScopeBlock(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report scoped inside activityScope`() {
        val code = """
            import org.koin.androidx.scope.activityScope

            val myModule = module {
                activityScope {
                    scoped { Presenter() }
                }
            }
        """.trimIndent()

        val findings = ScopedDependencyOutsideScopeBlock(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }
}
```

**Step 4: Implement ScopedDependencyOutsideScopeBlock**

File: `src/main/kotlin/io/github/krozov/detekt/koin/scope/ScopedDependencyOutsideScopeBlock.kt`

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

class ScopedDependencyOutsideScopeBlock(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "ScopedDependencyOutsideScopeBlock",
        severity = Severity.Warning,
        description = "Detects scoped {} or scopedOf() outside a scope {} / activityScope {} block. " +
                "Scoped dependencies must be defined within a scope block.",
        debt = Debt.TEN_MINS
    )

    private var insideScopeBlock = false
    private val scopeBlockFunctions = setOf("scope", "activityScope", "fragmentScope", "viewModelScope", "requestScope")

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Track entering scope blocks
        if (callName in scopeBlockFunctions) {
            val wasInside = insideScopeBlock
            insideScopeBlock = true
            super.visitCallExpression(expression)
            insideScopeBlock = wasInside
            return
        }

        // Check for scoped definitions outside scope blocks
        if (callName in setOf("scoped", "scopedOf") && !insideScopeBlock) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "'$callName' must be defined inside a scope {} block (scope, activityScope, fragmentScope, etc.)."
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
```

**Step 5: Run tests**

Run: `./gradlew test --tests MissingScopeCloseTest`
Run: `./gradlew test --tests ScopedDependencyOutsideScopeBlockTest`
Expected: All tests PASS

**Step 6: Register rules in RuleSetProvider**

Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

Add to listOf():
```kotlin
MissingScopeClose(config),
ScopedDependencyOutsideScopeBlock(config),
```

**Step 7: Verify all tests pass**

Run: `./gradlew test`
Expected: All tests PASS

**Step 8: Commit rules**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/scope/MissingScopeClose.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/MissingScopeCloseTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/scope/ScopedDependencyOutsideScopeBlock.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/ScopedDependencyOutsideScopeBlockTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: add MissingScopeClose and ScopedDependencyOutsideScopeBlock rules"
```

---

## Task 12: Scope Management Rules 13-14 (FactoryInScopeBlock, KtorRequestScopeMisuse)

**Files:**
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/scope/FactoryInScopeBlock.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/scope/FactoryInScopeBlockTest.kt`
- Create: `src/main/kotlin/io/github/krozov/detekt/koin/scope/KtorRequestScopeMisuse.kt`
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/scope/KtorRequestScopeMisuseTest.kt`
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

**Step 1: Write test for FactoryInScopeBlock**

File: `src/test/kotlin/io/github/krozov/detekt/koin/scope/FactoryInScopeBlockTest.kt`

```kotlin
package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class FactoryInScopeBlockTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports factory inside scope block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                scope<MyActivity> {
                    factory { Presenter(get()) }
                }
            }
        """.trimIndent()

        val findings = FactoryInScopeBlock(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("factory")
    }

    @Test
    fun `reports factoryOf inside scope block`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.factoryOf

            val myModule = module {
                scope<MyActivity> {
                    factoryOf(::MyPresenter)
                }
            }
        """.trimIndent()

        val findings = FactoryInScopeBlock(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report scoped inside scope block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                scope<MyActivity> {
                    scoped { Presenter(get()) }
                }
            }
        """.trimIndent()

        val findings = FactoryInScopeBlock(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report factory outside scope block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                factory { UseCase() }
            }
        """.trimIndent()

        val findings = FactoryInScopeBlock(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }
}
```

**Step 2: Implement FactoryInScopeBlock**

File: `src/main/kotlin/io/github/krozov/detekt/koin/scope/FactoryInScopeBlock.kt`

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

class FactoryInScopeBlock(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "FactoryInScopeBlock",
        severity = Severity.Style,
        description = "Detects factory {} or factoryOf() inside scope {} blocks. " +
                "Factory creates a new instance on every call regardless of scope, which may be unintended.",
        debt = Debt.FIVE_MINS
    )

    private var insideScopeBlock = false
    private val scopeBlockFunctions = setOf("scope", "activityScope", "fragmentScope", "viewModelScope", "requestScope")

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Track entering scope blocks
        if (callName in scopeBlockFunctions) {
            val wasInside = insideScopeBlock
            insideScopeBlock = true
            super.visitCallExpression(expression)
            insideScopeBlock = wasInside
            return
        }

        // Check for factory inside scope blocks
        if (callName in setOf("factory", "factoryOf") && insideScopeBlock) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "'$callName' inside a scope block may be unintended. " +
                            "Factory creates new instances on every call regardless of scope. Consider using scoped {} instead."
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
```

**Step 3: Write test for KtorRequestScopeMisuse**

File: `src/test/kotlin/io/github/krozov/detekt/koin/scope/KtorRequestScopeMisuseTest.kt`

```kotlin
package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class KtorRequestScopeMisuseTest(private val env: io.gitlab.arturbosch.detekt.test.KotlinCoreEnvironment) {

    @Test
    fun `reports single inside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    single { RequestLogger() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("singleton")
    }

    @Test
    fun `reports singleOf inside requestScope`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf

            val myModule = module {
                requestScope {
                    singleOf(::RequestHandler)
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report scoped inside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    scoped { RequestLogger() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report single outside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { Logger() }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }
}
```

**Step 4: Implement KtorRequestScopeMisuse**

File: `src/main/kotlin/io/github/krozov/detekt/koin/scope/KtorRequestScopeMisuse.kt`

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

class KtorRequestScopeMisuse(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "KtorRequestScopeMisuse",
        severity = Severity.Warning,
        description = "Detects single {} or singleOf() inside requestScope {} in Ktor. " +
                "Singleton in a request scope is semantically incorrect.",
        debt = Debt.TEN_MINS
    )

    private var insideRequestScope = false

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Track entering requestScope blocks
        if (callName == "requestScope") {
            val wasInside = insideRequestScope
            insideRequestScope = true
            super.visitCallExpression(expression)
            insideRequestScope = wasInside
            return
        }

        // Check for single inside requestScope
        if (callName in setOf("single", "singleOf") && insideRequestScope) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "'$callName' inside requestScope is semantically incorrect. " +
                            "A singleton should not be scoped to a request. Use scoped {} instead."
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
```

**Step 5: Run tests**

Run: `./gradlew test --tests FactoryInScopeBlockTest`
Run: `./gradlew test --tests KtorRequestScopeMisuseTest`
Expected: All tests PASS

**Step 6: Register rules in RuleSetProvider**

Modify: `src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt`

Add to listOf():
```kotlin
FactoryInScopeBlock(config),
KtorRequestScopeMisuse(config),
```

**Step 7: Verify all tests pass**

Run: `./gradlew test`
Expected: All tests PASS

**Step 8: Commit rules**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/scope/FactoryInScopeBlock.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/FactoryInScopeBlockTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/scope/KtorRequestScopeMisuse.kt
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/KtorRequestScopeMisuseTest.kt
git add src/main/kotlin/io/github/krozov/detekt/koin/KoinRuleSetProvider.kt
git commit -m "feat: add FactoryInScopeBlock and KtorRequestScopeMisuse rules"
```

---

## Task 13: Default Configuration File

**Files:**
- Create: `src/main/resources/config/config.yml`

**Step 1: Create default config.yml**

File: `src/main/resources/config/config.yml`

```yaml
koin-rules:
  active: true

  # Service Locator Rules
  NoGetOutsideModuleDefinition:
    active: true

  NoInjectDelegate:
    active: true

  NoKoinComponentInterface:
    active: true
    allowedSuperTypes:
      - 'Application'
      - 'Activity'
      - 'Fragment'
      - 'Service'
      - 'BroadcastReceiver'
      - 'ViewModel'

  NoGlobalContextAccess:
    active: true

  NoKoinGetInApplication:
    active: true

  # Module DSL Rules
  EmptyModule:
    active: true

  SingleForNonSharedDependency:
    active: true
    namePatterns:
      - '.*UseCase'
      - '.*Interactor'
      - '.*Mapper'

  MissingScopedDependencyQualifier:
    active: true

  DeprecatedKoinApi:
    active: true

  ModuleIncludesOrganization:
    active: false
    maxIncludesWithDefinitions: 3

  # Scope Management Rules
  MissingScopeClose:
    active: true

  ScopedDependencyOutsideScopeBlock:
    active: true

  FactoryInScopeBlock:
    active: false

  KtorRequestScopeMisuse:
    active: true
```

**Step 2: Verify build succeeds**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit configuration**

```bash
git add src/main/resources/config/config.yml
git commit -m "feat: add default configuration file for Koin rules"
```

---

## Task 14: README and Documentation

**Files:**
- Create: `README.md`
- Create: `docs/rules.md`

**Step 1: Create README.md**

File: `README.md`

```markdown
# detekt-rules-koin

[![Maven Central](https://img.shields.io/maven-central/v/io.github.krozov/detekt-rules-koin)](https://search.maven.org/artifact/io.github.krozov/detekt-rules-koin)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Detekt 1.x extension library with 14 rules for Koin 4.x to enforce best practices and catch common anti-patterns via static analysis.

## Features

✅ **14 Rules** across 3 categories: Service Locator, Module DSL, Scope Management
✅ **Zero runtime overhead** — pure syntactic analysis via Kotlin PSI
✅ **No Koin dependency** in consumer projects
✅ **Configurable** — customize rules via detekt config
✅ **Production-ready** — comprehensive test coverage

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    detektPlugins("io.github.krozov:detekt-rules-koin:0.1.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    detektPlugins 'io.github.krozov:detekt-rules-koin:0.1.0'
}
```

## Usage

After adding the plugin, Detekt will automatically discover and apply the Koin rules.

Run analysis:

```bash
./gradlew detekt
```

## Rules

### Service Locator Rules (5)

| Rule | Severity | Default |
|------|----------|---------|
| `NoGetOutsideModuleDefinition` | Warning | Active |
| `NoInjectDelegate` | Warning | Active |
| `NoKoinComponentInterface` | Warning | Active |
| `NoGlobalContextAccess` | Warning | Active |
| `NoKoinGetInApplication` | Warning | Active |

### Module DSL Rules (5)

| Rule | Severity | Default |
|------|----------|---------|
| `EmptyModule` | Warning | Active |
| `SingleForNonSharedDependency` | Warning | Active |
| `MissingScopedDependencyQualifier` | Warning | Active |
| `DeprecatedKoinApi` | Warning | Active |
| `ModuleIncludesOrganization` | Style | Inactive |

### Scope Management Rules (4)

| Rule | Severity | Default |
|------|----------|---------|
| `MissingScopeClose` | Warning | Active |
| `ScopedDependencyOutsideScopeBlock` | Warning | Active |
| `FactoryInScopeBlock` | Style | Inactive |
| `KtorRequestScopeMisuse` | Warning | Active |

📖 **[Complete Rule Documentation](docs/rules.md)**

## Configuration

Create or update `.detekt.yml`:

```yaml
koin-rules:
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes:
      - 'Application'
      - 'Activity'
      - 'Fragment'

  SingleForNonSharedDependency:
    active: true
    namePatterns:
      - '.*UseCase'
      - '.*Command'

  ModuleIncludesOrganization:
    active: true
    maxIncludesWithDefinitions: 5
```

## Requirements

- Kotlin 2.0+
- Detekt 1.23.8+
- Targets Koin 4.x (all platforms: Core, Android, Compose, Ktor)

## License

```
Copyright 2026 Kirill Rozov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

Contributions are welcome! Please open an issue or pull request.

## Roadmap

- v0.2.0: Architecture rules (Koin imports in domain layer)
- v0.3.0: Koin Annotations support
- v1.0.0: API stabilization, Detekt 2.x compatibility
```

**Step 2: Create docs/rules.md**

File: `docs/rules.md`

```markdown
# Koin Rules Documentation

Complete reference for all 14 Detekt rules for Koin.

---

## Service Locator Rules

### NoGetOutsideModuleDefinition

**Severity:** Warning
**Active by default:** Yes

Detects `get()` / `getOrNull()` / `getAll()` calls outside Koin module definition blocks.

❌ **Bad:**
```kotlin
class MyRepository : KoinComponent {
    val api = get<ApiService>()
}
```

✅ **Good:**
```kotlin
module {
    single { MyRepository(get()) }
}
```

---

### NoInjectDelegate

**Severity:** Warning
**Active by default:** Yes

Detects `by inject()` property delegate usage.

❌ **Bad:**
```kotlin
class MyService : KoinComponent {
    val repo: Repository by inject()
}
```

✅ **Good:**
```kotlin
class MyService(private val repo: Repository)
```

---

### NoKoinComponentInterface

**Severity:** Warning
**Active by default:** Yes
**Configurable:** Yes

Detects `KoinComponent` / `KoinScopeComponent` in non-framework classes.

❌ **Bad:**
```kotlin
class UserRepository : KoinComponent
```

✅ **Good:**
```kotlin
class MainActivity : Activity(), KoinComponent
```

**Configuration:**
```yaml
NoKoinComponentInterface:
  allowedSuperTypes:
    - 'Application'
    - 'ViewModel'
```

---

### NoGlobalContextAccess

**Severity:** Warning
**Active by default:** Yes

Detects direct `GlobalContext.get()` or `KoinPlatformTools` access.

❌ **Bad:**
```kotlin
val koin = GlobalContext.get()
```

---

### NoKoinGetInApplication

**Severity:** Warning
**Active by default:** Yes

Detects `get()` / `inject()` inside `startKoin {}` blocks.

❌ **Bad:**
```kotlin
startKoin {
    val service = get<MyService>()
}
```

---

## Module DSL Rules

### EmptyModule

**Severity:** Warning
**Active by default:** Yes

Detects modules without definitions or includes.

❌ **Bad:**
```kotlin
val emptyModule = module { }
```

---

### SingleForNonSharedDependency

**Severity:** Warning
**Active by default:** Yes
**Configurable:** Yes

Detects `single {}` for types that shouldn't be singletons.

❌ **Bad:**
```kotlin
module {
    single { GetUserUseCase(get()) }
}
```

✅ **Good:**
```kotlin
module {
    factory { GetUserUseCase(get()) }
}
```

**Configuration:**
```yaml
SingleForNonSharedDependency:
  namePatterns:
    - '.*UseCase'
    - '.*Command'
```

---

### MissingScopedDependencyQualifier

**Severity:** Warning
**Active by default:** Yes

Detects duplicate type definitions without qualifiers.

❌ **Bad:**
```kotlin
module {
    single { HttpClient(CIO) }
    single { HttpClient(OkHttp) }
}
```

✅ **Good:**
```kotlin
module {
    single(named("cio")) { HttpClient(CIO) }
    single(named("okhttp")) { HttpClient(OkHttp) }
}
```

---

### DeprecatedKoinApi

**Severity:** Warning
**Active by default:** Yes

Detects deprecated Koin 4.x APIs.

| Deprecated | Replacement |
|------------|-------------|
| `checkModules()` | `verify()` |
| `koinNavViewModel()` | `koinViewModel()` |
| `stateViewModel()` | `viewModel()` |

---

### ModuleIncludesOrganization

**Severity:** Style
**Active by default:** No
**Configurable:** Yes

Detects "God Modules" with many includes + definitions.

**Configuration:**
```yaml
ModuleIncludesOrganization:
  active: true
  maxIncludesWithDefinitions: 3
```

---

## Scope Management Rules

### MissingScopeClose

**Severity:** Warning
**Active by default:** Yes

Detects scopes without `close()` calls (memory leak).

❌ **Bad:**
```kotlin
class SessionManager : KoinComponent {
    val scope = getKoin().createScope("session")
}
```

✅ **Good:**
```kotlin
class SessionManager : KoinComponent {
    val scope = getKoin().createScope("session")
    fun destroy() { scope.close() }
}
```

---

### ScopedDependencyOutsideScopeBlock

**Severity:** Warning
**Active by default:** Yes

Detects `scoped {}` outside `scope {}` blocks.

❌ **Bad:**
```kotlin
module {
    scoped { UserSession() }
}
```

✅ **Good:**
```kotlin
module {
    scope<MainActivity> {
        scoped { UserSession() }
    }
}
```

---

### FactoryInScopeBlock

**Severity:** Style
**Active by default:** No

Detects `factory {}` inside `scope {}` blocks (possibly unintended).

⚠️ **Warning:**
```kotlin
module {
    scope<MyActivity> {
        factory { Presenter() }
    }
}
```

---

### KtorRequestScopeMisuse

**Severity:** Warning
**Active by default:** Yes

Detects `single {}` inside `requestScope {}` (Ktor).

❌ **Bad:**
```kotlin
module {
    requestScope {
        single { RequestLogger() }
    }
}
```

✅ **Good:**
```kotlin
module {
    requestScope {
        scoped { RequestLogger() }
    }
}
```
```

**Step 3: Create docs directory**

Run: `mkdir -p docs`

**Step 4: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit documentation**

```bash
git add README.md docs/rules.md
git commit -m "docs: add README and complete rule documentation"
```

---

## Task 15: Final Verification and Summary

**Step 1: Run full test suite**

Run: `./gradlew test --info`
Expected: All 60+ tests PASS

**Step 2: Run detekt on itself**

Run: `./gradlew detekt`
Expected: No issues or only style issues

**Step 3: Build final artifact**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Verify all 14 rules are registered**

Run: `./gradlew jar && jar tf build/libs/detekt-rules-koin-0.1.0-SNAPSHOT.jar | grep ".class"`
Expected: See all rule classes

**Step 5: Create summary commit**

```bash
git log --oneline
git commit --allow-empty -m "feat: complete detekt-rules-koin 0.1.0 implementation

Summary:
- 14 rules across 3 categories (Service Locator, Module DSL, Scope Management)
- 12 rules active by default
- Full test coverage (60+ tests)
- Complete documentation (README + rules.md)
- Default configuration file
- Ready for Maven Central publication
"
```

---

## Completion

✅ All 14 rules implemented
✅ Comprehensive test coverage
✅ Default configuration file
✅ Documentation complete
✅ Ready for v0.1.0 release

**Next Steps:**
1. Set up GitHub Actions CI
2. Configure Maven Central publishing
3. Create first release tag
4. Submit to Detekt Marketplace

---

**Implementation complete!** 🎉
