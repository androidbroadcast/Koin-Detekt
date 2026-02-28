# CLAUDE.md — detekt-rules-koin

Detekt extension with Koin-specific static analysis rules for Kotlin.

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
```

Rule set ID: `KoinRuleSet`

## Adding a New Rule

1. Create `src/main/kotlin/io/github/krozov/detekt/koin/<category>/YourRule.kt`
2. Register in `KoinRuleSetProvider.kt` — add `YourRule(config)` to the list
3. Add default config to `src/main/resources/config/config.yml`
4. Write tests in `src/test/kotlin/io/github/krozov/detekt/koin/<category>/YourRuleTest.kt`
5. Document in `docs/rules.md`

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

## Gotchas

- `detekt/` directory is a copy of upstream detekt — don't modify it for this project
- Coverage is enforced — adding rules requires tests with sufficient coverage
- Rule must be added to **both** `KoinRuleSetProvider.kt` and the default config YAML
