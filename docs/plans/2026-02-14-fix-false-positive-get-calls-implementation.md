# Fix False Positive on Non-Koin get() Methods - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix false positives in `NoGetOutsideModuleDefinition` and `NoKoinGetInApplication` rules by implementing import-based detection.

**Architecture:** Add `visitImportDirective()` to collect Koin imports from `org.koin.*` packages, then check that `get()`/`getOrNull()`/`getAll()`/`inject()` calls are from imported Koin functions before reporting violations.

**Tech Stack:** Kotlin 2.0.21, Detekt 1.23.8, JUnit 5, AssertJ

**Reference:** Design document at `docs/plans/2026-02-14-fix-false-positive-get-calls-design.md`

---

## Task 1: Add False Positive Tests for NoGetOutsideModuleDefinition

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinitionTest.kt:141`

**Step 1: Write failing tests for non-Koin methods**

Add after line 141 (at end of test class):

```kotlin
    @Test
    fun `does not report AtomicReference get()`() {
        val code = """
            import java.util.concurrent.atomic.AtomicReference

            class MyClass {
                val ref = AtomicReference("value")
                val current = ref.get()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report SendChannel getOrNull()`() {
        val code = """
            import kotlinx.coroutines.channels.Channel

            suspend fun test() {
                val channel = Channel<String>()
                val value = channel.getOrNull()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report Map get()`() {
        val code = """
            class MyClass {
                val map = mapOf("key" to "value")
                val value = map.get("key")
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report List getOrNull()`() {
        val code = """
            class MyClass {
                val list = listOf(1, 2, 3)
                val value = list.getOrNull(0)
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `still reports Koin get() with import`() {
        val code = """
            import org.koin.core.component.get

            class MyRepo {
                val service = get<ApiService>()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "NoGetOutsideModuleDefinitionTest"`
Expected: FAIL - new tests fail because rule currently reports all `get()` calls

**Step 3: Commit failing tests**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinitionTest.kt
git commit -m "test: add failing tests for non-Koin get() methods

Tests verify that AtomicReference.get(), SendChannel.getOrNull(),
Map.get(), and List.getOrNull() should not be reported as violations.

Related to false positive fix.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Implement Import-Checking in NoGetOutsideModuleDefinition

**Files:**
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinition.kt:13-57`

**Step 1: Add import collection field and method**

Add after line 23 (after `definitionFunctions`):

```kotlin
    private val koinImports = mutableSetOf<String>()

    override fun visitImportDirective(importDirective: KtImportDirective) {
        val importPath = importDirective.importPath?.pathStr
        if (importPath?.startsWith("org.koin.") == true) {
            importDirective.importedName?.asString()?.let { koinImports.add(it) }
        }
        super.visitImportDirective(importDirective)
    }
```

Add import at top of file (after existing imports):

```kotlin
import org.jetbrains.kotlin.psi.KtImportDirective
```

**Step 2: Modify visitCallExpression to check imports**

Replace lines 26-56 with:

```kotlin
    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Check if function is imported from Koin - skip if not
        if (callName != null && callName in setOf("get", "getOrNull", "getAll") && callName !in koinImports) {
            super.visitCallExpression(expression)
            return
        }

        // Track entering definition blocks
        if (callName in definitionFunctions) {
            val wasInside = insideDefinitionBlock
            insideDefinitionBlock = true
            super.visitCallExpression(expression)
            insideDefinitionBlock = wasInside
            return
        }

        // Check for get() calls from Koin
        if (callName in setOf("get", "getOrNull", "getAll") && !insideDefinitionBlock) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    $callName() used outside module definition → Service locator pattern, harder to test
                    → Use constructor injection instead

                    ✗ Bad:  class MyRepo : KoinComponent { val api = get<Api>() }
                    ✓ Good: class MyRepo(private val api: Api)
                    """.trimIndent()
                )
            )
        }

        super.visitCallExpression(expression)
    }
```

**Step 3: Run tests to verify new tests pass**

Run: `./gradlew test --tests "NoGetOutsideModuleDefinitionTest"`
Expected: New tests PASS, but some existing tests may FAIL (need imports)

**Step 4: Commit implementation**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinition.kt
git commit -m "feat: add import-based detection to NoGetOutsideModuleDefinition

Rule now checks that get/getOrNull/getAll are imported from org.koin.*
before reporting violations. This fixes false positives on non-Koin
methods with the same names (AtomicReference.get(), Map.get(), etc.).

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Fix Existing Tests in NoGetOutsideModuleDefinitionTest

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinitionTest.kt`

**Step 1: Add import to "does not report get() inside single block"**

Modify test at lines 31-44:

```kotlin
    @Test
    fun `does not report get() inside single block`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.component.get

            val myModule = module {
                single { MyRepository(get()) }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
```

**Step 2: Add import to "does not report get() inside factory block"**

Modify test at lines 46-60:

```kotlin
    @Test
    fun `does not report get() inside factory block`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.component.get

            val myModule = module {
                factory { MyUseCase(get()) }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
```

**Step 3: Add import to "reports get in init block"**

Modify test at lines 103-114:

```kotlin
    @Test
    fun `reports get in init block`() {
        val code = """
            import org.koin.core.component.get

            class MyRepo {
                init {
                    val service = get<ApiService>()
                }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
```

**Step 4: Add import to "reports get in property initializer"**

Modify test at lines 117-126:

```kotlin
    @Test
    fun `reports get in property initializer`() {
        val code = """
            import org.koin.core.component.get

            class MyRepo {
                val service = get<ApiService>()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
```

**Step 5: Add import to "reports get in companion object"**

Modify test at lines 129-140:

```kotlin
    @Test
    fun `reports get in companion object`() {
        val code = """
            import org.koin.core.component.get

            class MyRepo {
                companion object {
                    val service = get<ApiService>()
                }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
```

**Step 6: Run all tests to verify they pass**

Run: `./gradlew test --tests "NoGetOutsideModuleDefinitionTest"`
Expected: ALL PASS

**Step 7: Commit test fixes**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinitionTest.kt
git commit -m "test: add missing Koin imports to NoGetOutsideModuleDefinition tests

After implementing import-based detection, tests need to import
org.koin.core.component.get to properly test the rule behavior.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Add False Positive Tests for NoKoinGetInApplication

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplicationTest.kt:280`

**Step 1: Write failing tests for non-Koin methods**

Add after line 280 (at end of test class):

```kotlin
    @Test
    fun `does not report non-Koin get() without import`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    val map = mapOf("key" to "value")
                    val value = map.get("key")
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report non-Koin inject() without import`() {
        val code = """
            import org.koin.core.context.startKoin

            class CustomDelegate {
                operator fun getValue(thisRef: Any?, property: Any?): String = "value"
            }

            fun inject(): CustomDelegate = CustomDelegate()

            fun main() {
                startKoin {
                    val service: String by inject()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "NoKoinGetInApplicationTest"`
Expected: FAIL - new tests fail because rule currently reports all `get()`/`inject()` calls

**Step 3: Commit failing tests**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplicationTest.kt
git commit -m "test: add failing tests for non-Koin get/inject in NoKoinGetInApplication

Tests verify that non-Koin get() and inject() methods should not be
reported inside startKoin blocks.

Related to false positive fix.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Implement Import-Checking in NoKoinGetInApplication

**Files:**
- Modify: `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplication.kt:13-56`

**Step 1: Add import collection field and method**

Add after line 23 (after `insideStartKoinBlock`):

```kotlin
    private val koinImports = mutableSetOf<String>()

    override fun visitImportDirective(importDirective: KtImportDirective) {
        val importPath = importDirective.importPath?.pathStr
        if (importPath?.startsWith("org.koin.") == true) {
            importDirective.importedName?.asString()?.let { koinImports.add(it) }
        }
        super.visitImportDirective(importDirective)
    }
```

Add import at top of file (after existing imports):

```kotlin
import org.jetbrains.kotlin.psi.KtImportDirective
```

**Step 2: Modify visitCallExpression to check imports**

Replace lines 25-55 with:

```kotlin
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

        // Check if function is imported from Koin - skip if not
        if (callName != null && callName in setOf("get", "inject") && callName !in koinImports) {
            super.visitCallExpression(expression)
            return
        }

        // Check for get()/inject() from Koin inside application blocks
        if (callName in setOf("get", "inject") && insideStartKoinBlock) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    $callName() used in startKoin block → Service locator at app initialization
                    → Define dependencies in modules instead

                    ✗ Bad:  startKoin { val api = get<Api>() }
                    ✓ Good: startKoin { modules(appModule) }
                    """.trimIndent()
                )
            )
        }

        super.visitCallExpression(expression)
    }
```

**Step 3: Run tests to verify new tests pass**

Run: `./gradlew test --tests "NoKoinGetInApplicationTest"`
Expected: New tests PASS, but some existing tests may FAIL (need imports)

**Step 4: Commit implementation**

```bash
git add src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplication.kt
git commit -m "feat: add import-based detection to NoKoinGetInApplication

Rule now checks that get/inject are imported from org.koin.* before
reporting violations inside startKoin blocks. This fixes false positives
on non-Koin methods with the same names.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 6: Fix Existing Tests in NoKoinGetInApplicationTest

**Files:**
- Modify: `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplicationTest.kt`

**Step 1: Add import to "reports get() inside startKoin block"**

Modify test at lines 11-29:

```kotlin
    @Test
    fun `reports get() inside startKoin block`() {
        val code = """
            import org.koin.core.context.startKoin
            import org.koin.core.component.get

            fun main() {
                startKoin {
                    val service = get<MyService>()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }
```

**Step 2: Add import to "reports inject() inside startKoin block"**

Modify test at lines 50-68:

```kotlin
    @Test
    fun `reports inject() inside startKoin block`() {
        val code = """
            import org.koin.core.context.startKoin
            import org.koin.core.component.inject

            fun main() {
                startKoin {
                    val service: MyService by inject()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }
```

**Step 3: Add import to "reports get() inside koinConfiguration block"**

Modify test at lines 71-89:

```kotlin
    @Test
    fun `reports get() inside koinConfiguration block`() {
        val code = """
            import org.koin.core.context.koinConfiguration
            import org.koin.core.component.get

            fun configure() {
                koinConfiguration {
                    val service = get<MyService>()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }
```

**Step 4: Add import to "reports inject() inside koinConfiguration block"**

Modify test at lines 92-110:

```kotlin
    @Test
    fun `reports inject() inside koinConfiguration block`() {
        val code = """
            import org.koin.core.context.koinConfiguration
            import org.koin.core.component.inject

            fun configure() {
                koinConfiguration {
                    val service: MyService by inject()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }
```

**Step 5: Add import to "reports multiple get() calls inside startKoin"**

Modify test at lines 113-130:

```kotlin
    @Test
    fun `reports multiple get() calls inside startKoin`() {
        val code = """
            import org.koin.core.context.startKoin
            import org.koin.core.component.get

            fun main() {
                startKoin {
                    val service1 = get<Service1>()
                    val service2 = get<Service2>()
                    modules(appModule)
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(2)
    }
```

**Step 6: Add import to "reports nested get() inside startKoin with lambda"**

Modify test at lines 133-151:

```kotlin
    @Test
    fun `reports nested get() inside startKoin with lambda`() {
        val code = """
            import org.koin.core.context.startKoin
            import org.koin.core.component.get

            fun main() {
                startKoin {
                    modules(appModule)
                    listOf(1, 2).forEach {
                        val service = get<MyService>()
                    }
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }
```

**Step 7: Add import to "handles nested startKoin blocks correctly"**

Modify test at lines 184-201:

```kotlin
    @Test
    fun `handles nested startKoin blocks correctly`() {
        val code = """
            import org.koin.core.context.startKoin
            import org.koin.core.component.get

            fun main() {
                startKoin {
                    modules(module {
                        single { get<OtherService>() }
                    })
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }
```

**Step 8: Add import to "does not report get() in module definition outside startKoin"**

Modify test at lines 204-223:

```kotlin
    @Test
    fun `does not report get() in module definition outside startKoin`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.component.get
            import org.koin.core.context.startKoin

            val myModule = module {
                single { MyService(get()) }
            }

            fun main() {
                startKoin {
                    modules(myModule)
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
```

**Step 9: Add import to "reports get() with qualifier inside startKoin"**

Modify test at lines 226-242:

```kotlin
    @Test
    fun `reports get() with qualifier inside startKoin`() {
        val code = """
            import org.koin.core.context.startKoin
            import org.koin.core.qualifier.named
            import org.koin.core.component.get

            fun main() {
                startKoin {
                    val service = get<MyService>(named("special"))
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }
```

**Step 10: Add import to "reports inject() with parameters inside startKoin"**

Modify test at lines 245-260:

```kotlin
    @Test
    fun `reports inject() with parameters inside startKoin`() {
        val code = """
            import org.koin.core.context.startKoin
            import org.koin.core.component.inject

            fun main() {
                startKoin {
                    val service: MyService by inject { parametersOf("param") }
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }
```

**Step 11: Run all tests to verify they pass**

Run: `./gradlew test --tests "NoKoinGetInApplicationTest"`
Expected: ALL PASS

**Step 12: Commit test fixes**

```bash
git add src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplicationTest.kt
git commit -m "test: add missing Koin imports to NoKoinGetInApplication tests

After implementing import-based detection, tests need to import
org.koin.core.component.get and inject to properly test the rule behavior.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 7: Update Documentation (docs/rules.md)

**Files:**
- Modify: `docs/rules.md:9-36`

**Step 1: Update NoGetOutsideModuleDefinition section**

Replace lines 9-36 with:

```markdown
### NoGetOutsideModuleDefinition

**Severity:** Warning
**Active by default:** Yes

Detects `get()` / `getOrNull()` / `getAll()` calls **from Koin API** outside module definition blocks.

**Note:** The rule checks only Koin functions imported from `org.koin.*` packages. Other methods with the same names (e.g., `AtomicReference.get()`, `Map.get()`) are ignored.

❌ **Bad:**
```kotlin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MyRepository : KoinComponent {
    val api = get<ApiService>()  // ← Koin get()
}
```

✅ **Good:**
```kotlin
import org.koin.dsl.module

module {
    single { MyRepository(get()) }
}
```

**Not reported (non-Koin APIs):**
```kotlin
// java.util.concurrent.atomic
val ref = AtomicReference("value")
val current = ref.get()  // ✓ OK

// kotlinx.coroutines.channels
val channel = Channel<String>()
val value = channel.getOrNull()  // ✓ OK

// Map, List, etc.
val map = mapOf("key" to "value")
val value = map.get("key")  // ✓ OK
```

**Edge Cases:**
- ✅ Detects `get()`, `getOrNull()`, and `getAll()` variants from Koin
- ✅ Detects in init blocks and property initializers
- ✅ Detects in companion objects
- ✅ Allows `get()` inside `single {}`, `factory {}`, and other module definitions
- ✅ Ignores non-Koin methods with the same names
```

**Step 2: Run documentation build to verify syntax**

Run: `./gradlew build`
Expected: Build succeeds

**Step 3: Commit documentation update**

```bash
git add docs/rules.md
git commit -m "docs: update NoGetOutsideModuleDefinition with import-based detection

Document that the rule now uses import-based detection and only checks
Koin API calls. Add examples of non-Koin methods that won't be reported.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 8: Update CHANGELOG.md

**Files:**
- Modify: `CHANGELOG.md` (at top, after existing entries)

**Step 1: Add v0.4.1 changelog entry**

Add at the top of CHANGELOG.md (after header, before existing entries):

```markdown
## [0.4.1] - 2026-02-14

### Fixed
- **NoGetOutsideModuleDefinition**: Fixed false positive on non-Koin `get()` methods (#XX)
  - Rule now checks only Koin functions imported from `org.koin.*` packages
  - No longer reports `AtomicReference.get()`, `SendChannel.getOrNull()`, `Map.get()`, etc.
  - Fully backward compatible - only reduces false positives
- **NoKoinGetInApplication**: Fixed false positive on non-Koin `get()` and `inject()` methods
  - Same import-based detection as NoGetOutsideModuleDefinition
  - Only Koin API calls inside startKoin blocks are now reported

```

**Step 2: Commit CHANGELOG update**

```bash
git add CHANGELOG.md
git commit -m "docs: add v0.4.1 changelog entry

Document false positive fixes in NoGetOutsideModuleDefinition and
NoKoinGetInApplication rules.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 9: Run Full Test Suite and Coverage Verification

**Files:**
- N/A (verification only)

**Step 1: Run all tests**

Run: `./gradlew test`
Expected: ALL PASS

**Step 2: Verify coverage thresholds**

Run: `./gradlew koverVerify`
Expected: PASS - coverage ≥96% line, ≥70% branch

**Step 3: Generate and inspect coverage report**

Run: `./gradlew koverHtmlReport`
Then: Open `build/reports/kover/html/index.html` in browser
Expected: Both modified rules have high coverage on new code

**Step 4: Run full build with all checks**

Run: `./gradlew clean check`
Expected: SUCCESS - all tests pass, coverage verified, quality checks pass

---

## Task 10: Final Verification and Summary

**Files:**
- N/A (verification only)

**Step 1: Verify success criteria**

Checklist:
- ✅ All new tests pass
- ✅ All existing tests pass (with import additions)
- ✅ Coverage remains ≥96% line, ≥70% branch
- ✅ `AtomicReference.get()` no longer reported
- ✅ `SendChannel.getOrNull()` no longer reported
- ✅ Koin `get<T>()` with import still reported correctly
- ✅ Documentation updated (docs/rules.md)
- ✅ CHANGELOG updated

**Step 2: Review git history**

Run: `git log --oneline -10`
Expected: Clean commit history with:
1. Test commits (failing tests)
2. Implementation commits
3. Test fix commits
4. Documentation commits

**Step 3: Create summary of changes**

Files modified:
- `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinition.kt`
- `src/main/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplication.kt`
- `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoGetOutsideModuleDefinitionTest.kt`
- `src/test/kotlin/io/github/krozov/detekt/koin/servicelocator/NoKoinGetInApplicationTest.kt`
- `docs/rules.md`
- `CHANGELOG.md`

New functionality:
- Import-based detection in both rules
- 7 new tests (5 + 2)
- Documentation updates

**Step 4: Ready for review**

Implementation complete. Next steps:
- Code review (@superpowers:requesting-code-review if available)
- Version bump to 0.4.1
- Release preparation

---

## Notes

- **TDD approach:** All new tests written first, verified to fail, then implementation
- **DRY:** Import-checking logic is similar in both rules (could be extracted to utility if more rules need it in future)
- **YAGNI:** No over-engineering - simple import checking, no type resolution complexity
- **Coverage:** New code fully covered by new tests
- **Backward compatibility:** 100% - only reduces false positives, no breaking changes
