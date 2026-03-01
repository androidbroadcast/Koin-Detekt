# CLAUDE.md — detekt-rules-koin

Detekt extension with Koin-specific static analysis rules for Kotlin.

## Code Search (ast-index)

For fast structural search use `ast-index` — 17-69x faster than grep.

```bash
ast-index search "visitAnnotationEntry"   # universal search
ast-index class "QualifierObfuscationRisk"
ast-index implementations "Rule"          # all rules in the project
ast-index usages "KoinAnnotationConstants"
```

After first clone run `ast-index rebuild` once. If you use Claude Code with hooks configured, `ast-index` updates after git operations automatically; otherwise run `ast-index update` manually after `git pull`, `checkout`, or `merge`.

## Commands

```bash
# Build
./gradlew build

# Test
./gradlew test
./gradlew test --tests "EmptyModuleTest"
./gradlew test --tests "io.github.krozov.detekt.koin.moduledsl.*"

# Full check (build + tests + detekt self-analysis)
./gradlew check

# Code coverage
./gradlew koverHtmlReport   # HTML report at build/reports/kover/html/
./gradlew koverVerify       # Fails build if coverage drops
```

## Architecture

Single-module Gradle project. JDK 21, Kotlin 2.0.21, Detekt API 1.23.8.

```
src/main/kotlin/io/github/krozov/detekt/koin/
├── KoinRuleSetProvider.kt      ← Entry point — registers ALL rules
├── annotations/                ← Koin Annotations rules (17 rules)
├── architecture/               ← Architecture boundary rules (4 rules)
├── moduledsl/                  ← Module DSL rules (14 rules)
├── platform/
│   ├── android/                ← Android-specific rules
│   ├── compose/                ← Jetpack Compose rules
│   └── ktor/                   ← Ktor rules
├── scope/                      ← Scope lifecycle rules (8 rules)
└── servicelocator/             ← Service locator anti-patterns (5 rules)
└── util/                       ← Shared utilities (import resolution, type utils)
```

Rule set ID: `KoinRuleSet`

## Adding a New Rule

1. Create `src/main/kotlin/io/github/krozov/detekt/koin/<category>/YourRule.kt`
2. Register in `KoinRuleSetProvider.kt` — add `YourRule(config)` to the list
3. Add default config to `src/main/resources/config/config.yml`
4. Add entry to `config/detekt-koin-all-rules.yml` ← **often forgotten, also required**
5. Write tests in `src/test/kotlin/io/github/krozov/detekt/koin/<category>/YourRuleTest.kt`
6. Document in `docs/rules.md`

Rule skeleton:
```kotlin
internal class YourRule(config: Config) : Rule(config) {
    override val issue = Issue(
        id = "YourRule",
        severity = Severity.Warning,
        description = "...",
        debt = Debt.FIVE_MINS
    )
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        // implementation
    }
}
```

## Available Utilities

- `KoinAnnotationConstants` — `DEFINITION_ANNOTATIONS`, `PROVIDER_ANNOTATIONS`, `ALL_ANNOTATIONS` (short names, no import check)
- `ImportAwareRule` — base class providing `importContext: FileImportContext`; call `importContext.resolveKoin(name)` → `Resolution.KOIN / NOT_KOIN / UNKNOWN` to disambiguate same-named annotations
- `KoinSymbols` — canonical Koin annotation/DSL names and `KOIN_PACKAGES`
- `TypeNameUtils` — `stripTypeMetadata()` / `typeArgumentsText()` for handling generic type strings
- `ConfigExtensions` — `config.value(key, default, deprecatedKey?)` for backward-compatible parameters

## Testing

Tests use JUnit 5 + AssertJ. Each rule has its own test file.

```kotlin
class YourRuleTest {
    @Test
    fun `detects violation`() {
        val findings = YourRule(Config.empty).lint("""
            // Kotlin code
        """.trimIndent())
        assertThat(findings).hasSize(1)
    }
}
```

## Self-Dogfooding

The project runs its own Koin rules on itself. Config: `detekt-config.yml`.
`./gradlew check` includes this self-analysis — it must pass before merging.

## Branch Naming

- `feature/rule-name` — new rule
- `fix/issue-description` — bug fix
- `docs/topic` — documentation

Commit style: [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, etc.)

## Config Parameter Backward Compatibility

Config parameter names are **public API** — users embed them in their `detekt.yml` and upgrades must not silently break their configuration.

### Rules

- Config parameter names must not be renamed or removed in a minor/patch release
- Deprecation requires a two-phase cycle: warn in one minor version, remove in the next major

### Reading Config in Rules

Use `Config.value()` from `util/ConfigExtensions.kt` for all configurable parameters:

```kotlin
import io.github.krozov.detekt.koin.util.value

// New parameter (no prior key):
private val myParam: List<String> =
    config.value(key = "myParam", default = emptyList())

// Phase 1 — rename "oldParam" to "newParam" (minor release):
private val newParam: List<String> = config.value(
    key = "newParam",
    default = emptyList(),
    deprecatedKey = "oldParam"   // reads old key with a stderr warning
)

// Phase 2 — next major release: drop deprecatedKey, use plain value()
```

Update `config.yml` in Phase 1 to document the migration:
```yaml
MyRule:
  active: true
  newParam: []          # replaces deprecated 'oldParam' (removed in X.0.0)
  # oldParam: []        # DEPRECATED — rename to newParam
```

Do **not** use `by config()` delegate for new parameters — it bypasses the deprecation mechanism.

## Rule Quality Checklist

Before implementing a rule, verify its premise:

1. **Verify the Koin behaviour** against official docs or source — not assumptions. Several rule ideas were rejected because the described problem doesn't actually occur:
   - `@Scoped` supports `binds=` exactly like `@Single` and `@Factory`
   - KSP ignores un-annotated nested classes — no "confusion" occurs
   - `@ComponentScan` takes `vararg String`, not a single value

2. **No false positives > no analysis.** A rule that fires on correct code is worse than no rule at all.

3. **Package matching** — `startsWith` is substring-unsafe. Always use:
   ```kotlin
   pkg == other || pkg.startsWith("$other.") || other.startsWith("$pkg.")
   ```

4. **Short-name annotation matching** is a known limitation. Generic names (`Qualifier`, `Module`, `Inject`) collide with non-Koin libraries. Document known false-positive risk in tests.

5. **Tests must cover:**
   - The primary detection case
   - At least one negative case (should not report)
   - Edge cases relevant to the detection logic (vararg, companion objects, etc.)

## Gotchas

- `detekt/` directory is a copy of upstream detekt — don't modify it for this project
- Companion object detection: use `KtObjectDeclaration.isCompanion()`, not `klass.companionObjects.toHashSet()` membership check
- Coverage is enforced — adding rules requires tests with sufficient coverage
- Rule must be added to **both** `KoinRuleSetProvider.kt` **and both config YAMLs**:
  - `src/main/resources/config/config.yml` (default, bundled)
  - `config/detekt-koin-all-rules.yml` (user-facing reference config)
