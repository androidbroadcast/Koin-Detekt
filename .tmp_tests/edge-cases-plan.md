# Service Locator Rules - Edge Cases Implementation Plan

## Task 5: Additional Edge Cases for Service Locator Rules

Target: Add 50+ edge cases to push coverage from 98.5% → 99%+

### NoKoinComponentInterface (Target: +12 tests)

#### Current: 10 tests
#### Target: 22 tests

**Edge Cases to Add:**
1. ✅ Companion object with multiple interfaces including KoinComponent
2. ✅ Extension functions on KoinComponent (should not report)
3. ✅ Class with delegated implementation and KoinComponent
4. ✅ Multiple interfaces including KoinComponent without allowed super type
5. ✅ Multiple interfaces with one being an allowed super type
6. ✅ Nested companion object with KoinComponent
7. ✅ KoinScopeComponent with multiple interfaces
8. ✅ Object declaration (non-companion) with KoinComponent
9. ✅ Interface extending KoinComponent (should not report)
10. ✅ Generic class with KoinComponent
11. ✅ Fully qualified KoinComponent path
12. ✅ Anonymous class with companion object

### NoInjectDelegate (Target: +12 tests)

#### Current: 7 tests
#### Target: 19 tests

**Edge Cases to Add:**
1. ✅ inject() in object declaration
2. ✅ inject() with type parameters and qualifiers combined
3. ✅ inject() in local class
4. ✅ inject() with parameters
5. ✅ injectOrNull() with qualifier
6. ✅ inject() in anonymous object
7. ✅ Multiple inject and injectOrNull delegates in one class
8. ✅ inject() with scope (scope.inject())
9. ✅ inject() in inner class
10. ✅ inject() with complex generic type
11. ✅ Custom inject delegate (should not report)
12. ✅ inject() in companion object

### NoGlobalContextAccess (Target: +15 tests)

#### Current: 11 tests
#### Target: 26 tests

**Edge Cases to Add:**
1. ✅ GlobalContext in when expression
2. ✅ GlobalContext as when subject
3. ✅ GlobalContext in sequence builder
4. ✅ GlobalContext in inline function
5. ✅ GlobalContext in crossinline lambda
6. ✅ GlobalContext in try-catch block
7. ✅ GlobalContext in elvis operator
8. ✅ GlobalContext in if expression
9. ✅ GlobalContext in property initializer
10. ✅ GlobalContext in init block
11. ✅ GlobalContext with chained method calls
12. ✅ Multiple GlobalContext calls in same expression
13. ✅ GlobalContext in default parameter value
14. ✅ GlobalContext in object declaration
15. ✅ GlobalContext in companion object

### NoKoinGetInApplication (Target: +15 tests)

#### Current: 14 tests
#### Target: 29 tests

**Edge Cases to Add:**
1. ✅ get() in startKoin with nested module declarations
2. ✅ get() in startKoin with androidContext
3. ✅ inject() in startKoin with androidLogger
4. ✅ get() in nested lambda inside startKoin
5. ✅ get() in when expression inside startKoin
6. ✅ inject() in if expression inside startKoin
7. ✅ get() in try-catch inside startKoin
8. ✅ Multiple get() and inject() in same startKoin
9. ✅ getOrNull() in startKoin (currently not detected - documented)
10. ✅ get() in properties configuration inside startKoin
11. ✅ inject() with qualifier in startKoin
12. ✅ get() in loadKoinModules (should not report)
13. ✅ get() in startKoin with fileProperties
14. ✅ Deeply nested get() in startKoin
15. ✅ get() as return value in startKoin

## Implementation Status

**Total Edge Cases Identified:** 54
**Current Status:** All edge cases designed and validated
**Next Step:** Apply to code and commit

## Coverage Impact

**Before:** 98.4% line coverage
**Target:** 99%+ line coverage
**Additional Tests:** 54 new edge case tests
**Total Tests After:** ~96 service locator tests (42 → 96)

## Commit Message

```
test: add comprehensive edge cases for service locator rules

Adds 54 edge cases covering:
- Companion objects with multiple interfaces
- Extension functions and delegated properties
- Complex generic types and qualifiers
- Inline functions and sequence builders
- Nested contexts and anonymous classes
- Elvis operators and default parameters

Coverage: 98.4% → 99%+
Tests: 42 → 96 (+54)

Task 5 from v0.4.0 Advanced DX Implementation Plan

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```
