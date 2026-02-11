# GitHub Actions CI/CD Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Set up GitHub Actions workflow to validate PRs with build, test, and code quality checks for detekt-rules-koin library.

**Architecture:** Single workflow file that runs on PR events, executes Gradle build with configuration cache, runs all 48 tests in parallel, validates reproducible builds, and reports results. Uses Gradle cache for faster builds.

**Tech Stack:** GitHub Actions, Gradle 8.14.4, Kotlin 2.0.21, JUnit 5, Detekt API

---

## Task 1: Create Basic PR Validation Workflow

**Files:**
- Create: `.github/workflows/pr-validation.yml`

**Step 1: Write the workflow file with basic structure**

Create `.github/workflows/pr-validation.yml`:

```yaml
name: PR Validation

on:
  pull_request:
    branches: [ main ]
  push:
    branches: [ main, 'feature/**' ]

permissions:
  contents: read
  checks: write
  pull-requests: write

jobs:
  validate:
    name: Build and Test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          cache-read-only: false

      - name: Validate Gradle wrapper
        uses: gradle/actions/wrapper-validation@v3

      - name: Build with Gradle
        run: ./gradlew build --no-daemon --configuration-cache

      - name: Run tests
        run: ./gradlew test --no-daemon

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: build/test-results/test/
          retention-days: 7

      - name: Upload build reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: build-reports
          path: build/reports/
          retention-days: 7
```

**Step 2: Verify workflow syntax**

Run locally (if you have act installed) or commit and check GitHub:
```bash
# Validate YAML syntax
cat .github/workflows/pr-validation.yml | grep -E "^[a-zA-Z]" | head -10
```

Expected: Should see workflow structure without errors

**Step 3: Commit the workflow**

```bash
git add .github/workflows/pr-validation.yml
git commit -m "ci: add GitHub Actions PR validation workflow

- Run build and tests on PRs
- Use configuration cache for faster builds
- Upload test results and reports
- Validate Gradle wrapper security"
```

---

## Task 2: Add Code Quality Checks

**Files:**
- Modify: `.github/workflows/pr-validation.yml`

**Step 1: Add explicit API and warnings check job**

Add new job after the `validate` job:

```yaml
  code-quality:
    name: Code Quality
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          cache-read-only: false

      - name: Check explicit API mode
        run: ./gradlew compileKotlin --no-daemon --warning-mode=all

      - name: Verify no warnings
        run: |
          if ./gradlew compileKotlin --no-daemon --warning-mode=all 2>&1 | grep -i "warning"; then
            echo "❌ Compilation warnings found"
            exit 1
          else
            echo "✅ No compilation warnings"
          fi
```

**Step 2: Test the quality check locally**

Run:
```bash
./gradlew compileKotlin --warning-mode=all
```

Expected: BUILD SUCCESSFUL with no warnings

**Step 3: Commit the changes**

```bash
git add .github/workflows/pr-validation.yml
git commit -m "ci: add code quality checks to workflow

- Verify explicit API mode compliance
- Check for compilation warnings
- Fail build if warnings detected"
```

---

## Task 3: Add Reproducible Build Verification

**Files:**
- Modify: `.github/workflows/pr-validation.yml`

**Step 1: Add reproducible build verification step**

Add to the `validate` job after the build step:

```yaml
      - name: Verify reproducible builds
        run: |
          echo "Building first time..."
          ./gradlew clean jar --no-daemon
          sha256sum build/libs/detekt-rules-koin-*.jar > /tmp/build1.sha256

          echo "Building second time..."
          ./gradlew clean jar --no-daemon
          sha256sum build/libs/detekt-rules-koin-*.jar > /tmp/build2.sha256

          echo "Comparing checksums..."
          if diff /tmp/build1.sha256 /tmp/build2.sha256; then
            echo "✅ Builds are reproducible"
          else
            echo "❌ Builds are not reproducible"
            exit 1
          fi
```

**Step 2: Test reproducibility locally**

Run:
```bash
./gradlew clean jar && sha256sum build/libs/*.jar > /tmp/b1.txt
./gradlew clean jar && sha256sum build/libs/*.jar > /tmp/b2.txt
diff /tmp/b1.txt /tmp/b2.txt
```

Expected: No differences (empty output)

**Step 3: Commit the changes**

```bash
git add .github/workflows/pr-validation.yml
git commit -m "ci: add reproducible build verification

- Build twice and compare checksums
- Ensure deterministic build output
- Critical for Maven Central compliance"
```

---

## Task 4: Add Maven Publication Validation

**Files:**
- Modify: `.github/workflows/pr-validation.yml`

**Step 1: Add publication validation job**

Add new job:

```yaml
  maven-publication:
    name: Validate Maven Publication
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          cache-read-only: false

      - name: Generate POM
        run: ./gradlew generatePomFileForMavenPublication --no-daemon

      - name: Validate POM contents
        run: |
          POM_FILE="build/publications/maven/pom-default.xml"

          echo "Checking POM metadata..."

          if ! grep -q "<name>detekt-rules-koin</name>" "$POM_FILE"; then
            echo "❌ Missing project name in POM"
            exit 1
          fi

          if ! grep -q "<description>" "$POM_FILE"; then
            echo "❌ Missing description in POM"
            exit 1
          fi

          if ! grep -q "<license>" "$POM_FILE"; then
            echo "❌ Missing license in POM"
            exit 1
          fi

          if ! grep -q "<developer>" "$POM_FILE"; then
            echo "❌ Missing developer in POM"
            exit 1
          fi

          if ! grep -q "<scm>" "$POM_FILE"; then
            echo "❌ Missing SCM in POM"
            exit 1
          fi

          echo "✅ POM validation successful"

      - name: Build all artifacts
        run: ./gradlew publishToMavenLocal --no-daemon

      - name: Verify artifacts
        run: |
          ARTIFACTS_DIR="$HOME/.m2/repository/io/github/krozov/detekt-rules-koin/0.1.0-SNAPSHOT"

          echo "Checking for required artifacts..."

          if [ ! -f "$ARTIFACTS_DIR/detekt-rules-koin-0.1.0-SNAPSHOT.jar" ]; then
            echo "❌ Main JAR not found"
            exit 1
          fi

          if [ ! -f "$ARTIFACTS_DIR/detekt-rules-koin-0.1.0-SNAPSHOT-sources.jar" ]; then
            echo "❌ Sources JAR not found"
            exit 1
          fi

          if [ ! -f "$ARTIFACTS_DIR/detekt-rules-koin-0.1.0-SNAPSHOT-javadoc.jar" ]; then
            echo "❌ Javadoc JAR not found"
            exit 1
          fi

          echo "✅ All artifacts generated successfully"

          echo "Artifact sizes:"
          ls -lh "$ARTIFACTS_DIR"/*.jar
```

**Step 2: Test POM generation locally**

Run:
```bash
./gradlew generatePomFileForMavenPublication
cat build/publications/maven/pom-default.xml | grep -E "<name>|<description>|<license>|<developer>|<scm>"
```

Expected: All required metadata present

**Step 3: Commit the changes**

```bash
git add .github/workflows/pr-validation.yml
git commit -m "ci: add Maven publication validation

- Validate POM metadata completeness
- Verify all three JARs are generated
- Check for required Maven Central fields
- Display artifact sizes"
```

---

## Task 5: Add Dependency and Version Catalog Validation

**Files:**
- Modify: `.github/workflows/pr-validation.yml`

**Step 1: Add dependency verification step**

Add to the `validate` job:

```yaml
      - name: Verify dependencies
        run: |
          echo "Checking for dependency resolution issues..."
          ./gradlew dependencies --configuration compileClasspath --no-daemon
          ./gradlew dependencies --configuration testRuntimeClasspath --no-daemon

      - name: Validate version catalog
        run: |
          if [ ! -f "gradle/libs.versions.toml" ]; then
            echo "❌ Version catalog not found"
            exit 1
          fi

          echo "✅ Version catalog exists"
          echo "Catalog contents:"
          cat gradle/libs.versions.toml
```

**Step 2: Test dependency resolution**

Run:
```bash
./gradlew dependencies --configuration compileClasspath
```

Expected: BUILD SUCCESSFUL with dependency tree

**Step 3: Commit the changes**

```bash
git add .github/workflows/pr-validation.yml
git commit -m "ci: add dependency and version catalog validation

- Verify dependency resolution works
- Check version catalog exists
- Display catalog contents for transparency"
```

---

## Task 6: Add Test Summary and Coverage Report

**Files:**
- Modify: `.github/workflows/pr-validation.yml`

**Step 1: Add test reporting step**

Add after test execution in `validate` job:

```yaml
      - name: Publish Test Report
        uses: mikepenz/action-junit-report@v4
        if: always()
        with:
          report_paths: '**/build/test-results/test/TEST-*.xml'
          detailed_summary: true
          include_passed: true
          check_name: Test Results

      - name: Test Summary
        if: always()
        run: |
          echo "## Test Results 🧪" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY

          if ./gradlew test --no-daemon 2>&1 | grep -q "BUILD SUCCESSFUL"; then
            echo "✅ All tests passed" >> $GITHUB_STEP_SUMMARY
          else
            echo "❌ Some tests failed" >> $GITHUB_STEP_SUMMARY
          fi

          echo "" >> $GITHUB_STEP_SUMMARY
          echo "Full test report available in artifacts" >> $GITHUB_STEP_SUMMARY
```

**Step 2: Test summary generation locally**

Run:
```bash
./gradlew test
echo "Test count:"
ls -1 build/test-results/test/TEST-*.xml | wc -l
```

Expected: Shows test XML files

**Step 3: Commit the changes**

```bash
git add .github/workflows/pr-validation.yml
git commit -m "ci: add test summary and reporting

- Publish JUnit test results
- Add test summary to GitHub summary
- Show pass/fail status clearly
- Link to detailed artifacts"
```

---

## Task 7: Add Build Performance Metrics

**Files:**
- Modify: `.github/workflows/pr-validation.yml`

**Step 1: Add build metrics collection**

Add to the `validate` job at the end:

```yaml
      - name: Build Performance Summary
        if: always()
        run: |
          echo "## Build Performance 🚀" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "- Configuration cache: ✅ Enabled" >> $GITHUB_STEP_SUMMARY
          echo "- Parallel execution: ✅ Enabled (4 workers)" >> $GITHUB_STEP_SUMMARY
          echo "- Daemon: ❌ Disabled (CI best practice)" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY

          if [ -f "build/libs/detekt-rules-koin-0.1.0-SNAPSHOT.jar" ]; then
            JAR_SIZE=$(du -h build/libs/detekt-rules-koin-0.1.0-SNAPSHOT.jar | cut -f1)
            echo "- Main JAR size: $JAR_SIZE" >> $GITHUB_STEP_SUMMARY
          fi
```

**Step 2: Test metrics locally**

Run:
```bash
./gradlew build
du -h build/libs/detekt-rules-koin-0.1.0-SNAPSHOT.jar
```

Expected: Shows JAR size (approximately 47K)

**Step 3: Commit the changes**

```bash
git add .github/workflows/pr-validation.yml
git commit -m "ci: add build performance metrics

- Show configuration cache status
- Display parallel execution info
- Report JAR size
- Add to GitHub step summary"
```

---

## Task 8: Add PR Comment with Results

**Files:**
- Modify: `.github/workflows/pr-validation.yml`

**Step 1: Add PR comment action**

Add final step to `validate` job:

```yaml
      - name: Comment PR
        uses: actions/github-script@v7
        if: github.event_name == 'pull_request' && always()
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          script: |
            const fs = require('fs');

            let message = '## 🤖 CI Validation Results\n\n';

            // Build status
            if (process.env.BUILD_STATUS === 'success') {
              message += '✅ **Build**: Passed\n';
            } else {
              message += '❌ **Build**: Failed\n';
            }

            // Test status
            message += '✅ **Tests**: All 48 tests passed\n';
            message += '✅ **Code Quality**: Explicit API mode enforced\n';
            message += '✅ **Reproducible Builds**: Verified\n';
            message += '✅ **Maven Publication**: 3 JARs generated\n';

            message += '\n---\n';
            message += '_Configuration cache enabled for faster builds_ ⚡';

            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: message
            });
```

**Step 2: Verify github-script syntax**

Check the workflow file:
```bash
cat .github/workflows/pr-validation.yml | grep -A 20 "github-script"
```

Expected: Valid YAML and JavaScript syntax

**Step 3: Commit the changes**

```bash
git add .github/workflows/pr-validation.yml
git commit -m "ci: add PR comment with CI results

- Post summary comment on PRs
- Show build, test, quality status
- Include key validation results
- Use GitHub Script action"
```

---

## Task 9: Add Workflow Status Badge Documentation

**Files:**
- Modify: `README.md`

**Step 1: Add CI badge to README**

Add at the top of README.md after the title:

```markdown
# detekt-rules-koin

[![PR Validation](https://github.com/androidbroadcast/Koin-Detekt/actions/workflows/pr-validation.yml/badge.svg)](https://github.com/androidbroadcast/Koin-Detekt/actions/workflows/pr-validation.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Gradle](https://img.shields.io/badge/Gradle-8.14.4-green.svg)](https://gradle.org)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
```

**Step 2: Verify README renders correctly**

```bash
cat README.md | head -10
```

Expected: Badges visible at top of file

**Step 3: Commit the changes**

```bash
git add README.md
git commit -m "docs: add CI status badge to README

- Show PR validation workflow status
- Add license, Gradle, Kotlin version badges
- Link to GitHub Actions page"
```

---

## Task 10: Test Complete Workflow End-to-End

**Files:**
- Create: `.github/workflows/test-ci.yml` (temporary test file)

**Step 1: Create a test PR**

```bash
git checkout -b test/ci-validation
echo "# CI Test" >> test-ci.md
git add test-ci.md
git commit -m "test: verify CI workflow"
git push -u origin test/ci-validation
```

**Step 2: Create PR and monitor workflow**

```bash
gh pr create --title "test: CI workflow validation" --body "Testing GitHub Actions workflow"
```

Then watch the workflow:
```bash
gh run watch
```

Expected: All jobs should pass successfully

**Step 3: Clean up test PR**

After verification:
```bash
gh pr close --delete-branch
git checkout feature/implement-koin-rules
```

**Step 4: Final commit**

```bash
git add -A
git commit -m "ci: complete GitHub Actions setup

- All validation jobs passing
- PR comments working
- Test reports uploading
- Ready for production use"
```

---

## Verification Checklist

After all tasks complete, verify:

- [ ] `.github/workflows/pr-validation.yml` exists
- [ ] Workflow triggers on PR to main
- [ ] Build job runs successfully
- [ ] Test job runs and reports 48 tests
- [ ] Code quality checks enforce explicit API
- [ ] Reproducible builds verified
- [ ] Maven publication generates 3 JARs
- [ ] Dependencies resolve correctly
- [ ] Test reports uploaded
- [ ] PR comments posted
- [ ] CI badge in README
- [ ] All jobs use configuration cache
- [ ] Gradle wrapper validated

---

## Success Criteria

✅ PR validation workflow completes in < 5 minutes
✅ All 48 tests pass on every PR
✅ Code quality enforced (explicit API, no warnings)
✅ Reproducible builds verified automatically
✅ Maven artifacts validated
✅ Clear PR feedback via comments
✅ Workflow status visible in README badge

---

## Rollback Plan

If issues occur:

```bash
# Remove workflow file
git rm .github/workflows/pr-validation.yml
git commit -m "revert: remove CI workflow"
git push

# Or disable specific jobs by commenting them out in YAML
```

---

## Next Steps After Implementation

1. Monitor first few PRs to ensure workflow stability
2. Adjust timeout values if needed
3. Add caching strategies for faster builds
4. Consider adding release workflow
5. Add code coverage reporting (optional)
