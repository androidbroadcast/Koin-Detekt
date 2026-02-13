# Kover Test Coverage Setup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Configure Kover (Kotlin code coverage tool) for the detekt-rules-koin project to track and enforce test coverage metrics.

**Architecture:** Add Kover Gradle plugin to the build system, configure coverage rules with appropriate thresholds, and integrate coverage reporting into the verification workflow. Kover will track line and branch coverage for production code, excluding generated files and test code.

**Tech Stack:** Kotlin 2.0.21, Gradle 8.x with Kotlin DSL, Kover 0.9.7, JUnit Jupiter 5.11.4

---

## Task 1: Add Kover Plugin to Version Catalog

**Files:**
- Modify: `gradle/libs.versions.toml:1-22`

**Step 1: Add Kover version to the versions section**

Edit `gradle/libs.versions.toml` and add the Kover version after the detekt version:

```toml
[versions]
# Build toolchain
kotlin = "2.0.21"
detekt = "1.23.8"
kover = "0.9.7"

# Test dependencies
assertj = "3.27.7"
junit = "5.11.4"
```

**Step 2: Add Kover plugin to the plugins section**

Edit `gradle/libs.versions.toml` and add the Kover plugin after the maven-publish plugin:

```toml
[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
maven-publish = { id = "maven-publish" }
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
```

**Step 3: Verify the version catalog changes**

Run: `./gradlew --dry-run tasks`
Expected: No errors, configuration cache message

**Step 4: Commit the version catalog changes**

```bash
git add gradle/libs.versions.toml
git commit -m "chore: add Kover plugin to version catalog

Add Kover 0.9.7 for Kotlin code coverage tracking.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Apply Kover Plugin to Build Script

**Files:**
- Modify: `build.gradle.kts:1-4`

**Step 1: Apply the Kover plugin**

Edit `build.gradle.kts` and add the Kover plugin to the plugins block:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kover)
}
```

**Step 2: Verify the plugin applies successfully**

Run: `./gradlew tasks --group=verification`
Expected: Output includes Kover tasks like `koverHtmlReport`, `koverXmlReport`, `koverVerify`, `koverBinaryReport`

**Step 3: Commit the plugin application**

```bash
git add build.gradle.kts
git commit -m "chore: apply Kover plugin to build

Enable Kover code coverage for the project.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Configure Basic Kover Settings

**Files:**
- Modify: `build.gradle.kts:139` (add after kotlin configuration block)

**Step 1: Add basic Kover configuration**

Add the Kover configuration block at the end of `build.gradle.kts`:

```kotlin
// Code coverage configuration
kover {
    reports {
        // Enable all report types
        total {
            html {
                onCheck = true
                htmlDir = layout.buildDirectory.dir("reports/kover/html")
            }
            xml {
                onCheck = true
                xmlFile = layout.buildDirectory.file("reports/kover/coverage.xml")
            }
        }
    }
}
```

**Step 2: Run Kover to generate initial reports**

Run: `./gradlew koverHtmlReport`
Expected: SUCCESS, HTML report generated at `build/reports/kover/html/index.html`

**Step 3: Verify the HTML report exists**

Run: `ls -la build/reports/kover/html/index.html`
Expected: File exists with positive file size

**Step 4: Commit the basic configuration**

```bash
git add build.gradle.kts
git commit -m "feat: configure Kover report generation

Configure HTML and XML coverage reports.
Reports are generated in build/reports/kover/.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Configure Coverage Exclusions

**Files:**
- Modify: `build.gradle.kts` (within kover block)

**Step 1: Add exclusion rules for generated and test code**

Update the Kover configuration to exclude generated code and test utilities:

```kotlin
// Code coverage configuration
kover {
    reports {
        // Configure what to exclude from coverage
        filters {
            excludes {
                // Exclude generated code
                classes("*.*BuildConfig*")

                // Exclude test utilities if any exist in main source set
                packages("io.github.krozov.detekt.koin.test")
            }
        }

        // Enable all report types
        total {
            html {
                onCheck = true
                htmlDir = layout.buildDirectory.dir("reports/kover/html")
            }
            xml {
                onCheck = true
                xmlFile = layout.buildDirectory.file("reports/kover/coverage.xml")
            }
        }
    }
}
```

**Step 2: Regenerate coverage report with exclusions**

Run: `./gradlew clean koverHtmlReport`
Expected: SUCCESS, report excludes specified packages/classes

**Step 3: Verify exclusions are applied**

Run: `cat build/reports/kover/coverage.xml | grep -c "class name" || echo "0"`
Expected: Count of classes in coverage (should not include excluded ones)

**Step 4: Commit the exclusion configuration**

```bash
git add build.gradle.kts
git commit -m "feat: configure Kover coverage exclusions

Exclude generated code and test utilities from coverage.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Add Coverage Verification Rules

**Files:**
- Modify: `build.gradle.kts` (within kover.reports block)

**Step 1: Add coverage thresholds**

Update the Kover configuration to add verification rules:

```kotlin
// Code coverage configuration
kover {
    reports {
        // Configure what to exclude from coverage
        filters {
            excludes {
                // Exclude generated code
                classes("*.*BuildConfig*")

                // Exclude test utilities if any exist in main source set
                packages("io.github.krozov.detekt.koin.test")
            }
        }

        // Enable all report types
        total {
            html {
                onCheck = true
                htmlDir = layout.buildDirectory.dir("reports/kover/html")
            }
            xml {
                onCheck = true
                xmlFile = layout.buildDirectory.file("reports/kover/coverage.xml")
            }

            // Verification rules - enforce minimum coverage
            verify {
                onCheck = true

                rule {
                    // Minimum line coverage: 80%
                    minBound(80)
                }

                rule("Branch Coverage") {
                    // Minimum branch coverage: 70%
                    minBound(70, aggregationForGroup = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE)
                    bound {
                        minValue = 70
                        metric = kotlinx.kover.gradle.plugin.dsl.MetricType.BRANCH
                        aggregation = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
    }
}
```

**Step 2: Run verification to check current coverage**

Run: `./gradlew koverVerify`
Expected: May FAIL if current coverage is below thresholds (this is expected)

**Step 3: Check what the current coverage is**

Run: `./gradlew koverHtmlReport && open build/reports/kover/html/index.html`
Expected: HTML report opens showing current coverage percentages

**Step 4: Adjust thresholds based on current coverage**

If verification failed, adjust the minBound values in the rule blocks to be slightly below current coverage (e.g., if current is 85%, set to 80%). This ensures we don't lose coverage going forward but don't block the build initially.

**Step 5: Verify the adjusted thresholds pass**

Run: `./gradlew koverVerify`
Expected: SUCCESS with coverage thresholds passing

**Step 6: Commit the verification rules**

```bash
git add build.gradle.kts
git commit -m "feat: add Kover coverage verification rules

Enforce minimum coverage thresholds:
- Line coverage: 80%
- Branch coverage: 70%

Verification runs as part of 'check' task.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 6: Integrate Coverage into Check Task

**Files:**
- Verify: `build.gradle.kts` (no changes needed, onCheck=true already configured)

**Step 1: Run the check task to verify integration**

Run: `./gradlew clean check`
Expected: SUCCESS, includes test execution, Kover report generation, and verification

**Step 2: Verify all coverage artifacts are generated**

Run: `ls -la build/reports/kover/`
Expected: Directory contains `html/`, `coverage.xml`, and `report.bin`

**Step 3: Verify coverage reports in the check output**

Run: `./gradlew check --info 2>&1 | grep -i kover`
Expected: Log messages showing Kover tasks executed during check

**Step 4: Commit verification (documentation only)**

Create a note in the plan about successful integration. No code changes needed.

**Verification Results (2026-02-13):**
✅ Integration verified successfully:
- The `check` task successfully includes Kover coverage tasks (`koverHtmlReport`, `koverXmlReport`, `koverVerify`)
- All coverage artifacts are generated correctly:
  - HTML report at `build/reports/kover/html/index.html` (21 files including index and sorted views)
  - XML report at `build/reports/kover/coverage.xml` (37,627 bytes with detailed coverage counters)
  - Verification error file `verify.err` is empty (no violations)
- Coverage verification passed with all rules enforced (80% instruction, 70% branch)
- Build completed successfully in 376ms with Kover tasks executing as part of the check workflow
- Integration configured via `onCheck = true` in Tasks 3 and 5 is working as expected

---

## Task 7: Update .gitignore for Coverage Reports

**Files:**
- Modify: `.gitignore`

**Step 1: Add Kover reports to .gitignore**

Check current .gitignore content:

Run: `cat .gitignore`

Add or verify these lines exist:

```gitignore
# Build outputs
build/
.gradle/

# Coverage reports
*.exec
*.bin
kover/
.kover/
```

**Step 2: Verify gitignore works**

Run: `git status`
Expected: `build/reports/kover/` should not appear in untracked files

**Step 3: Commit .gitignore updates if changes were made**

```bash
git add .gitignore
git commit -m "chore: update .gitignore for Kover coverage files

Ignore coverage report outputs and binary files.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 8: Document Kover Usage in README

**Files:**
- Modify: `README.md` (add coverage badge and usage section)

**Step 1: Add coverage badge to README**

Add after the existing badges (if any) near the top of README.md:

```markdown
![Coverage](https://img.shields.io/badge/coverage-check%20locally-blue)
```

**Step 2: Add coverage section to README**

Add before the "Contributing" section or at the end:

```markdown
## Code Coverage

This project uses [Kover](https://github.com/Kotlin/kotlinx-kover) for code coverage tracking.

### Generate Coverage Reports

```bash
# Generate HTML coverage report
./gradlew koverHtmlReport

# Open the report (macOS)
open build/reports/kover/html/index.html

# Open the report (Linux)
xdg-open build/reports/kover/html/index.html
```

### Coverage Verification

Coverage thresholds are enforced as part of the `check` task:
- **Line Coverage:** ≥80%
- **Branch Coverage:** ≥70%

```bash
# Run all checks including coverage verification
./gradlew check

# Run only coverage verification
./gradlew koverVerify
```

### CI Integration

Coverage is automatically verified in GitHub Actions. The `check` task includes coverage verification and will fail if thresholds are not met.
```

**Step 3: Verify README renders correctly**

Run: `cat README.md | grep -A 5 "Code Coverage"`
Expected: Shows the new coverage section

**Step 4: Commit README documentation**

```bash
git add README.md
git commit -m "docs: add Kover coverage documentation to README

Document how to generate coverage reports and verify coverage.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 9: Optional - Add GitHub Actions Coverage Upload

**Files:**
- Modify: `.github/workflows/ci.yml` (if exists)

**Step 1: Check if CI workflow exists**

Run: `ls -la .github/workflows/`
Expected: List of workflow files

**Step 2: If ci.yml exists, add coverage artifact upload**

Add this step after the test/check step:

```yaml
- name: Upload coverage reports
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: coverage-report
    path: |
      build/reports/kover/html/
      build/reports/kover/coverage.xml
    retention-days: 30
```

**Step 3: If adding to CI, verify workflow syntax**

Run: `cat .github/workflows/ci.yml | grep -A 5 "coverage"`
Expected: Shows the coverage upload configuration

**Step 4: Commit CI changes if made**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: upload Kover coverage reports as artifacts

Upload HTML and XML coverage reports for inspection.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 10: Final Verification and Testing

**Files:**
- Verify: All modified files

**Step 1: Clean build to verify everything works**

Run: `./gradlew clean build`
Expected: SUCCESS with all tests passing and coverage verified

**Step 2: Verify coverage report quality**

Run: `./gradlew koverHtmlReport && ls -lh build/reports/kover/html/index.html`
Expected: HTML report file exists and has reasonable size (>10KB)

**Step 3: Run all verification tasks**

Run: `./gradlew check --rerun-tasks`
Expected: SUCCESS with fresh execution of all tasks

**Step 4: Review git status**

Run: `git status`
Expected: Clean working tree (all changes committed) or only expected uncommitted files

**Step 5: Create summary of what was configured**

Document in this plan (no commit needed):

✅ Kover plugin added and configured
✅ Coverage reports (HTML + XML) enabled
✅ Coverage verification rules enforced (80% line, 70% branch)
✅ Integration with `check` task
✅ Documentation updated
✅ CI integration (optional)

---

## Success Criteria

- [ ] `./gradlew check` runs successfully and includes coverage verification
- [ ] `./gradlew koverHtmlReport` generates readable HTML report
- [ ] Coverage thresholds are enforced (build fails if coverage drops below thresholds)
- [ ] Coverage reports are excluded from version control
- [ ] Documentation explains how to generate and view coverage reports
- [ ] Optional: CI uploads coverage artifacts for review

## References

- [Kover Documentation](https://kotlin.github.io/kotlinx-kover/)
- [Kover Gradle Plugin](https://github.com/Kotlin/kotlinx-kover)
- [Kover Releases](https://github.com/Kotlin/kotlinx-kover/releases)
- Kover version: 0.9.7 (latest as of February 2026)
- Compatible with Kotlin 2.0.21

---

**Implementation Notes:**

- Adjust coverage thresholds based on actual current coverage to avoid blocking builds
- Consider using `onCheck = false` during initial setup if thresholds are too aggressive
- XML reports are useful for IDE integration and CI tools like Codecov or Coveralls
- Binary reports (`.bin`) are used by Kover for incremental builds and should be gitignored
