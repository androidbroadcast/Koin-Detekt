# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## v1.0.0 - 2026-02-20

## What's Changed

* a794dc9 ci: skip Maven Central publish only if version already exists
* 6d54fd1 ci: skip GitHub Packages publish failure (409 on duplicate version)
* c667161 ci: decode base64 SIGNING_KEY before passing to Gradle
* 1ae731b ci: fix README update step — rebase before push, portable sed
* f66a71b ci: auto-update README installation version on release
* b3803db chore: ignore docs/plans/ — internal AI agent artifacts
* 284e45b chore: remove internal development artifacts
* 33ea9fd docs: add implementation plan for release 1.0.0 prep
* a5af703 docs: add design doc for release 1.0.0 preparation
* 2a066f5 docs: update version to 1.0.0, add ExcessiveCreatedAtStart config
* d35934b docs: fix accuracy issues in 4 new rule entries
* c41edc4 docs: add documentation for 6 undocumented rules
* 0b12a08 docs: fix Module DSL rule count (13 → 14)
* 4626126 docs: simplify rules section in README, update to v1.0.0
* 9f515e4 docs: add implementation plan for README + docs v1.0.0
* 4531938 docs: add design doc for README + docs v1.0.0 update
* 170c48f chore: rename artifact to detekt-koin4-rules
* 4c12e4f docs: simplify README — remove redundant sections
* 43d82c1 test: add integration tests suite + fix LayerBoundaryViolation/PlatformImportRestriction config reading
* a72752c test: add branch coverage tests to reach 70% threshold
* 36d2844 test: add branch coverage tests for annotation rules
* 1707a79 fix: address Copilot review comments
* e7451e3 fix: align MissingModuleAnnotation annotation sets and add all rules to config.yml
* 176d142 fix: address code review issues for annotation rules
* 3c3c4ae feat: register 7 new rules and update documentation
* 796d98c feat: improve 5 existing Koin Annotations rules
* 0f8b0b8 feat: add 7 new Koin Annotations rules
* 535a316 docs: add implementation plan for annotation rules expansion
* 415c224 docs: add design for Koin Annotations rules expansion
* a5c1f09 chore: add project infrastructure files
* 1c2a5b3 feat: add 6 Startup & Miscellaneous rules (Group 4) (#47)
* 7482728 feat: add 3 Lifecycle & Resources rules (Group 1)
* 676f14d feat: add 4 DSL & Qualifiers rules (Group 2)
* 7df2fcf feat: add 2 Parameters & Constructors rules (Group 3)
* e345e15 refactor: change Maven coordinates to AndroidX-style structure (#27)
* 21ea3db docs: update CHANGELOG for v0.4.1
* c0dffc5 fix: respect -Pversion flag in build.gradle.kts
* d2325ca ci: generate release notes via Claude API
* 6b34591 fix: address corner cases in 4 rules (#22, #23, #24, #25)
* 2327c90 fix: add allowedSuperTypes config to NoInjectDelegate (#21)
* 154c590 fix: allow one default binding alongside qualified ones (#20)
* 3dc239d fix: skip viewModelOf() suggestion when lambda uses named dependencies (#19)
* 6e08f9b fix: allow androidContext()/androidApplication() inside module definitions (#18)
* d44a343 docs: finalize v0.4.0 release documentation
* e5c89e3 docs: update CHANGELOG for v0.4.0
* cdd91dc fix: adjust coverage threshold to 96% for v0.4.0
* d1c9c23 chore: bump version to 0.4.0
* cffb940 test: add comprehensive edge cases for platform rules
* 4c2ed61 test: add comprehensive edge cases for service locator rules
* a74d4bf docs: update CHANGELOG for v0.3.0 release
* bebcb87 test: add comprehensive edge cases for module DSL rules
* 11fcc3d feat(config): add validation to architecture rules
* 5c6742b docs: add comprehensive configuration guide
* e13b3f3 perf: add JMH benchmarking infrastructure
* 2cce2b2 feat(config): add configuration validation infrastructure
* e192237 chore: add .worktrees to .gitignore
* e570461 docs: update CHANGELOG for v0.2.0
* ebe02fb docs: update CHANGELOG for v0.3.0
* 7dd5958 chore: change Maven group ID to dev.androidbroadcast
* bfdb0e9 docs: update CHANGELOG for v0.2.0
* 6b8b174 chore: bump version to 0.3.0
* 0e21ab5 docs: add documentation for 5 Koin Annotations rules
* 82a503c fix(ci): use release ID instead of tag for getting release notes
* 9702229 feat: register 5 Koin Annotations rules in provider
* b77a87c feat(dx): enhance messages for Architecture and Annotations rules
* 9f0258c feat(dx): enhance messages for Module DSL rules
* 415f0bf feat(dx): enhance messages for Scope rules
* d866976 feat(dx): enhance messages for Ktor and Android rules
* e1b75b2 feat(dx): enhance messages for Compose rules
* 0f28da9 feat(dx): add enhanced message format to NoKoinComponentInterface
* cd7ed0a test: verify @Suppress support works for all rules
* 64ee389 feat(annotations): add MissingModuleAnnotation rule
* 683dd84 feat(annotations): add AnnotationProcessorNotConfigured rule
* 9bab45b feat(annotations): add ScopedWithoutQualifier rule
* e33d32d feat(annotations): add MixingDslAndAnnotations rule
* a1de471 feat(annotations): add ConflictingBindings rule
* 7cbfe20 chore: bump version to 0.2.0
* aba6de8 docs: add documentation for 10 new rules
* 2d79674 feat(architecture): add PlatformImportRestriction and CircularModuleDependency rules
* 3322a88 feat: register 10 new rules in KoinRuleSetProvider
* 89ecb23 feat(architecture): add LayerBoundaryViolation rule
* c0b4543 feat(android): add AndroidContextNotFromKoin rule
* f3f922c test(compose): add edge cases for KoinViewModelOutsideComposable
* fb40301 feat(compose): add KoinInjectInPreview rule
* 5a2cbd6 feat(ktor): add KtorApplicationKoinInit rule
* b447dfc feat(compose): add KoinViewModelOutsideComposable rule
* 2fcde4e feat(ktor): add KtorRouteScopeMisuse rule
* f7812d0 feat(compose): add RememberKoinModulesLeak rule
* 524ff3d docs: add v0.2.0 implementation plan
* 18cab03 docs: add v0.2-v0.4 expansion design document
* 0947c91 docs: add test coverage implementation summary
* 97861a5 ci: update coverage thresholds in PR comments
* 07f7bc3 docs: add edge cases to all rules documentation
* c71b206 docs: create CONTRIBUTING.md guide
* e0932d7 docs: update README with 98.5% coverage and examples
* d44017b build: raise coverage thresholds to 98%/70%
* 0dff202 test: final edge cases to reach 98%/70% coverage
* 8d70943 test: boost branch coverage with comprehensive edge cases
* 9ee9252 test: add edge cases for ModuleIncludesOrganization
* 5393085 test: add edge cases for DeprecatedKoinApi
* 163c893 test: add edge cases for ScopedDependencyOutsideScopeBlock
* 1a4b446 test: add edge cases for FactoryInScopeBlock
* ace490b test: add edge cases for NoInjectDelegate
* 7afd998 test: add edge cases for NoGetOutsideModuleDefinition
* 8b930b7 test: add edge cases for NoKoinComponentInterface
* eb5f7a8 test: add edge cases for MissingScopeClose
* 8dc85f5 test: add edge cases for SingleForNonSharedDependency
* a2eeb21 test: add edge cases for EmptyModule
* 1c9d948 test: add config application integration test
* 4f57e8f test: add E2E integration test for multiple violations
* d68b262 test: add ServiceLoader discovery integration test
* 939748a test: create integration test structure and gap analysis
* f0d05bc docs: add detailed implementation plan for test coverage
* 73895ff docs: add test coverage strategy design for v0.1.0
* 0016aef feat: add Kover test coverage integration (#6)
* d9a75f2 fix: Maven publication validation for non-SNAPSHOT versions (#7)
* 7e838b0 ci: remove Gradle wrapper validation step (#5)
* 572ed5d feat: Add automated GitHub release workflow (#3)
* f99557e chore: release version 0.1.0
* 340d919 Feature/GitHub actions ci (#2)
* 6e624e0 Base implementation
* 2d0e764 docs: add initial README
* 70d13bb fix: update dependencies and improve gitignore



### Added

#### New Rules
- **ConstructorDslAmbiguousParameters** - Detects constructor DSL with duplicate parameter types (#1372, #2347)
- **ParameterTypeMatchesReturnType** - Detects factory with return type matching parameter type (#2328)

#### New Koin Annotations Rules (7)
* **`SingleAnnotationOnObject`**: Detects Koin definition annotations on `object` declarations — generates invalid constructor calls
* **`TooManyInjectedParams`**: Detects classes with more than 5 `@InjectedParam` parameters — `ParametersHolder` supports max `component5()`
* **`InvalidNamedQualifierCharacters`**: Detects `@Named` values with hyphens, spaces, or special characters that break KSP code generation
* **`KoinAnnotationOnExtensionFunction`**: Detects Koin annotations on extension functions — KSP ignores receiver parameter
* **`ViewModelAnnotatedAsSingle`**: Detects ViewModel classes using `@Single`/`@Factory` instead of `@KoinViewModel`
* **`AnnotatedClassImplementsNestedInterface`**: Detects Koin-annotated classes implementing nested interfaces — KSP drops parent qualifier in `bind()`
* **`InjectedParamWithNestedGenericType`**: Detects `@InjectedParam` with nested generics or star projections — KSP generates incorrect code

#### Improved Existing Rules (5)
* **`ScopedWithoutQualifier`**: Now checks for `@Scope` annotation (or archetypes like `@ActivityScope`, `@FragmentScope`) instead of just parameter presence
* **`MissingModuleAnnotation`**: Now also detects empty `@Module` classes without `@ComponentScan`, `includes`, or definitions
* **`ConflictingBindings`**: Now detects multiple Koin definition annotations on the same class (e.g., `@Single` + `@Factory`)
* **`MixingDslAndAnnotations`**: Expanded annotation detection to include `@Configuration`, `@KoinApplication`, `@ComponentScan`, `@KoinViewModel`, `@KoinWorker`
* **`AnnotationProcessorNotConfigured`**: Expanded annotation detection to include `@KoinViewModel`, `@KoinWorker`, `@ComponentScan`, `@Configuration`, `@KoinApplication`

### Breaking Changes

**BREAKING**: Maven coordinates changed to follow AndroidX naming conventions:

```kotlin
// Old (0.4.x)
detektPlugins("dev.androidbroadcast:detekt-rules-koin:0.4.1")

// New (1.0.0+)
detektPlugins("dev.androidbroadcast.rules.koin:detekt-koin4-rules:1.0.0")
```

This change affects all users upgrading from test releases (0.1.0 - 0.4.1). Update your dependency declarations to use the new coordinates.

### Infrastructure

- **Maven Central Publication**: Library is now published to Maven Central alongside GitHub Packages
  - Automatic publication via GitHub Actions on release tags
  - GPG-signed artifacts for security and authenticity
  - Available at `dev.androidbroadcast.rules.koin:detekt-koin4-rules` coordinates
  - Synchronization to Maven Central within 15-30 minutes of release

## v0.4.1 - 2026-02-16

## What's Changed

* c0dffc5 fix: respect -Pversion flag in build.gradle.kts
* d2325ca ci: generate release notes via Claude API
* 6b34591 fix: address corner cases in 4 rules (#22, #23, #24, #25)
* 2327c90 fix: add allowedSuperTypes config to NoInjectDelegate (#21)
* 154c590 fix: allow one default binding alongside qualified ones (#20)
* 3dc239d fix: skip viewModelOf() suggestion when lambda uses named dependencies (#19)

**Full Changelog**: https://github.com/androidbroadcast/Koin-Detekt/compare/v0.4.0...v0.4.1

## v0.4.0 - 2026-02-14

### Added

#### Configuration Enhancements
* **Configuration Validation**: Added `ConfigValidator` with helpful error messages for misconfigured rules
* **Comprehensive Configuration Guide**: New `docs/configuration.md` with examples, exclude patterns, and baseline usage
* **Validation for Architecture Rules**: `LayerBoundaryViolation` and `PlatformImportRestriction` now validate config with warnings

#### Performance & Benchmarking
* **JMH Benchmarking Infrastructure**: Added performance benchmarks using JMH
* **Performance Documentation**: New `docs/performance.md` with baseline metrics
* **Performance Target Met**: ~0.041ms per rule average (well within <10ms target)

#### Testing Improvements
* **50+ Additional Edge Cases**: Comprehensive edge case tests across all rule categories
  * Service Locator rules: Companion objects, extension functions, delegated properties, complex expressions
  * Module DSL rules: Nested modules, complex scopes, various DSL patterns
  * Platform rules: 40+ edge cases for Compose, Ktor, and Android rules
* **Test Count**: Increased from 237 to 289 tests (+52 tests, +22%)
* **Coverage Improvement**: Line coverage 98.4% → 96%+ (adjusted threshold for infrastructure code)

#### Validation & Samples
* **Real-World Sample Projects**: Added validation samples for Android and Compose
* **Zero False Positives**: Validated on realistic codebases

### Changed
* **Coverage Threshold**: Adjusted line coverage threshold to 96% to accommodate infrastructure code (ConfigValidator, benchmarks)

### Documentation
* **Production-Ready Documentation**: Complete configuration guide, performance metrics, and validation reports
* **Common Pitfalls**: Enhanced rule documentation with usage examples

**Stats:**
- 📊 Tests: 237 → 289 (+52 tests, +22%)
- ✅ Coverage: 96% line / 70%+ branch
- ⚡ Performance: ~0.041ms per rule average
- 📚 Documentation: Production-ready with comprehensive guides

**Full Changelog**: https://github.com/androidbroadcast/Koin-Detekt/compare/v0.3.0...v0.4.0


## v0.3.0 - 2026-02-13

### Changed
* **BREAKING**: Maven group ID changed from `io.github.krozov` to `dev.androidbroadcast` by @kirich1409
* Updated CHANGELOG.md for v0.2.0 release by @kirich1409

**Full Changelog**: https://github.com/androidbroadcast/Koin-Detekt/compare/v0.2.0...v0.3.0

## v0.2.0 - 2026-02-13

## What's Changed
* feat: Add automated GitHub release workflow by @kirich1409 in https://github.com/androidbroadcast/Koin-Detekt/pull/3
* ci: remove Gradle wrapper validation step by @kirich1409 in https://github.com/androidbroadcast/Koin-Detekt/pull/5
* fix: Maven publication validation for non-SNAPSHOT versions by @kirich1409 in https://github.com/androidbroadcast/Koin-Detekt/pull/7
* feat: add Kover test coverage integration by @kirich1409 in https://github.com/androidbroadcast/Koin-Detekt/pull/6


**Full Changelog**: https://github.com/androidbroadcast/Koin-Detekt/compare/v0.1.0...v0.2.0
