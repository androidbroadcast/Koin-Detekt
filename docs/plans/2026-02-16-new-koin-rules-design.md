# New Koin Rules Design Document

**Date:** 2026-02-16
**Status:** Approved
**Author:** Claude Code
**Related Research:** [Koin Issues Research](../research/koin-issues-analysis.md)

---

## Executive Summary

Based on comprehensive research of 150+ GitHub issues in the Koin repository, we identified **15 new rule opportunities** to add to detekt-koin. These rules catch common mistakes, runtime errors, and anti-patterns that developers frequently encounter when using Koin.

**Scope:** 15 new rules organized into 4 thematic groups for parallel implementation
**Effort:** ~2-4 days with 4 parallel agents
**Expected Impact:** Prevent silent bugs, memory leaks, and runtime crashes

---

## Rule Groups

### Group 1: Lifecycle & Resources (3 rules)
**Priority:** HIGH
**Category:** `scope`, `platform`
**Estimated effort:** ~4-6 hours

| Rule | Priority | Koin Issue | Impact |
|------|----------|------------|--------|
| `ViewModelAsSingleton` | HIGH | [#2310](https://github.com/InsertKoinIO/koin/issues/2310) | Coroutine failures after navigation |
| `CloseableWithoutOnClose` | HIGH | [#1790](https://github.com/InsertKoinIO/koin/issues/1790), [#2001](https://github.com/InsertKoinIO/koin/issues/2001) | Resource leaks |
| `ScopeAccessInOnDestroy` | MEDIUM | [#1543](https://github.com/InsertKoinIO/koin/issues/1543), [#1773](https://github.com/InsertKoinIO/koin/issues/1773) | ClosedScopeException |

### Group 2: DSL & Qualifiers (4 rules)
**Priority:** HIGH-MEDIUM
**Category:** `moduledsl`
**Estimated effort:** ~6-8 hours

| Rule | Priority | Koin Issue | Impact |
|------|----------|------------|--------|
| `UnassignedQualifierInWithOptions` | HIGH | [#2331](https://github.com/InsertKoinIO/koin/issues/2331) | Silent dead code |
| `DuplicateBindingWithoutQualifier` | HIGH | [#2115](https://github.com/InsertKoinIO/koin/issues/2115) | Silent override |
| `GenericDefinitionWithoutQualifier` | MEDIUM | [#188](https://github.com/InsertKoinIO/koin/issues/188) | Type erasure, ClassCastException |
| `EnumQualifierCollision` | MEDIUM | [#2364](https://github.com/InsertKoinIO/koin/issues/2364) | Production crash with R8 |

### Group 3: Parameters & Constructors (2 rules)
**Priority:** HIGH-MEDIUM
**Category:** `moduledsl`
**Estimated effort:** ~3-4 hours

| Rule | Priority | Koin Issue | Impact |
|------|----------|------------|--------|
| `ConstructorDslAmbiguousParameters` | HIGH | [#1372](https://github.com/InsertKoinIO/koin/issues/1372), [#2347](https://github.com/InsertKoinIO/koin/issues/2347) | Silent data corruption |
| `ParameterTypeMatchesReturnType` | MEDIUM | [#2328](https://github.com/InsertKoinIO/koin/issues/2328) | Factory never executes |

### Group 4: Startup & Miscellaneous (6 rules)
**Priority:** MEDIUM-LOW
**Category:** `platform`, `moduledsl`, `architecture`
**Estimated effort:** ~4-6 hours

| Rule | Priority | Koin Issue | Impact |
|------|----------|------------|--------|
| `StartKoinInActivity` | MEDIUM | [#1840](https://github.com/InsertKoinIO/koin/issues/1840) | Crash on config change |
| `GetConcreteTypeInsteadOfInterface` | MEDIUM | [#2222](https://github.com/InsertKoinIO/koin/issues/2222) | verify() passes, runtime fails |
| `ExcessiveCreatedAtStart` | LOW | [#2266](https://github.com/InsertKoinIO/koin/issues/2266) | ANR risk |
| `ScopeDeclareWithActivityOrFragment` | LOW | [#1122](https://github.com/InsertKoinIO/koin/issues/1122) | Memory leak |
| `OverrideInIncludedModule` | LOW | [#1919](https://github.com/InsertKoinIO/koin/issues/1919) | Confusing behavior |
| `ModuleAsTopLevelVal` | LOW | DeepWiki best practices | Preallocation issues |

---

## Architecture

### Plan Files Structure

```
docs/plans/
├── 2026-02-16-lifecycle-resources-rules.md      (Group 1)
├── 2026-02-16-dsl-qualifiers-rules.md           (Group 2)
├── 2026-02-16-parameters-constructors-rules.md  (Group 3)
└── 2026-02-16-startup-misc-rules.md             (Group 4)
```

### Implementation Structure

```
src/main/kotlin/io/github/krozov/detekt/koin/
├── scope/
│   ├── ViewModelAsSingleton.kt
│   ├── CloseableWithoutOnClose.kt
│   └── ScopeAccessInOnDestroy.kt
├── moduledsl/
│   ├── UnassignedQualifierInWithOptions.kt
│   ├── DuplicateBindingWithoutQualifier.kt
│   ├── GenericDefinitionWithoutQualifier.kt
│   ├── EnumQualifierCollision.kt
│   ├── ConstructorDslAmbiguousParameters.kt
│   └── ParameterTypeMatchesReturnType.kt
├── platform/
│   ├── StartKoinInActivity.kt
│   └── ScopeDeclareWithActivityOrFragment.kt
└── architecture/
    └── GetConcreteTypeInsteadOfInterface.kt
```

---

## Plan Template

Each implementation plan will follow this structure:

```markdown
# [Group Name] Rules Implementation Plan

## Overview
- Number of rules: X
- Priority: High/Medium/Low
- Detekt categories: [list]
- Estimated effort: ~N hours

## Rules in this group
1. RuleName1 (Priority: HIGH, Issue: #XXXX)
2. RuleName2 (Priority: MEDIUM, Issue: #YYYY)

---

## Rule 1: [RuleName]

### Problem Statement
- **Koin Issue:** [link]
- **Runtime Problem:** [description]
- **Frequency:** Common/Occasional/Rare

### Detection Strategy
```kotlin
// PSI pattern pseudocode
visitCallExpression { expr ->
    if (condition) {
        report("message")
    }
}
```

### Implementation Checklist
- [ ] Create rule file in correct category
- [ ] Implement PSI visitor (override visitXXX())
- [ ] Define issue (id, severity, description, debt)
- [ ] Add configuration (if needed)
- [ ] Register in KoinRuleSetProvider

### Test Matrix
| Test Case | Code Example | Should Report? |
|-----------|--------------|----------------|
| Basic violation | `code` | ✅ Yes |
| Correct usage | `code` | ❌ No |
| Edge case | `code` | ✅/❌ |

### PSI Utilities Needed
- List of helper functions for reuse
- Examples: `isViewModelSubclass()`, `getQualifierName()`

### Documentation Updates
- [ ] docs/rules.md — add rule description
- [ ] README.md — update rule count
- [ ] CHANGELOG.md — add to Unreleased

### Acceptance Criteria
- [ ] Rule detects pattern correctly
- [ ] Tests pass (coverage ≥96%)
- [ ] Documentation updated
- [ ] No false positives

---

[Repeat for each rule in group]
```

---

## Data Flow

### Parallel Agent Workflow

```
┌─────────────────────────────────────────────────────────┐
│ Agent 1: Group 1 (Lifecycle & Resources)               │
│ → worktree: feature/lifecycle-rules                    │
│ → Implements 3 rules                                   │
│ → Creates PR #1                                        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ Agent 2: Group 2 (DSL & Qualifiers)                    │
│ → worktree: feature/dsl-qualifiers-rules               │
│ → Implements 4 rules                                   │
│ → Creates PR #2                                        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ Agent 3: Group 3 (Parameters & Constructors)           │
│ → worktree: feature/parameters-rules                   │
│ → Implements 2 rules                                   │
│ → Creates PR #3                                        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ Agent 4: Group 4 (Startup & Miscellaneous)             │
│ → worktree: feature/startup-misc-rules                 │
│ → Implements 6 rules                                   │
│ → Creates PR #4                                        │
└─────────────────────────────────────────────────────────┘
```

### Agent Execution Steps

1. **Read plan** for assigned group
2. **Create git worktree** with feature branch
3. **For each rule in group:**
   - Implement Rule class with PSI visitor
   - Write tests (TDD approach)
   - Run tests until passing
   - Update documentation
4. **Run full verification:**
   - `./gradlew test` — all tests pass
   - `./gradlew koverVerify` — coverage ≥96%
   - `./gradlew detekt` — no violations
5. **Create PR** with detailed description

### Synchronization Strategy

**Independence:**
- Different worktrees → No file conflicts
- Different rule files → No merge conflicts
- Shared code only in `KoinRuleSetProvider` → Sync at end

**PR Review Process:**
1. Each agent creates separate PR
2. Review each PR individually
3. Merge PRs sequentially (avoid Provider conflicts)
4. Final integration: update README with total rule count

---

## Error Handling

### Problem 1: PSI Pattern More Complex Than Expected

**Symptom:** Agent cannot implement detection logic in reasonable time

**Resolution:**
1. Document blocker in plan
2. Skip rule, mark as `[NEEDS_RESEARCH]`
3. Continue with remaining rules
4. Create separate issue for complex rule

**Example:**
```markdown
## Rule: EnumQualifierCollision [NEEDS_RESEARCH]

**Blocker:** Detekt PSI API doesn't provide direct enum type information.
Requires research into semantic analysis or type resolution.

**Issue created:** #XXX
```

### Problem 2: Tests Below 96% Coverage

**Symptom:** Kover verification fails

**Resolution:**
- Add edge case tests
- If still < 96%, add `@Suppress("coverage")` with WHY comment
- Document uncovered scenarios in PR description

### Problem 3: False Positives

**Symptom:** Rule detects valid code

**Resolution:**
1. Add configuration for exclusions
2. Improve detection logic
3. If impossible → Lower severity to `Style` + document limitation

### Problem 4: KoinRuleSetProvider Merge Conflict

**Symptom:** Multiple agents modified same file

**Resolution:**
```kotlin
class KoinRuleSetProvider : RuleSetProvider {
    override fun instance(config: Config) = RuleSet(
        "koin-rules",
        listOf(
            // ... existing 29 rules ...

            // GROUP 1: Lifecycle & Resources (Agent 1)
            ViewModelAsSingleton(config),
            CloseableWithoutOnClose(config),
            ScopeAccessInOnDestroy(config),

            // GROUP 2: DSL & Qualifiers (Agent 2)
            UnassignedQualifierInWithOptions(config),
            DuplicateBindingWithoutQualifier(config),
            GenericDefinitionWithoutQualifier(config),
            EnumQualifierCollision(config),

            // GROUP 3: Parameters & Constructors (Agent 3)
            ConstructorDslAmbiguousParameters(config),
            ParameterTypeMatchesReturnType(config),

            // GROUP 4: Startup & Miscellaneous (Agent 4)
            StartKoinInActivity(config),
            GetConcreteTypeInsteadOfInterface(config),
            ExcessiveCreatedAtStart(config),
            ScopeDeclareWithActivityOrFragment(config),
            OverrideInIncludedModule(config),
            ModuleAsTopLevelVal(config),
        )
    )
}
```

Last agent to merge reconciles all blocks.

---

## Testing Strategy

### Test File Structure

```
src/test/kotlin/io/github/krozov/detekt/koin/
├── scope/
│   ├── ViewModelAsSingletonTest.kt
│   ├── CloseableWithoutOnCloseTest.kt
│   └── ScopeAccessInOnDestroyTest.kt
├── moduledsl/
│   ├── UnassignedQualifierInWithOptionsTest.kt
│   ├── DuplicateBindingWithoutQualifierTest.kt
│   ├── GenericDefinitionWithoutQualifierTest.kt
│   ├── EnumQualifierCollisionTest.kt
│   ├── ConstructorDslAmbiguousParametersTest.kt
│   └── ParameterTypeMatchesReturnTypeTest.kt
└── platform/
    └── StartKoinInActivityTest.kt
```

### Test Template

```kotlin
class RuleNameTest {
    private val rule = RuleName(Config.empty)

    @Test
    fun `reports when [violation condition]`() {
        val code = """
            // Bad code example
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("expected message")
    }

    @Test
    fun `does not report when [correct usage]`() {
        val code = """
            // Good code example
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles edge case [specific scenario]`() {
        // Edge case test
    }
}
```

### Coverage Requirements

| Metric | Minimum | Target |
|--------|---------|--------|
| Line coverage | 96% | 98% |
| Branch coverage | 70% | 85% |
| Tests per rule | 3 | 5+ |

### Test Categories

1. **Happy path violations** — основные случаи срабатывания
2. **Happy path valid code** — правило не срабатывает на корректном коде
3. **Edge cases:**
   - Generic types: `List<T>`, nullable types
   - Nested structures: companion objects, inner classes
   - Kotlin features: extension functions, lambda receivers
4. **Configuration tests** — если правило конфигурируемое
5. **Integration tests** — совместная работа с другими правилами

### Verification Commands

```bash
# Agent runs before creating PR:
./gradlew test              # All tests must pass
./gradlew koverHtmlReport   # Generate coverage report
./gradlew koverVerify       # Enforces 96%/70% thresholds
./gradlew detekt            # No violations in new code
```

If coverage < 96% → PR blocked by CI.

---

## Success Metrics

### Completion Criteria

- [ ] All 4 implementation plans created
- [ ] All 15 rules implemented (or documented as `[NEEDS_RESEARCH]`)
- [ ] All tests passing with ≥96% coverage
- [ ] All 4 PRs created and reviewed
- [ ] Documentation updated:
  - [ ] docs/rules.md — 15 new rule descriptions
  - [ ] README.md — Updated rule count (29 → 44)
  - [ ] CHANGELOG.md — Unreleased section populated
- [ ] Integration verified — all rules work together

### Quality Gates

Each PR must pass:
- ✅ CI build green
- ✅ Test coverage ≥96%
- ✅ No detekt violations
- ✅ No false positives in self-dogfooding
- ✅ Documentation complete
- ✅ Code review approved

---

## Timeline Estimate

**Sequential (1 developer):** 12-16 hours
**Parallel (4 agents):** 3-4 hours (wall clock time)

### Breakdown by Group

| Group | Rules | Complexity | Time (Sequential) | Time (Parallel) |
|-------|-------|------------|-------------------|-----------------|
| Group 1 | 3 | Medium | 4-6 hours | 4-6 hours |
| Group 2 | 4 | Medium-High | 6-8 hours | 6-8 hours |
| Group 3 | 2 | Medium | 3-4 hours | 3-4 hours |
| Group 4 | 6 | Low-Medium | 4-6 hours | 4-6 hours |

**With 4 parallel agents:** Wall clock time = longest group = ~6-8 hours

---

## Next Steps

1. ✅ Design approved
2. **Create 4 detailed implementation plans** (next phase)
3. Launch 4 parallel agents with plans
4. Review and merge PRs
5. Final integration and release

---

## Appendix: Research Summary

Full research findings available in agent output (Task ab5d8da).

**Key Insights:**
- 150+ Koin GitHub issues analyzed
- 15 high-impact rule opportunities identified
- Common mistake categories: lifecycle, qualifiers, parameters, startup
- Most rules prevent **silent bugs** (no compile-time error, runtime failure)

**Related Issues:**
- See individual rule sections for Koin issue links
- All rules based on real production bugs reported by users
