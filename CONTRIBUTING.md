# Contributing to detekt-rules-koin

Thank you for your interest in contributing to detekt-rules-koin! This document provides guidelines and instructions for contributing to this project.

## Table of Contents

- [Development Setup](#development-setup)
- [Running Tests](#running-tests)
- [Code Coverage Requirements](#code-coverage-requirements)
- [Adding New Rules](#adding-new-rules)
- [Pull Request Process](#pull-request-process)
- [Code Quality Standards](#code-quality-standards)
- [Questions and Help](#questions-and-help)

## Development Setup

### Prerequisites

- **JDK 21** - Java Development Kit 21 or later
- **Gradle 8.x** - The project uses Gradle 8.x (wrapper included)
- **Git** - For version control

### Clone and Build

1. **Fork the repository** on GitHub

2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Koin-Detekt.git
   cd Koin-Detekt
   ```

3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/androidbroadcast/Koin-Detekt.git
   ```

4. **Build the project**:
   ```bash
   ./gradlew build
   ```

5. **Verify everything works**:
   ```bash
   ./gradlew check
   ```

### IDE Setup

This project works well with:
- **IntelliJ IDEA** (recommended) - Import as a Gradle project
- **Android Studio** - Import as a Gradle project
- **VS Code** - With Kotlin extension

The project uses:
- Kotlin 2.0.21
- Detekt API 1.23.8
- JUnit 5 for testing
- AssertJ for assertions
- Kover for code coverage

## Running Tests

### Execute All Tests

```bash
./gradlew test
```

Tests run in parallel for faster execution (configured to use half of available CPU cores).

### Run Specific Test Classes

```bash
./gradlew test --tests "EmptyModuleTest"
./gradlew test --tests "io.github.krozov.detekt.koin.moduledsl.*"
```

### Test Output

The test task provides detailed output:
- Passed, skipped, and failed tests
- Full exception stack traces
- Test duration and summary

View the HTML test report at: `build/reports/tests/test/index.html`

### Generate Code Coverage Reports

```bash
./gradlew koverHtmlReport
```

View the coverage report at: `build/reports/kover/html/index.html`

### Verify Coverage Thresholds

```bash
./gradlew koverVerify
```

Or run the full check (includes tests + coverage verification):

```bash
./gradlew check
```

## Code Coverage Requirements

This project maintains **high code coverage standards** to ensure quality and reliability.

### Minimum Coverage Thresholds

- **Line Coverage**: **98%** minimum
- **Branch Coverage**: **70%** minimum

### Coverage Verification

Coverage verification runs automatically as part of:
```bash
./gradlew check
```

If coverage falls below the thresholds, the build will fail with a detailed report showing which areas need additional testing.

### Before Submitting a Pull Request

1. **Run tests**:
   ```bash
   ./gradlew test
   ```

2. **Generate coverage report**:
   ```bash
   ./gradlew koverHtmlReport
   ```

3. **Verify coverage meets requirements**:
   ```bash
   ./gradlew koverVerify
   ```

4. **Check the HTML report** at `build/reports/kover/html/index.html` to identify any uncovered lines

### Coverage Exclusions

The following are excluded from coverage:
- Test code (`src/test/**`)
- Generated code (`*BuildConfig*`)
- Test utilities in main source set

### Testing Best Practices

- Write **unit tests** for all new rules
- Include **edge cases** and **negative cases**
- Test **rule configuration** options
- Add **integration tests** for complex scenarios
- Ensure tests are **deterministic** and **isolated**

## Adding New Rules

Follow these steps to add a new Detekt rule for Koin:

### 1. Determine Rule Category

Rules are organized into three categories:

- **`servicelocator/`** - Service locator anti-patterns (e.g., `KoinComponent`, `get()` misuse)
- **`moduledsl/`** - Module DSL rules (e.g., empty modules, single vs factory)
- **`scope/`** - Scope management rules (e.g., scope lifecycle, scoped dependencies)

### 2. Create Rule Implementation

Create a new Kotlin file in the appropriate package:

```kotlin
package io.github.krozov.detekt.koin.[category]

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*

/**
 * This rule detects [brief description of what it detects].
 *
 * Example of non-compliant code:
 * ```kotlin
 * // Bad example
 * ```
 *
 * Example of compliant code:
 * ```kotlin
 * // Good example
 * ```
 */
internal class YourNewRule(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "YourNewRule",
        severity = Severity.Warning,  // or Severity.Style, Severity.Minor, etc.
        description = "Clear, concise description of what this rule detects and why it matters",
        debt = Debt.FIVE_MINS  // Estimated time to fix the issue
    )

    // Optional: Configuration properties
    private val configProperty: String by config("default-value")

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        // Rule implementation logic
        // Check for violations and report them
        if (isViolation(expression)) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Detailed message explaining the violation"
                )
            )
        }
    }

    private fun isViolation(expression: KtCallExpression): Boolean {
        // Implementation logic
        return false
    }
}
```

### 3. Write Comprehensive Tests

Create a test file in `src/test/kotlin/io/github/krozov/detekt/koin/[category]/YourNewRuleTest.kt`:

```kotlin
package io.github.krozov.detekt.koin.[category]

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class YourNewRuleTest(private val env: KotlinCoreEnvironment) {

    private fun subject(config: Config = Config.empty) = YourNewRule(config)

    @Test
    fun `reports violation when pattern is detected`() {
        val code = """
            // Code that should trigger the rule
        """.trimIndent()

        val findings = subject().compileAndLint(code)

        findings shouldHaveSize 1
        findings.first().message shouldContain "expected message"
    }

    @Test
    fun `does not report when code is compliant`() {
        val code = """
            // Code that should NOT trigger the rule
        """.trimIndent()

        val findings = subject().compileAndLint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `handles edge case correctly`() {
        val code = """
            // Edge case code
        """.trimIndent()

        val findings = subject().compileAndLint(code)

        findings.shouldBeEmpty()
    }

    // Add more test cases covering:
    // - Different violation scenarios
    // - Configuration options
    // - Edge cases
    // - Integration with real Koin code patterns
}
```

### 4. Register Rule in Provider

Add your rule to `KoinRuleSetProvider.kt`:

```kotlin
import io.github.krozov.detekt.koin.[category].YourNewRule

public class KoinRuleSetProvider : RuleSetProvider {
    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                // ... existing rules ...
                YourNewRule(config),  // Add your rule here
            )
        )
    }
}
```

### 5. Document the Rule

Add documentation to `docs/rules.md`:

```markdown
### YourNewRule

**Severity**: Warning
**Default**: Active
**Category**: [Service Locator / Module DSL / Scope Management]

**Description**:
Clear explanation of what the rule detects and why it matters.

**Non-compliant Code**:
```kotlin
// Bad example
```

**Compliant Code**:
```kotlin
// Good example
```

**Configuration**:
```yaml
koin-rules:
  YourNewRule:
    active: true
    configProperty: 'custom-value'
```
```

### 6. Verify Everything Works

1. **Run tests**:
   ```bash
   ./gradlew test
   ```

2. **Check coverage**:
   ```bash
   ./gradlew koverHtmlReport
   ./gradlew koverVerify
   ```

3. **Build the project**:
   ```bash
   ./gradlew build
   ```

4. **Run the full check**:
   ```bash
   ./gradlew check
   ```

## Pull Request Process

### Branch Naming

Use descriptive branch names following this pattern:

- `feature/rule-name` - For new rules (e.g., `feature/no-lazy-koin-inject`)
- `fix/issue-description` - For bug fixes (e.g., `fix/empty-module-false-positive`)
- `docs/topic` - For documentation (e.g., `docs/contributing-guide`)
- `refactor/description` - For refactoring (e.g., `refactor/extract-koin-utils`)
- `test/description` - For test improvements (e.g., `test/add-integration-tests`)
- `ci/description` - For CI/CD changes (e.g., `ci/add-codecov`)

### Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): brief description

Detailed explanation (optional)

Fixes #123
```

**Types**:
- `feat` - New feature (new rule)
- `fix` - Bug fix
- `docs` - Documentation changes
- `test` - Test additions or modifications
- `refactor` - Code refactoring
- `perf` - Performance improvements
- `ci` - CI/CD changes
- `chore` - Maintenance tasks

**Examples**:
```
feat(moduledsl): add NoLazyKoinInject rule

Detects lazy { inject<T>() } pattern which is an anti-pattern.
Suggests using by inject<T>() or constructor injection instead.

Fixes #42
```

```
fix(scope): handle nested scopes in MissingScopeClose

Previously failed to detect missing close() in nested scope blocks.

Fixes #55
```

### Before Submitting

1. **Sync with upstream**:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Run full verification**:
   ```bash
   ./gradlew clean check
   ```

3. **Ensure all tests pass** and **coverage meets requirements** (98% line, 70% branch)

4. **Check for code quality issues**:
   ```bash
   ./gradlew detekt
   ```

5. **Update documentation** if needed (README.md, docs/rules.md)

### Submitting the PR

1. **Push your branch**:
   ```bash
   git push origin feature/your-branch-name
   ```

2. **Create Pull Request** on GitHub with:
   - Clear title following commit message format
   - Detailed description of changes
   - Reference to related issues
   - Screenshots (if applicable)
   - Checklist of completed items

3. **PR Description Template**:
   ```markdown
   ## Description
   Brief description of what this PR does.

   ## Changes
   - Added new rule `YourNewRule`
   - Updated documentation
   - Added comprehensive tests

   ## Motivation
   Why is this change needed? What problem does it solve?

   ## Related Issues
   Fixes #123

   ## Checklist
   - [ ] All tests pass
   - [ ] Code coverage meets requirements (98% line, 70% branch)
   - [ ] Documentation updated
   - [ ] Rule registered in `KoinRuleSetProvider`
   - [ ] Follows code style and quality standards
   ```

### CI Requirements

All pull requests must pass:

1. **Build validation** - Project builds successfully
2. **All tests pass** - No test failures
3. **Code coverage** - Meets 98% line and 70% branch coverage
4. **Gradle wrapper validation** - Security check
5. **Code quality checks** - Detekt analysis (if configured)

The CI pipeline runs automatically on every PR and commit.

### Review Process

1. **Automated checks** must pass
2. **Maintainer review** - At least one approval required
3. **Address feedback** - Make requested changes
4. **Squash and merge** - PRs are squashed into a single commit

## Code Quality Standards

### Kotlin Style

- Use **explicit API mode** - All public APIs must have explicit visibility and return types
- Follow **Kotlin coding conventions**
- Use **meaningful names** for classes, functions, and variables
- Write **KDoc comments** for public APIs

### Code Compilation

The project enforces strict compilation settings:

- **All warnings as errors** - No compilation warnings allowed
- **Progressive mode** - Uses latest Kotlin compiler features
- **Explicit API mode** - Public APIs must be explicit
- **JVM target 21** - Targets Java 21

These settings ensure high code quality and maintainability.

### Rule Implementation Best Practices

1. **Keep rules focused** - One rule should detect one specific issue
2. **Provide clear messages** - Error messages should explain what's wrong and how to fix it
3. **Make rules configurable** - Allow customization via config when appropriate
4. **Consider performance** - Rules should be efficient (avoid excessive PSI traversal)
5. **Handle edge cases** - Test with complex Kotlin code patterns
6. **Use appropriate severity**:
   - `Warning` - For anti-patterns and bugs
   - `Style` - For stylistic preferences
   - `Minor` - For minor improvements

### Testing Standards

1. **Comprehensive coverage** - Test all code paths
2. **Test naming** - Use descriptive test names in backticks
3. **Use AssertJ/Kotest matchers** - For readable assertions
4. **Isolate tests** - Each test should be independent
5. **Test data** - Use realistic Koin code examples
6. **Edge cases** - Test boundary conditions and unusual inputs

### Documentation Standards

1. **KDoc for public APIs** - All public classes and functions
2. **Inline comments** - For complex logic
3. **Rule documentation** - Complete entry in docs/rules.md
4. **README updates** - If adding significant features
5. **Code examples** - Show both compliant and non-compliant code

## Questions and Help

### Getting Help

- **GitHub Discussions** - For general questions and discussions
- **GitHub Issues** - For bug reports and feature requests
- **Code Review** - Feel free to request feedback on draft PRs

### Reporting Issues

When reporting a bug, please include:

1. **Description** - Clear description of the issue
2. **Steps to reproduce** - Minimal code example
3. **Expected behavior** - What should happen
4. **Actual behavior** - What actually happens
5. **Environment** - Kotlin version, Detekt version, OS
6. **Stack trace** - If applicable

### Feature Requests

When requesting a new feature/rule:

1. **Use case** - Describe the problem or anti-pattern
2. **Proposed solution** - How the rule should work
3. **Examples** - Code examples of compliant/non-compliant code
4. **Impact** - Why this rule would be valuable

### Community Guidelines

- Be respectful and constructive
- Follow the [Code of Conduct](https://www.contributor-covenant.org/)
- Help others when you can
- Ask questions when unclear
- Provide feedback on pull requests

---

Thank you for contributing to detekt-rules-koin! Your efforts help improve code quality for the entire Koin community.
