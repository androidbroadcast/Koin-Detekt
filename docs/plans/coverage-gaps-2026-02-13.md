# Coverage Gap Analysis

**Date:** 2026-02-13
**Current Coverage:** 94.67% line, ~55% branch (from README badge)

## Analysis Results

Coverage report: `build/reports/kover/html/index.html`

## Current Status

✅ **Strong Coverage:**
- All 14 rules have basic unit tests
- Positive and negative cases covered
- Provider class tested

❌ **Gaps Identified:**

### Missing Test Types
1. **No integration tests** — ServiceLoader discovery not tested
2. **Branch coverage low (~55%)** — many conditional paths untested
3. **Edge cases incomplete** — complex scenarios not covered

### Priority Edge Cases to Add

#### High Priority (Uncovered Branches)
1. **Generic types** — `<T>`, `<out T>`, `<in T>` in supertype checks
2. **Qualified names** — Full package paths vs short names
3. **Nested structures** — Classes in classes, lambdas in lambdas
4. **Conditional logic** — if/when branches in rules
5. **Multiple violations** — Several issues in one file

#### Medium Priority (Better Coverage)
1. **Comments-only blocks** — Empty with comments
2. **Companion objects** — KoinComponent in companion
3. **Property initializers** — `get()` in property init
4. **Multiple scopes** — Several createScope() calls
5. **Custom configurations** — All configurable parameters

#### Lower Priority (Edge Syntax)
1. **Trailing commas**
2. **Multiline expressions**
3. **Type aliases**
4. **Anonymous classes**

## Target Coverage

**Goal:** 98%+ line, 90%+ branch

**Strategy:**
1. Add 3 integration tests → +1% line coverage
2. Add ~30-40 edge cases → +3% line coverage, +35% branch coverage
3. Target uncovered branches systematically

## Implementation Order

### Phase 1: Integration Tests (Priority: CRITICAL)
- ServiceLoader discovery
- E2E multi-violation
- Config application

**Expected gain:** +1% line, +5% branch

### Phase 2: High-Priority Edge Cases (Priority: HIGH)
Focus on rules with lowest branch coverage:
1. NoKoinComponentInterface
2. EmptyModule
3. MissingScopeClose
4. SingleForNonSharedDependency

**Expected gain:** +2% line, +20% branch

### Phase 3: Systematic Edge Cases (Priority: MEDIUM)
Remaining 10 rules, 2-3 edge cases each

**Expected gain:** +1% line, +10% branch

### Phase 4: Polish (Priority: LOW)
Fill remaining gaps identified in coverage report

**Expected gain:** +0.5% line, +5% branch

## Success Criteria

- [ ] Line coverage ≥ 98%
- [ ] Branch coverage ≥ 90%
- [ ] All integration tests passing
- [ ] All edge cases documented in docs/rules.md

---

**Status:** ✅ Analysis complete
**Next:** Begin implementation (Task 2)
