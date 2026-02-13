# Test Coverage Strategy Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Achieve 98%+ line coverage and 90%+ branch coverage through integration tests and comprehensive edge cases

**Architecture:** Add ServiceLoader integration tests, systematically add edge cases for all 14 rules based on coverage gap analysis, update documentation and CI/CD thresholds

**Tech Stack:** Kotlin 2.0, JUnit 5, AssertJ, Detekt Test API, Kover

**Design Document:** `docs/plans/2026-02-13-test-coverage-strategy-design.md`

---

## Phase 1: Coverage Analysis & Gap Identification

### Task 1: Generate Current Coverage Report

**Files:**
- Analyze: `build/reports/kover/html/index.html` (generated)

**Step 1: Clean and regenerate coverage report**

```bash
./gradlew clean koverHtmlReport
```

Expected: Report generated at `build/reports/kover/html/index.html`

**Step 2: Open coverage report**

```bash
open build/reports/kover/html/index.html
# Or manually navigate to file in browser
```

**Step 3: Document uncovered lines**

Create checklist of uncovered code per rule:
- Click through each rule class
- Note line numbers with red (uncovered) or yellow (partially covered) highlighting
- Note branch coverage percentage per rule
- Identify patterns: which branches/conditions are missed

**Step 4: Create gap analysis file**

Create: `docs/plans/coverage-gaps-2026-02-13.md`

```markdown
# Coverage Gap Analysis

## Current Metrics
- Line Coverage: 94.67%
- Branch Coverage: ~55%

## Gaps by Rule

### NoKoinComponentInterface
- Uncovered lines: [list]
- Missing branches: [list]

### EmptyModule
- Uncovered lines: [list]
- Missing branches: [list]

[Continue for all 14 rules]

## Priority Edge Cases
1. [Most critical gap]
2. [Second priority]
...
```

---

## Phase 2: Integration Tests

### Task 2: Create Integration Test Structure

**Files:**
- Create: `src/test/kotlin/io/github/krozov/detekt/koin/integration/KoinRulesIntegrationTest.kt`

**Step 1: Create integration package directory**

```bash
mkdir -p src/test/kotlin/io/github/krozov/detekt/koin/integration
```

**Step 2: Create test file with boilerplate**

Create: `src/test/kotlin/io/github/krozov/detekt/koin/integration/KoinRulesIntegrationTest.kt`

```kotlin
package io.github.krozov.detekt.koin.integration

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

class KoinRulesIntegrationTest {
    // Tests will go here
}
```

**Step 3: Verify file compiles**

```bash
./gradlew compileTestKotlin
```

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/integration/
git commit -m "test: create integration test structure

Prepare for ServiceLoader and E2E integration tests.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 3: ServiceLoader Discovery Test

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/integration/KoinRulesIntegrationTest.kt`

**Step 1: Write the failing test**

Add to `KoinRulesIntegrationTest.kt`:

```kotlin
@Test
fun `ServiceLoader can discover KoinRuleSetProvider`() {
    val providers = ServiceLoader.load(RuleSetProvider::class.java).toList()
    val koinProvider = providers.find { it is KoinRuleSetProvider }

    assertThat(koinProvider)
        .withFailMessage("KoinRuleSetProvider not discovered via ServiceLoader")
        .isNotNull()
}
```

**Step 2: Run test to verify it passes** (should already pass since META-INF exists)

```bash
./gradlew test --tests "*Integration*" --no-daemon
```

Expected: PASS (validates existing ServiceLoader setup)

**Step 3: Add validation of rule set properties**

Add to same test:

```kotlin
@Test
fun `ServiceLoader can discover KoinRuleSetProvider`() {
    val providers = ServiceLoader.load(RuleSetProvider::class.java).toList()
    val koinProvider = providers.find { it is KoinRuleSetProvider }

    assertThat(koinProvider)
        .withFailMessage("KoinRuleSetProvider not discovered via ServiceLoader")
        .isNotNull()

    val ruleSet = koinProvider!!.instance(Config.empty)

    assertThat(ruleSet.id).isEqualTo("koin-rules")
    assertThat(ruleSet.rules).hasSize(14)

    // Verify all rule names
    val ruleIds = ruleSet.rules.map { it.ruleId }
    assertThat(ruleIds).containsExactlyInAnyOrder(
        "NoGetOutsideModuleDefinition",
        "NoInjectDelegate",
        "NoKoinComponentInterface",
        "NoGlobalContextAccess",
        "NoKoinGetInApplication",
        "EmptyModule",
        "SingleForNonSharedDependency",
        "MissingScopedDependencyQualifier",
        "DeprecatedKoinApi",
        "ModuleIncludesOrganization",
        "MissingScopeClose",
        "ScopedDependencyOutsideScopeBlock",
        "FactoryInScopeBlock",
        "KtorRequestScopeMisuse"
    )
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew test --tests "*Integration*ServiceLoader*" --no-daemon
```

Expected: PASS

**Step 5: Commit**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/integration/KoinRulesIntegrationTest.kt
git commit -m "test: add ServiceLoader discovery integration test

Verifies that Detekt can discover KoinRuleSetProvider via
ServiceLoader and that all 14 rules are instantiated.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 4: End-to-End Analysis Test

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/integration/KoinRulesIntegrationTest.kt`

**Step 1: Write the failing test**

Add to `KoinRulesIntegrationTest.kt`:

```kotlin
@Test
fun `E2E analysis detects multiple violations in real code`() {
    val code = """
        package test

        import org.koin.core.component.KoinComponent
        import org.koin.core.component.get

        class MyRepository : KoinComponent {
            val apiService = get<ApiService>()
        }

        interface ApiService
    """.trimIndent()

    val provider = KoinRuleSetProvider()
    val ruleSet = provider.instance(Config.empty)

    // Use Detekt test API to lint code
    val findings = ruleSet.rules.flatMap { rule ->
        rule.lint(code)
    }

    // Should find 2 violations:
    // 1. NoKoinComponentInterface (MyRepository implements KoinComponent)
    // 2. NoGetOutsideModuleDefinition (get() outside module)
    assertThat(findings).hasSize(2)

    val ruleIds = findings.map { it.issue.id }
    assertThat(ruleIds).containsExactlyInAnyOrder(
        "NoKoinComponentInterface",
        "NoGetOutsideModuleDefinition"
    )
}
```

**Step 2: Add required imports**

At top of file:

```kotlin
import io.gitlab.arturbosch.detekt.test.lint
```

**Step 3: Run test to verify it passes**

```bash
./gradlew test --tests "*Integration*E2E*" --no-daemon
```

Expected: PASS

**Step 4: Commit**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/integration/KoinRulesIntegrationTest.kt
git commit -m "test: add E2E integration test for multiple violations

Verifies that rules work together to detect multiple violations
in realistic Kotlin code.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 5: Configuration Application Test

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/integration/KoinRulesIntegrationTest.kt`

**Step 1: Write the failing test**

Add to `KoinRulesIntegrationTest.kt`:

```kotlin
@Test
fun `custom configuration is applied to rules`() {
    val config = io.gitlab.arturbosch.detekt.test.TestConfig(
        "NoKoinComponentInterface" to mapOf(
            "allowedSuperTypes" to listOf("Application", "Activity", "CustomFramework")
        )
    )

    val provider = KoinRuleSetProvider()
    val ruleSet = provider.instance(config)

    val code = """
        import org.koin.core.component.KoinComponent

        class MyApp : CustomFramework(), KoinComponent
    """.trimIndent()

    val rule = ruleSet.rules.find { it.ruleId == "NoKoinComponentInterface" }
    assertThat(rule).isNotNull()

    val findings = rule!!.lint(code)

    // Should NOT report violation because CustomFramework is in allowedSuperTypes
    assertThat(findings).isEmpty()
}
```

**Step 2: Add TestConfig import**

```kotlin
import io.gitlab.arturbosch.detekt.test.TestConfig
```

**Step 3: Run test to verify it passes**

```bash
./gradlew test --tests "*Integration*configuration*" --no-daemon
```

Expected: PASS

**Step 4: Commit**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/integration/KoinRulesIntegrationTest.kt
git commit -m "test: add config application integration test

Verifies that custom .detekt.yml configuration is correctly
applied to rule instances.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Phase 3: Systematic Edge Cases

**Note:** Use coverage gap analysis from Task 1 to prioritize which edge cases to add first.

### Task 6: NoKoinComponentInterface Edge Cases

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinComponentInterfaceTest.kt`

**Step 1: Add generic types edge case**

Add test:

```kotlin
@Test
fun `does not report ViewModel with generic parameter`() {
    val code = """
        import org.koin.core.component.KoinComponent

        class MyViewModel<T> : ViewModel(), KoinComponent
    """.trimIndent()

    val findings = NoKoinComponentInterface(Config.empty).lint(code)

    assertThat(findings).isEmpty()
}
```

**Step 2: Run test**

```bash
./gradlew test --tests "NoKoinComponentInterfaceTest*generic*" --no-daemon
```

Expected: PASS (endsWith logic should handle this)

**Step 3: Add qualified name edge case**

Add test:

```kotlin
@Test
fun `recognizes fully qualified ComponentActivity`() {
    val code = """
        import org.koin.core.component.KoinComponent

        class MyActivity : androidx.activity.ComponentActivity(), KoinComponent
    """.trimIndent()

    val findings = NoKoinComponentInterface(Config.empty).lint(code)

    assertThat(findings).isEmpty()
}
```

**Step 4: Run test**

```bash
./gradlew test --tests "NoKoinComponentInterfaceTest*ComponentActivity*" --no-daemon
```

Expected: PASS

**Step 5: Add multiple inheritance edge case**

Add test:

```kotlin
@Test
fun `does not report Activity with multiple interfaces`() {
    val code = """
        import org.koin.core.component.KoinComponent

        class MyActivity : Activity(), SomeInterface, KoinComponent
    """.trimIndent()

    val findings = NoKoinComponentInterface(Config.empty).lint(code)

    assertThat(findings).isEmpty()
}
```

**Step 6: Run test**

```bash
./gradlew test --tests "NoKoinComponentInterfaceTest*multiple*" --no-daemon
```

Expected: PASS

**Step 7: Add companion object edge case**

Add test:

```kotlin
@Test
fun `reports KoinComponent in companion object`() {
    val code = """
        import org.koin.core.component.KoinComponent

        class MyClass {
            companion object : KoinComponent
        }
    """.trimIndent()

    val findings = NoKoinComponentInterface(Config.empty).lint(code)

    // Companion object is not a framework entry point
    assertThat(findings).hasSize(1)
}
```

**Step 8: Run test**

```bash
./gradlew test --tests "NoKoinComponentInterfaceTest*companion*" --no-daemon
```

Expected: May FAIL if companion objects not handled. If fails, need to update rule logic.

**Step 9: Commit edge cases**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinComponentInterfaceTest.kt
git commit -m "test: add edge cases for NoKoinComponentInterface

- Generic types: ViewModel<T>
- Qualified names: androidx.activity.ComponentActivity
- Multiple inheritance
- Companion objects

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 7: EmptyModule Edge Cases

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/EmptyModuleTest.kt`

**Step 1: Add whitespace-only edge case**

Add test:

```kotlin
@Test
fun `reports module with only whitespace`() {
    val code = """
        import org.koin.dsl.module

        val m = module {


        }
    """.trimIndent()

    val findings = EmptyModule(Config.empty).lint(code)

    assertThat(findings).hasSize(1)
}
```

**Step 2: Run test**

```bash
./gradlew test --tests "EmptyModuleTest*whitespace*" --no-daemon
```

Expected: PASS

**Step 3: Add comments-only edge case**

Add test:

```kotlin
@Test
fun `reports module with only comments`() {
    val code = """
        import org.koin.dsl.module

        val m = module {
            // TODO: Add definitions
            /* Work in progress */
        }
    """.trimIndent()

    val findings = EmptyModule(Config.empty).lint(code)

    assertThat(findings).hasSize(1)
}
```

**Step 4: Run test**

```bash
./gradlew test --tests "EmptyModuleTest*comments*" --no-daemon
```

Expected: PASS (comments don't count as statements)

**Step 5: Add multiline empty lambda edge case**

Add test:

```kotlin
@Test
fun `reports module with empty nested lambda`() {
    val code = """
        import org.koin.dsl.module

        val m = module {

        }
    """.trimIndent()

    val findings = EmptyModule(Config.empty).lint(code)

    assertThat(findings).hasSize(1)
}
```

**Step 6: Run test**

```bash
./gradlew test --tests "EmptyModuleTest" --no-daemon
```

Expected: All PASS

**Step 7: Commit**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/EmptyModuleTest.kt
git commit -m "test: add edge cases for EmptyModule

- Whitespace-only modules
- Comments-only modules
- Multiline empty lambdas

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 8: MissingScopeClose Edge Cases

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/scope/MissingScopeCloseTest.kt`

**Step 1: Add multiple scopes edge case**

Add test:

```kotlin
@Test
fun `reports class with multiple scopes without close`() {
    val code = """
        class MyClass {
            fun onCreate() {
                val scope1 = koin.createScope()
                val scope2 = koin.createScope()
                // Missing close() for both
            }
        }
    """.trimIndent()

    val findings = MissingScopeClose(Config.empty).lint(code)

    assertThat(findings).hasSize(1)  // Reports class once
}
```

**Step 2: Run test**

```bash
./gradlew test --tests "MissingScopeCloseTest*multiple*" --no-daemon
```

Expected: PASS

**Step 3: Add conditional scope creation edge case**

Add test:

```kotlin
@Test
fun `reports scope in conditional block without close`() {
    val code = """
        class MyClass {
            fun onCreate() {
                if (condition) {
                    val scope = koin.createScope()
                    // Missing close()
                }
            }
        }
    """.trimIndent()

    val findings = MissingScopeClose(Config.empty).lint(code)

    assertThat(findings).hasSize(1)
}
```

**Step 4: Run test**

```bash
./gradlew test --tests "MissingScopeCloseTest*conditional*" --no-daemon
```

Expected: PASS

**Step 5: Add nested class edge case**

Add test:

```kotlin
@Test
fun `detects scope in nested class without close`() {
    val code = """
        class Outer {
            inner class Inner {
                fun onCreate() {
                    val scope = koin.createScope()
                    // Missing close()
                }
            }
        }
    """.trimIndent()

    val findings = MissingScopeClose(Config.empty).lint(code)

    assertThat(findings).hasSize(1)
}
```

**Step 6: Run test**

```bash
./gradlew test --tests "MissingScopeCloseTest*nested*" --no-daemon
```

Expected: PASS

**Step 7: Commit**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/scope/MissingScopeCloseTest.kt
git commit -m "test: add edge cases for MissingScopeClose

- Multiple scopes in one class
- Conditional scope creation
- Nested classes with scopes

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 9: SingleForNonSharedDependency Edge Cases

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/SingleForNonSharedDependencyTest.kt`

**Step 1: Add custom suffix variations**

Add tests:

```kotlin
@Test
fun `reports single for Interactor`() {
    val code = """
        import org.koin.dsl.module

        val m = module {
            single { DataInteractor() }
        }
    """.trimIndent()

    val config = io.gitlab.arturbosch.detekt.test.TestConfig(
        "namePatterns" to listOf(".*Interactor", ".*Worker")
    )

    val findings = SingleForNonSharedDependency(config).lint(code)

    assertThat(findings).hasSize(1)
}

@Test
fun `reports single for Worker`() {
    val code = """
        import org.koin.dsl.module

        val m = module {
            single { BackgroundWorker() }
        }
    """.trimIndent()

    val config = io.gitlab.arturbosch.detekt.test.TestConfig(
        "namePatterns" to listOf(".*Interactor", ".*Worker")
    )

    val findings = SingleForNonSharedDependency(config).lint(code)

    assertThat(findings).hasSize(1)
}

@Test
fun `reports single for Handler`() {
    val code = """
        import org.koin.dsl.module

        val m = module {
            single { EventHandler() }
        }
    """.trimIndent()

    val config = io.gitlab.arturbosch.detekt.test.TestConfig(
        "namePatterns" to listOf(".*Handler")
    )

    val findings = SingleForNonSharedDependency(config).lint(code)

    assertThat(findings).hasSize(1)
}
```

**Step 2: Run tests**

```bash
./gradlew test --tests "SingleForNonSharedDependencyTest" --no-daemon
```

Expected: PASS

**Step 3: Commit**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/moduledsl/SingleForNonSharedDependencyTest.kt
git commit -m "test: add edge cases for SingleForNonSharedDependency

Test custom namePatterns: Interactor, Worker, Handler

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 10: Remaining Edge Cases (Systematic)

**Files:**
- Modify: Multiple test files based on coverage gaps

**Approach:** For each remaining rule (9 rules left), add 2-4 edge cases targeting uncovered branches.

**Priority order** (from coverage gap analysis):
1. NoGetOutsideModuleDefinition - nested lambdas, init blocks
2. MissingScopedDependencyQualifier - generics, same type multiple times
3. DeprecatedKoinApi - all deprecated methods
4. ModuleIncludesOrganization - boundary conditions
5. ScopedDependencyOutsideScopeBlock - various scope types
6. FactoryInScopeBlock - factoryOf variation
7. KtorRequestScopeMisuse - nested scopes
8. NoInjectDelegate - companion object, multiple delegates
9. NoGlobalContextAccess - all GlobalContext methods
10. NoKoinGetInApplication - nested blocks

**Template for each rule:**

```markdown
**Step 1: Read current test file**
**Step 2: Identify 2-4 uncovered branches from coverage report**
**Step 3: Write edge case tests**
**Step 4: Run tests: `./gradlew test --tests "<RuleName>Test" --no-daemon`**
**Step 5: Commit with descriptive message**
```

**Example for NoGetOutsideModuleDefinition:**

```kotlin
@Test
fun `reports get in init block`() {
    val code = """
        class MyRepo {
            init {
                val service = get<ApiService>()
            }
        }
    """.trimIndent()

    val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}

@Test
fun `reports get in property initializer`() {
    val code = """
        class MyRepo {
            val service = get<ApiService>()
        }
    """.trimIndent()

    val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}

@Test
fun `reports get in nested lambda`() {
    val code = """
        import org.koin.dsl.module

        val m = module {
            single {
                MyClass().apply {
                    val nested = get<Service>()
                }
            }
        }
    """.trimIndent()

    val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
    assertThat(findings).hasSize(1)
}
```

**Commit after each rule:**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/<category>/<RuleName>Test.kt
git commit -m "test: add edge cases for <RuleName>

- [Edge case 1]
- [Edge case 2]
- [Edge case 3]

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

**Repeat for all 9 remaining rules.**

---

## Phase 4: Coverage Validation

### Task 11: Verify Coverage Targets

**Files:**
- Analyze: `build/reports/kover/html/index.html`

**Step 1: Run tests and generate coverage**

```bash
./gradlew clean test koverHtmlReport
```

**Step 2: Open coverage report**

```bash
open build/reports/kover/html/index.html
```

**Step 3: Check metrics**

Verify:
- Line coverage: **≥ 98%** (currently 94.67%)
- Branch coverage: **≥ 90%** (currently ~55%)

**Step 4: If below target, identify remaining gaps**

Navigate to classes with < 98% coverage:
- Note specific uncovered lines
- Note uncovered branches
- Add targeted tests for those specific paths

**Step 5: Iterate**

Repeat: Add test → Run coverage → Check metrics

Until both targets met.

**Step 6: Document final metrics**

Update `docs/plans/coverage-gaps-2026-02-13.md`:

```markdown
## Final Metrics (2026-02-13)

- Line Coverage: XX.XX% ✅ (target: 98%)
- Branch Coverage: XX.XX% ✅ (target: 90%)
- Total Tests: XXX (started: 55)

## Remaining Gaps (if any)

[List any acceptable gaps with justification]
```

---

### Task 12: Update Kover Thresholds

**Files:**
- Modify: `build.gradle.kts`

**Step 1: Locate Kover configuration**

Find the `kover { }` block in `build.gradle.kts`.

**Step 2: Update thresholds**

Change from:

```kotlin
rule("Line Coverage") {
    bound {
        minValue = 80
        metric = kotlinx.kover.gradle.plugin.dsl.MetricType.LINE
    }
}

rule("Branch Coverage") {
    bound {
        minValue = 55
        metric = kotlinx.kover.gradle.plugin.dsl.MetricType.BRANCH
    }
}
```

To:

```kotlin
rule("Line Coverage") {
    bound {
        minValue = 98
        metric = kotlinx.kover.gradle.plugin.dsl.MetricType.LINE
    }
}

rule("Branch Coverage") {
    bound {
        minValue = 90
        metric = kotlinx.kover.gradle.plugin.dsl.MetricType.BRANCH
    }
}
```

**Step 3: Verify thresholds are enforced**

```bash
./gradlew koverVerify
```

Expected: BUILD SUCCESSFUL (if coverage meets thresholds)

**Step 4: Commit**

```bash
git add build.gradle.kts
git commit -m "build: raise coverage thresholds to 98%/90%

Line coverage: 98% (was 80%)
Branch coverage: 90% (was 55%)

All tests meet new thresholds.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Phase 5: Documentation Updates

### Task 13: Update README.md

**Files:**
- Modify: `README.md`

**Step 1: Update coverage badge**

Find line with coverage badge (around line 6):

Change from:
```markdown
![Coverage](https://img.shields.io/badge/coverage-94.67%25-brightgreen.svg)
```

To:
```markdown
![Coverage](https://img.shields.io/badge/coverage-98%2B%25-brightgreen.svg)
```

**Step 2: Add Example Output section**

After "Configuration" section (around line 101), add:

```markdown
## Example Output

When Detekt finds violations, you'll see clear messages:

```bash
src/main/kotlin/com/example/MyRepository.kt:5:1: [koin-rules] NoKoinComponentInterface
  Class 'MyRepository' implements KoinComponent but is not a framework entry point.
  Use constructor injection instead.

src/main/kotlin/com/example/di/AppModule.kt:10:5: [koin-rules] EmptyModule
  Module is empty. Remove it or add definitions/includes().

src/main/kotlin/com/example/MyService.kt:12:9: [koin-rules] NoGetOutsideModuleDefinition
  Found get() call outside module definition. Use constructor injection or define inside module { }.
```

Run analysis:

```bash
./gradlew detekt
```

View results in `build/reports/detekt/detekt.html`.
```

**Step 3: Enhance Code Coverage section**

Find "Code Coverage" section (around line 103), replace content with:

```markdown
## Code Coverage

This project uses [Kover](https://github.com/Kotlin/kotlinx-kover) for code coverage tracking and maintains **98%+ line coverage** and **90%+ branch coverage**.

### Test Types

- **Unit Tests (80+)**: Test each rule in isolation with positive and negative cases
- **Integration Tests (3)**: Verify ServiceLoader discovery and Detekt integration
- **Edge Case Tests (30+)**: Cover boundary conditions, complex scenarios, and edge syntax

All tests run automatically in CI with strict coverage verification.

### Generate Coverage Reports

```bash
./gradlew koverHtmlReport
```

View the HTML report at `build/reports/kover/html/index.html`.

### Verify Coverage

Coverage verification runs automatically with:

```bash
./gradlew check
```

This enforces minimum coverage thresholds:
- **Line coverage**: 98%
- **Branch coverage**: 90%

> **Note:** The coverage badge is updated manually after significant changes. Run `koverHtmlReport` to see current exact percentage.

### Coverage Rules

- Test code is excluded from coverage
- Generated code and providers are excluded
- All public APIs must have tests
- All rule implementations must have comprehensive tests including edge cases
```

**Step 4: Run build to verify**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add README.md
git commit -m "docs: update README with 98% coverage and examples

- Update coverage badge to 98%+
- Add Example Output section showing Detekt messages
- Enhance Code Coverage section with test breakdown
- Document new 98%/90% thresholds

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 14: Update docs/rules.md

**Files:**
- Modify: `docs/rules.md`

**Step 1: Add Edge Cases subsections**

For each rule in `docs/rules.md`, add an "Edge Cases" subsection after the examples.

**Template:**

```markdown
### [Rule Name]

...existing docs...

**Edge Cases:**
- ✅ [Edge case 1 description]
- ✅ [Edge case 2 description]
- ✅ [Edge case 3 description]

---
```

**Example for NoKoinComponentInterface:**

After existing docs (around line 78), add:

```markdown
**Edge Cases:**
- ✅ Generic types: `class MyViewModel<T> : ViewModel(), KoinComponent`
- ✅ Fully qualified names: `androidx.activity.ComponentActivity`
- ✅ Multiple inheritance: `class A : Activity(), SomeInterface, KoinComponent`
- ✅ Companion objects implementing KoinComponent are reported
```

**Step 2: Apply to all 14 rules**

Add edge cases for:
- NoKoinComponentInterface
- NoGetOutsideModuleDefinition
- NoInjectDelegate
- NoGlobalContextAccess
- NoKoinGetInApplication
- EmptyModule
- SingleForNonSharedDependency
- MissingScopedDependencyQualifier
- DeprecatedKoinApi
- ModuleIncludesOrganization
- MissingScopeClose
- ScopedDependencyOutsideScopeBlock
- FactoryInScopeBlock
- KtorRequestScopeMisuse

**Step 3: Commit**

```bash
git add docs/rules.md
git commit -m "docs: add edge cases to all rules documentation

Document edge cases covered by comprehensive test suite
for all 14 rules.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 15: Create CONTRIBUTING.md

**Files:**
- Create: `CONTRIBUTING.md`

**Step 1: Create file**

Create: `CONTRIBUTING.md` in project root

```markdown
# Contributing to detekt-rules-koin

Thank you for contributing! This guide will help you get started.

## Development Setup

**Requirements:**
- JDK 21 (Temurin recommended)
- Gradle 8.x (wrapper included)

**Clone and build:**

```bash
git clone https://github.com/androidbroadcast/Koin-Detekt.git
cd Koin-Detekt
./gradlew build
```

## Running Tests

**Run all tests:**

```bash
./gradlew test
```

**Run specific test:**

```bash
./gradlew test --tests "NoKoinComponentInterfaceTest"
```

**Run with coverage:**

```bash
./gradlew koverHtmlReport
open build/reports/kover/html/index.html
```

## Code Coverage Requirements

We maintain **98%+ line coverage** and **90%+ branch coverage**.

**Before submitting a PR:**

1. Run tests: `./gradlew test`
2. Generate coverage: `./gradlew koverHtmlReport`
3. Check report: `build/reports/kover/html/index.html`
4. Ensure your changes don't decrease coverage below thresholds

CI will automatically fail PRs that decrease coverage.

## Adding New Rules

### 1. Create Rule Implementation

Add to appropriate package:
- `servicelocator/` - Service Locator anti-patterns
- `moduledsl/` - Module DSL best practices
- `scope/` - Scope management

Example:

```kotlin
internal class MyNewRule(config: Config) : Rule(config) {
    override val issue = Issue(
        id = "MyNewRule",
        severity = Severity.Warning,
        description = "Detects [problem]",
        debt = Debt.FIVE_MINS
    )

    override fun visitXxx(element: KtXxx) {
        super.visitXxx(element)
        // Detect violations
        if (violationFound) {
            report(CodeSmell(issue, Entity.from(element), "Message"))
        }
    }
}
```

### 2. Add Comprehensive Tests

Create test file: `src/test/kotlin/.../MyNewRuleTest.kt`

**Required tests:**
- ✅ Positive case (rule triggers)
- ✅ Negative case (rule doesn't trigger)
- ✅ Edge cases (generics, nesting, qualified names, etc.)
- ✅ Configuration options (if any)

Aim for **100% coverage** of your rule's logic.

### 3. Register Rule

Add to `KoinRuleSetProvider.kt`:

```kotlin
override fun instance(config: Config): RuleSet {
    return RuleSet(
        ruleSetId,
        listOf(
            // ... existing rules ...
            MyNewRule(config),
        )
    )
}
```

### 4. Document the Rule

Add to `docs/rules.md`:

```markdown
### MyNewRule

**Severity:** Warning
**Active by default:** Yes

[Description]

❌ **Bad:**
```kotlin
// Bad example
```

✅ **Good:**
```kotlin
// Good example
```

**Configuration:**
```yaml
MyNewRule:
  active: true
  someOption: value
```
```

### 5. Update Tests Count

Update integration test in `KoinRulesIntegrationTest.kt`:

```kotlin
assertThat(ruleSet.rules).hasSize(15)  // Increment from 14
```

Add rule ID to expected list.

## Pull Request Process

1. **Create feature branch:** `git checkout -b feature/my-new-rule`
2. **Make changes** following guidelines above
3. **Ensure all tests pass:** `./gradlew build`
4. **Verify coverage:** Coverage must stay ≥ 98%/90%
5. **Update documentation** if adding/changing rules
6. **Follow code style:**
   - Explicit API mode enforced (all public APIs must have visibility)
   - No compilation warnings (warnings as errors enabled)
   - Kotlin official code style
7. **Write clear commit messages:**
   ```
   feat: add MyNewRule for detecting X

   Detects [problem] and suggests [solution].

   Co-Authored-By: Your Name <email>
   ```
8. **Open Pull Request** with clear description
9. **Wait for CI** to pass (build, tests, coverage checks)
10. **Address review feedback**

## Code Quality Standards

- ✅ All warnings as errors (enforced by compiler)
- ✅ Explicit API mode (all public members need visibility modifiers)
- ✅ 98%+ line coverage, 90%+ branch coverage
- ✅ Comprehensive test suite (unit + integration + edge cases)
- ✅ Well-documented rules with examples

## Questions or Help?

- Open an issue for bugs or feature requests
- Start a discussion for questions
- Check existing issues/PRs for similar topics

## License

By contributing, you agree that your contributions will be licensed under Apache License 2.0.

---

**Thank you for making detekt-rules-koin better!** 🎉
```

**Step 2: Verify file**

```bash
cat CONTRIBUTING.md | head -20
```

**Step 3: Commit**

```bash
git add CONTRIBUTING.md
git commit -m "docs: create CONTRIBUTING.md guide

Comprehensive contribution guide covering:
- Development setup
- Running tests and coverage
- Adding new rules (step-by-step)
- PR process
- Code quality standards

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Phase 6: CI/CD Updates & Final Verification

### Task 16: Update CI Workflow

**Files:**
- Modify: `.github/workflows/pr-validation.yml`

**Step 1: Update coverage thresholds in PR comment**

Find the PR comment script (around line 136-144), update:

Change:
```javascript
'✅ **Coverage**: 80% line / 55% branch enforced\n' +
```

To:
```javascript
'✅ **Coverage**: 98% line / 90% branch enforced\n' +
```

**Step 2: Update test count in comment** (if hardcoded)

Find test count reference (around line 138):

Change:
```javascript
'✅ **Tests**: All 48 tests passed\n' +
```

To:
```javascript
'✅ **Tests**: All tests passed (80+ unit + integration + edge cases)\n' +
```

**Step 3: Commit**

```bash
git add .github/workflows/pr-validation.yml
git commit -m "ci: update coverage thresholds in PR comments

Reflect new 98% line / 90% branch requirements.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 17: Final Build Verification

**Files:**
- Verify: All files

**Step 1: Clean build**

```bash
./gradlew clean
```

**Step 2: Full build with all checks**

```bash
./gradlew build --no-daemon
```

Expected output:
```
BUILD SUCCESSFUL
```

Verify:
- ✅ Compilation successful (no warnings due to allWarningsAsErrors)
- ✅ All tests pass
- ✅ Coverage verification passes (98%+ line, 90%+ branch)
- ✅ JARs built successfully

**Step 3: Verify coverage report**

```bash
./gradlew koverHtmlReport
open build/reports/kover/html/index.html
```

Check final metrics:
- Line coverage: ≥ 98%
- Branch coverage: ≥ 90%

**Step 4: Run coverage verification**

```bash
./gradlew koverVerify
```

Expected: BUILD SUCCESSFUL (thresholds met)

**Step 5: Check test count**

```bash
./gradlew test 2>&1 | grep -E "tests completed|tests passed"
```

Should show 80-100 tests completed successfully.

---

### Task 18: Update Coverage Badge (Manual)

**Files:**
- Modify: `README.md`

**Step 1: Get exact coverage percentage**

From `build/reports/kover/html/index.html`, note exact line coverage percentage.

**Step 2: Update badge**

If exact percentage is e.g. 98.5%, update badge:

```markdown
![Coverage](https://img.shields.io/badge/coverage-98.5%25-brightgreen.svg)
```

**Step 3: Commit**

```bash
git add README.md
git commit -m "docs: update coverage badge to exact percentage

Reflects actual coverage after comprehensive test additions.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 19: Final Commit & Summary

**Step 1: Review all changes**

```bash
git log --oneline main..HEAD
```

Should show ~15-20 commits covering:
- Integration tests
- Edge cases for all rules
- Documentation updates
- Configuration updates
- CI updates

**Step 2: Run final verification**

```bash
./gradlew clean build koverHtmlReport
```

**Step 3: Create summary**

Create: `docs/plans/implementation-summary-2026-02-13.md`

```markdown
# Test Coverage Implementation Summary

**Date:** 2026-02-13
**Goal:** Achieve 98%+ line coverage and 90%+ branch coverage

## Final Metrics

- **Line Coverage:** XX.XX% (target: 98%, was: 94.67%)
- **Branch Coverage:** XX.XX% (target: 90%, was: ~55%)
- **Total Tests:** XXX (was: 55)

## Changes Implemented

### Integration Tests (3 new tests)
- ServiceLoader discovery validation
- End-to-end multi-violation detection
- Configuration application verification

### Edge Cases Added
- NoKoinComponentInterface: 4 edge cases
- EmptyModule: 3 edge cases
- MissingScopeClose: 3 edge cases
- SingleForNonSharedDependency: 3 edge cases
- [Continue for all rules...]

**Total edge case tests added:** ~XX

### Documentation
- ✅ README.md updated (badge, examples, coverage section)
- ✅ docs/rules.md updated (edge cases for all rules)
- ✅ CONTRIBUTING.md created (comprehensive guide)

### Configuration
- ✅ Kover thresholds: 98% line, 90% branch
- ✅ CI/CD updated for new thresholds
- ✅ All checks passing

## Commits

Total commits: XX

Key commits:
- Integration test structure
- ServiceLoader test
- E2E analysis test
- Config application test
- [Edge case commits for each rule]
- Documentation updates
- Configuration updates

## Verification

✅ All tests pass
✅ Coverage thresholds met
✅ CI build succeeds
✅ Documentation complete
✅ No compilation warnings
✅ Reproducible builds verified

## Next Steps for v0.1.0 Release

1. Review implementation
2. Merge to main
3. Proceed with release preparation
```

**Step 4: Commit summary**

```bash
git add docs/plans/implementation-summary-2026-02-13.md
git commit -m "docs: add implementation summary

Document final metrics and all changes made during
test coverage improvement initiative.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Completion Checklist

Before marking this plan complete, verify:

- [ ] Line coverage ≥ 98%
- [ ] Branch coverage ≥ 90%
- [ ] 3 integration tests added and passing
- [ ] ~30-40 edge case tests added across all 14 rules
- [ ] README.md updated (badge, examples, coverage section)
- [ ] docs/rules.md updated (edge cases documented)
- [ ] CONTRIBUTING.md created
- [ ] Kover thresholds updated to 98%/90%
- [ ] CI/CD workflow updated
- [ ] All tests pass: `./gradlew test`
- [ ] Coverage verification passes: `./gradlew koverVerify`
- [ ] Full build succeeds: `./gradlew build`
- [ ] No compilation warnings
- [ ] All commits follow convention
- [ ] Implementation summary documented

**Total estimated time:** 6-8 hours (can be split across sessions)

---

**Plan Status:** ✅ Ready for implementation
**Created:** 2026-02-13
**Next Step:** Execute plan using superpowers:executing-plans or superpowers:subagent-driven-development
