# Test Coverage Strategy Design

**Date:** 2026-02-13
**Status:** Approved
**Goal:** Achieve 98%+ line coverage and 90%+ branch coverage for v0.1.0 release

---

## 1. Overview and Goals

### Current State
- **Coverage:** 94.67% line coverage (Kover configured)
- **Tests:** 55 unit tests across 15 test files
- **Infrastructure:** JUnit 5, AssertJ, Detekt Test API, Kover reporting

### Problem
While unit test coverage is strong, there are critical gaps:
- No integration tests validating Detekt plugin loading
- Missing edge case coverage for complex scenarios
- Branch coverage at minimum threshold (55%)
- No verification of ServiceLoader discovery mechanism

### Objective
Add comprehensive test coverage to achieve:
- **98%+ line coverage** (up from 94.67%)
- **90%+ branch coverage** (up from 55% minimum)
- **Integration test suite** validating real-world Detekt integration
- **Systematic edge case coverage** for all 14 rules

### Scope

**In Scope:**
- Integration tests (ServiceLoader, E2E, Config)
- Comprehensive edge cases for all rules
- Branch coverage improvements
- Documentation updates
- CI/CD threshold enforcement

**Out of Scope (for v0.1.0):**
- Mutation testing (Pitest) - defer to v0.2.0
- Performance benchmarks - defer to v0.2.0
- Property-based testing - nice-to-have for future

---

## 2. Integration Test Design

### Purpose
Verify that the plugin works correctly within Detekt's runtime environment, beyond isolated unit tests.

### Architecture

**Location:** `src/test/kotlin/io/github/krozov/detekt/koin/integration/KoinRulesIntegrationTest.kt`

**Test Suite:**

#### Test 1: ServiceLoader Discovery
```kotlin
@Test
fun `Detekt can discover KoinRuleSetProvider via ServiceLoader`() {
    val providers = ServiceLoader.load(RuleSetProvider::class.java).toList()
    val koinProvider = providers.find { it is KoinRuleSetProvider }

    assertThat(koinProvider).isNotNull()
    val ruleSet = koinProvider!!.instance(Config.empty)
    assertThat(ruleSet.id).isEqualTo("koin-rules")
    assertThat(ruleSet.rules).hasSize(14)
}
```

**Validates:**
- META-INF/services file is correct
- ServiceLoader can discover the provider
- All 14 rules are instantiated

#### Test 2: End-to-End Analysis
```kotlin
@Test
fun `Detekt analyzes real Kotlin file and reports violations`() {
    val code = """
        import org.koin.core.component.KoinComponent
        class MyRepo : KoinComponent {
            val api = get<ApiService>()
        }
    """.trimIndent()

    val findings = Detekt.create().runAnalysis(code, ruleSetProvider)

    // Should find: NoKoinComponentInterface + NoGetOutsideModuleDefinition
    assertThat(findings).hasSize(2)
    assertThat(findings.map { it.id }).containsExactlyInAnyOrder(
        "NoKoinComponentInterface",
        "NoGetOutsideModuleDefinition"
    )
}
```

**Validates:**
- Real Kotlin code parsing
- Multiple rules working together
- Correct violation reporting

#### Test 3: Configuration Application
```kotlin
@Test
fun `custom detekt config is applied to rules`() {
    val config = TestConfig(mapOf(
        "koin-rules" to mapOf(
            "NoKoinComponentInterface" to mapOf(
                "active" to true,
                "allowedSuperTypes" to listOf("CustomFramework")
            )
        )
    ))

    val ruleSet = KoinRuleSetProvider().instance(config)
    val rule = ruleSet.rules.find { it.ruleId == "NoKoinComponentInterface" }

    assertThat(rule).isNotNull()
    // Verify CustomFramework is allowed
}
```

**Validates:**
- Config parsing from .detekt.yml
- Rule configuration application
- Custom settings override defaults

---

## 3. Comprehensive Edge Cases Strategy

### Approach

**Systematic Coverage:** For EACH of 14 rules, add tests covering:

1. **Boundary conditions** — empty values, nulls, limits
2. **Negative cases** — code that should NOT trigger the rule
3. **Complex nesting** — nested classes, lambdas, scopes
4. **Generic types** — `<T>`, `<*>`, `<out T>`, `<in T>`
5. **Qualified names** — full package paths
6. **Multiple violations** — multiple issues in one file
7. **Edge syntax** — trailing commas, multiline, comments

### Edge Cases by Rule Category

#### Service Locator Rules (5 rules)

**NoKoinComponentInterface:**
- Generic types: `class MyViewModel<T> : ViewModel(), KoinComponent`
- Qualified names: `androidx.activity.ComponentActivity`
- Multiple inheritance: `class A : B(), C(), KoinComponent`
- Companion objects: `companion object : KoinComponent`
- Anonymous classes with KoinComponent
- Type aliases: `typealias MyAlias = Activity`

**NoGetOutsideModuleDefinition:**
- `get()` in property initializer
- `get()` in init block
- `get()` in companion object
- Nested lambdas: `module { single { factory { get() } } }`
- `getOrNull()` and `getAll()` variations

**NoInjectDelegate:**
- `by inject()` in companion object
- `by injectOrNull()` variations
- Multiple delegates: `val a by inject(); val b by inject()`

**NoGlobalContextAccess:**
- All GlobalContext methods: `get()`, `getKoinApplicationOrNull()`, `getOrNull()`
- KoinPlatformTools variations

**NoKoinGetInApplication:**
- `get()` vs `inject()` in startKoin
- Nested blocks inside startKoin

#### Module DSL Rules (5 rules)

**EmptyModule:**
- Whitespace only: `module {   }`
- Comments only: `module { /* TODO */ }`
- Nested empty lambdas
- Multiple empty statements

**SingleForNonSharedDependency:**
- All suffixes: UseCase, Command, Mapper, Handler, Validator, Interactor, Worker
- Mixed case: `getUserUseCase`, `UserUseCaseImpl`
- `singleOf()` vs `single { }`
- Custom namePatterns configuration

**MissingScopedDependencyQualifier:**
- Same type multiple times without qualifier
- Same type with different qualifiers (should pass)
- Generic types: `Repository<User>` vs `Repository<Post>`

**DeprecatedKoinApi:**
- All deprecated APIs: checkModules, koinNavViewModel, stateViewModel
- Mixed deprecated and current API

**ModuleIncludesOrganization:**
- Boundary: exactly maxIncludesWithDefinitions
- One over threshold
- Many includes, no definitions (should pass)

#### Scope Management Rules (4 rules)

**MissingScopeClose:**
- Multiple scopes in one class
- Scope in companion object
- Conditional scope creation: `if (x) createScope()`
- Try-catch with scope
- Nested classes with scopes

**ScopedDependencyOutsideScopeBlock:**
- `scoped { }` at module level (violation)
- `scoped { }` inside `scope { }` (correct)
- Mixed scope types: `activityScope`, `fragmentScope`

**FactoryInScopeBlock:**
- `factory { }` inside `scope { }`
- `factoryOf()` inside `scope { }`

**KtorRequestScopeMisuse:**
- `single { }` inside `requestScope { }` (violation)
- `scoped { }` inside `requestScope { }` (correct)
- Nested requestScope blocks

### Implementation Location

Add edge cases to existing test files (do NOT create new files):
- `NoKoinComponentInterfaceTest.kt` — add 6+ tests
- `EmptyModuleTest.kt` — add 4+ tests
- etc.

Target: **~30-40 new edge case tests** across all rules.

---

## 4. Documentation Updates

### README.md

**Update coverage badge:**
```markdown
![Coverage](https://img.shields.io/badge/coverage-98%2B%25-brightgreen.svg)
```

**Add example output section:**
```markdown
## Example Output

When Detekt finds violations, you'll see clear messages:

```bash
src/main/kotlin/MyRepository.kt:5:1: [koin-rules] NoKoinComponentInterface
  Class 'MyRepository' implements KoinComponent but is not a framework entry point.
  Use constructor injection instead.

src/main/kotlin/MyModule.kt:10:5: [koin-rules] EmptyModule
  Module is empty. Remove it or add definitions/includes().
```
```

**Enhance Code Coverage section:**
```markdown
### Test Types

This project maintains 98%+ code coverage through:

- **Unit Tests (55+)**: Test each rule in isolation with positive and negative cases
- **Integration Tests (3)**: Verify ServiceLoader discovery and Detekt integration
- **Edge Case Tests (30+)**: Cover boundary conditions, complex scenarios, and edge syntax

All tests run automatically in CI with strict coverage verification.
```

### docs/rules.md

For each rule, add "Edge Cases" subsection:

```markdown
### NoKoinComponentInterface

...existing documentation...

**Edge Cases:**
- ✅ Generic types: `class MyViewModel<T> : ViewModel()`
- ✅ Fully qualified names: `androidx.activity.ComponentActivity`
- ✅ Multiple inheritance: `class A : B(), KoinComponent`
- ✅ Companion objects implementing KoinComponent
```

### CONTRIBUTING.md (new file)

```markdown
# Contributing to detekt-rules-koin

## Development Setup

Requires:
- JDK 21
- Gradle 8.x (wrapper included)

## Running Tests

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew koverHtmlReport
```

## Code Coverage Requirements

We maintain **98%+ line coverage** and **90%+ branch coverage**.

Before submitting a PR:
1. Run tests: `./gradlew test`
2. Generate coverage: `./gradlew koverHtmlReport`
3. Check report: `build/reports/kover/html/index.html`
4. Ensure your changes don't decrease coverage below thresholds

CI will automatically fail PRs that decrease coverage.

## Adding New Rules

1. Create rule implementation in appropriate package (`servicelocator`/`moduledsl`/`scope`)
2. Add comprehensive tests:
   - Positive cases (rule triggers)
   - Negative cases (rule doesn't trigger)
   - Edge cases (generics, nesting, qualified names, etc.)
3. Update `KoinRuleSetProvider` to include the rule
4. Document in `docs/rules.md` with examples
5. Update coverage badge if needed

## Test Structure

Each rule should have tests covering:
- Basic violation detection
- Configuration options (if any)
- Edge cases (see existing tests for examples)
- Integration with Detekt

Aim for 100% coverage of your rule's logic.

## Pull Request Process

1. Ensure all tests pass: `./gradlew build`
2. Verify coverage meets thresholds
3. Update documentation if adding/changing rules
4. Follow existing code style (explicit API mode enforced)
5. Write clear commit messages

## Questions?

Open an issue or discussion on GitHub.
```

---

## 5. Testing Infrastructure

### Kover Configuration Updates

**build.gradle.kts:**

```kotlin
kover {
    reports {
        filters {
            excludes {
                // Exclude test code itself
                classes("*.test.*", "*Test", "*Test$*")
            }
        }

        verify {
            rule("Line Coverage") {
                bound {
                    minValue = 98  // Raised from 80
                    metric = kotlinx.kover.gradle.plugin.dsl.MetricType.LINE
                }
            }

            rule("Branch Coverage") {
                bound {
                    minValue = 90  // Raised from 55
                    metric = kotlinx.kover.gradle.plugin.dsl.MetricType.BRANCH
                }
            }
        }

        html {
            title = "detekt-rules-koin Code Coverage"
        }
    }
}

tasks.check {
    dependsOn(tasks.koverVerify)  // Enforce on every build
}
```

### CI/CD Updates

**.github/workflows/pr-validation.yml:**

Update coverage thresholds in comments:
```yaml
'✅ **Coverage**: 98% line / 90% branch enforced\n' +
```

Add coverage report to artifacts (already done based on system reminders).

---

## 6. Success Criteria & Implementation Plan

### Success Criteria

**Must Have (v0.1.0 release blockers):**
- ✅ Line coverage: **98%+**
- ✅ Branch coverage: **90%+**
- ✅ Integration tests: **3 tests** (ServiceLoader, E2E, Config)
- ✅ Total tests: **80-100** (currently 55)
- ✅ CI passing with new coverage thresholds
- ✅ Documentation complete (README, rules.md, CONTRIBUTING.md)

**Quality Gates:**
- All tests pass locally
- CI build passes
- Coverage verification passes (enforced by Kover)
- No compilation warnings (explicit API mode)
- Reproducible builds verified

### Implementation Order

**Phase 1: Analysis (30 minutes)**
1. Generate Kover HTML report: `./gradlew koverHtmlReport`
2. Open `build/reports/kover/html/index.html`
3. Identify uncovered lines and branches per rule
4. Create checklist of gaps to fill

**Phase 2: Integration Tests (1-2 hours)**
1. Create `src/test/kotlin/io/github/krozov/detekt/koin/integration/` directory
2. Implement `KoinRulesIntegrationTest.kt` with 3 tests
3. Verify ServiceLoader discovery works
4. Run tests: `./gradlew test --tests "*Integration*"`

**Phase 3: Systematic Edge Cases (3-4 hours)**
1. For each rule, add edge cases from categories above
2. Priority: target uncovered branches from Phase 1
3. Add tests to existing test files (don't create new files)
4. Run tests after each rule: `./gradlew test`
5. Target: +30-40 tests

**Phase 4: Coverage Validation (30 minutes)**
1. Run: `./gradlew koverHtmlReport`
2. Verify: 98%+ line, 90%+ branch
3. If below target: identify remaining gaps, add targeted tests
4. Iterate until thresholds met

**Phase 5: Documentation (1 hour)**
1. Update README.md (badge, example output, test types)
2. Update docs/rules.md (edge cases for each rule)
3. Create CONTRIBUTING.md
4. Review all changes

**Phase 6: CI/CD & Final Verification (30 minutes)**
1. Update Kover thresholds in build.gradle.kts (98%/90%)
2. Update CI comments in pr-validation.yml
3. Run full build: `./gradlew clean build`
4. Verify CI passes (if pushed to branch)
5. Review coverage badge matches reality

### Time Estimate

**Total: 6-8 hours**
- Analysis: 0.5h
- Integration tests: 1.5h
- Edge cases: 3.5h
- Coverage validation: 0.5h
- Documentation: 1h
- CI/CD: 0.5h

Can be split across multiple sessions.

---

## 7. Risk Mitigation

### Risks

**Risk 1: Branch coverage hard to reach 90%**
- Some defensive code paths may be hard to trigger
- Mitigation: Focus on realistic branches first, evaluate if 85-87% is acceptable

**Risk 2: Integration tests complex to set up**
- Detekt Test API might not support full E2E easily
- Mitigation: Use Detekt's built-in test utilities, simplify if needed

**Risk 3: Time overrun**
- Edge cases could take longer than estimated
- Mitigation: Prioritize based on Phase 1 analysis, defer low-value tests

### Contingency

If 98% proves unrealistic due to unreachable code:
- Accept 95-96% with justification
- Document why remaining lines can't be tested
- Update thresholds accordingly

---

## Next Steps

After approval of this design:
1. Invoke `writing-plans` skill to create detailed implementation plan
2. Execute plan in phases
3. Review and iterate

---

**Design Status:** ✅ Approved
**Next:** Create implementation plan
