# Release Workflow Testing Guide

This document provides a comprehensive guide for testing the automated release workflow for the detekt-rules-koin library.

## Table of Contents

- [Overview](#overview)
- [Test Release Procedure](#test-release-procedure)
- [Workflow Execution Monitoring](#workflow-execution-monitoring)
- [Verification Steps](#verification-steps)
- [Version Format Tests](#version-format-tests)
- [Failure Scenario Testing](#failure-scenario-testing)
- [Success Criteria](#success-criteria)

## Overview

The release workflow (`.github/workflows/release.yml`) automates the following tasks:

1. **Build and Validate**: Version format validation, Gradle build with version override, test execution, JAR generation
2. **Publish**: Checksum generation (SHA-256 and SHA-512), Maven publication to GitHub Packages
3. **Release**: Draft GitHub Release creation, CHANGELOG.md update and commit to main

## Test Release Procedure

### Prerequisites

- Ensure you have push access to the repository
- Verify all CI workflows pass on the main branch
- Ensure your local repository is up to date

```bash
git checkout main
git pull origin main
```

### Creating a Test Release

Use a pre-release version tag for testing (e.g., `v0.1.0-test.1`):

```bash
# Create and push a test release tag
git tag v0.1.0-test.1
git push origin v0.1.0-test.1

# If you need to retry, delete and recreate the tag
git tag -d v0.1.0-test.1
git push origin :refs/tags/v0.1.0-test.1
```

**Note**: Pre-release versions (containing hyphens) will trigger the workflow but are marked as pre-releases and can be easily cleaned up.

## Workflow Execution Monitoring

### 1. Monitor Workflow Start

Navigate to:
```
https://github.com/androidbroadcast/Koin-Detekt/actions/workflows/release.yml
```

Verify:
- [ ] Workflow triggered by the tag push
- [ ] All three jobs appear: `build`, `publish`, `release`
- [ ] Jobs execute in the correct order (build → publish → release)

### 2. Monitor Build Job

Check the following steps:

- [ ] **Checkout code**: Repository cloned successfully
- [ ] **Extract version from tag**: Version extracted correctly (e.g., `0.1.0-test.1`)
- [ ] **Validate version format**: Tag format validated against semver pattern
- [ ] **Check if pre-release**: Pre-release flag set correctly (`true` for test versions)
- [ ] **Set up JDK 21**: Java Development Kit installed
- [ ] **Setup Gradle**: Gradle wrapper configured with caching
- [ ] **Build with Gradle**: Compilation successful with version override `-Pversion=X.Y.Z`
- [ ] **Generate all JARs**: Main JAR, sources JAR, javadoc JAR generated
- [ ] **Upload build artifacts**: 3 JAR files uploaded as artifacts

Expected output for version extraction:
```
Extracted version: 0.1.0-test.1
✅ Version format valid: 0.1.0-test.1
✅ Detected pre-release version
```

Expected build output:
```
BUILD SUCCESSFUL in 45s
48 actionable tasks: 48 executed
```

### 3. Monitor Publish Job

Check the following steps:

- [ ] **Checkout code**: Fresh repository checkout
- [ ] **Download build artifacts**: 3 JAR files downloaded from build job
- [ ] **Generate checksums**: SHA-256 and SHA-512 checksums created for each JAR
- [ ] **Publish to GitHub Packages**: Maven publication successful with version override
- [ ] **Upload checksums as artifacts**: 6 checksum files uploaded

Expected checksum generation output:
```
Generating SHA-256 and SHA-512 checksums...
✅ Generated checksums for detekt-koin-0.1.0-test.1.jar
✅ Generated checksums for detekt-koin-0.1.0-test.1-sources.jar
✅ Generated checksums for detekt-koin-0.1.0-test.1-javadoc.jar
```

Expected publish output:
```
Publishing to GitHubPackages
Published io.github.krozov:detekt-koin:0.1.0-test.1 to https://maven.pkg.github.com/androidbroadcast/Koin-Detekt
```

### 4. Monitor Release Job

Check the following steps:

- [ ] **Checkout code**: Full history fetched (`fetch-depth: 0`)
- [ ] **Download build artifacts**: 3 JAR files retrieved
- [ ] **Download checksums**: 6 checksum files retrieved
- [ ] **Verify all artifacts present**: Count validation (3 JARs + 6 checksums = 9 files)
- [ ] **Create Draft Release**: Draft release created with artifacts attached
- [ ] **Get release notes**: Auto-generated release notes from commits
- [ ] **Update and commit CHANGELOG to main**: CHANGELOG.md updated and pushed to main

Expected artifact verification output:
```
Found 3 JAR files
Found 6 checksum files
✅ All artifacts present
```

Expected CHANGELOG update output:
```
✅ CHANGELOG committed to main
changelog_updated=true
```

## Verification Steps

### 1. Verify GitHub Release

Navigate to:
```
https://github.com/androidbroadcast/Koin-Detekt/releases
```

Verify the draft release:

- [ ] Release title matches: "Release vX.Y.Z"
- [ ] Release is marked as **draft** (not published)
- [ ] Pre-release flag is set correctly (true for test versions)
- [ ] Release notes contain auto-generated commit list
- [ ] Release body includes "What's Changed" section

**Assets verification:**
- [ ] `detekt-koin-X.Y.Z.jar` attached (main library)
- [ ] `detekt-koin-X.Y.Z-sources.jar` attached (source code)
- [ ] `detekt-koin-X.Y.Z-javadoc.jar` attached (API documentation)
- [ ] `detekt-koin-X.Y.Z.jar.sha256` attached
- [ ] `detekt-koin-X.Y.Z.jar.sha512` attached
- [ ] `detekt-koin-X.Y.Z-sources.jar.sha256` attached
- [ ] `detekt-koin-X.Y.Z-sources.jar.sha512` attached
- [ ] `detekt-koin-X.Y.Z-javadoc.jar.sha256` attached
- [ ] `detekt-koin-X.Y.Z-javadoc.jar.sha512` attached
- [ ] Total: 9 files (3 JARs + 6 checksums)

### 2. Verify GitHub Packages

Navigate to:
```
https://github.com/androidbroadcast/Koin-Detekt/packages
```

Verify:
- [ ] Package version `X.Y.Z` appears in the versions list
- [ ] Package shows correct Maven coordinates: `io.github.krozov:detekt-koin:X.Y.Z`
- [ ] Package is linked to the tag commit
- [ ] Package metadata includes description from `build.gradle.kts`
- [ ] Installation instructions show Maven/Gradle configuration

Expected Maven coordinates:
```
<dependency>
  <groupId>io.github.krozov</groupId>
  <artifactId>detekt-koin</artifactId>
  <version>0.1.0-test.1</version>
</dependency>
```

Expected Gradle configuration:
```kotlin
detektPlugins("io.github.krozov:detekt-koin:0.1.0-test.1")
```

### 3. Verify CHANGELOG Update

Check the repository:

```bash
# Pull the latest changes from main
git pull origin main

# View the updated CHANGELOG
head -40 CHANGELOG.md
```

Verify:
- [ ] New version section exists: `## v0.1.0-test.1 - YYYY-MM-DD`
- [ ] Section inserted AFTER `## [Unreleased]` (not before the title)
- [ ] Release date is in `YYYY-MM-DD` format
- [ ] Section includes auto-generated release notes
- [ ] Original changelog structure preserved
- [ ] Unreleased section still exists at the top
- [ ] Commit authored by `github-actions[bot]`
- [ ] Commit message: `docs: update CHANGELOG for v0.1.0-test.1`

Expected structure:
```markdown
# Changelog

## [Unreleased]

## v0.1.0-test.1 - 2026-02-11

<auto-generated release notes>

## Previous versions...
```

### 4. Verify Checksums

Download and verify the checksums:

```bash
# Create temp directory for testing
mkdir -p /tmp/release-test
cd /tmp/release-test

# Download release assets (replace v0.1.0-test.1 with your version)
gh release download v0.1.0-test.1 --repo androidbroadcast/Koin-Detekt

# Verify SHA-256 checksums
sha256sum -c *.sha256

# Verify SHA-512 checksums
sha512sum -c *.sha512
```

Expected output:
```
detekt-koin-0.1.0-test.1.jar: OK
detekt-koin-0.1.0-test.1-sources.jar: OK
detekt-koin-0.1.0-test.1-javadoc.jar: OK
```

### 5. Verify JAR Contents

Inspect the JAR files:

```bash
# List contents of main JAR
unzip -l detekt-koin-0.1.0-test.1.jar

# Check META-INF/MANIFEST.MF for version
unzip -p detekt-koin-0.1.0-test.1.jar META-INF/MANIFEST.MF

# List contents of sources JAR
unzip -l detekt-koin-0.1.0-test.1-sources.jar

# List contents of javadoc JAR
unzip -l detekt-koin-0.1.0-test.1-javadoc.jar
```

Verify:
- [ ] Main JAR contains compiled `.class` files
- [ ] Main JAR contains detekt rule providers
- [ ] Sources JAR contains `.kt` source files
- [ ] Javadoc JAR contains `.html` documentation files
- [ ] All JARs have proper directory structure
- [ ] File permissions are reproducible (0644 for files, 0755 for dirs)

### 6. Test Package Installation

Create a test Gradle project:

```bash
mkdir -p /tmp/detekt-test
cd /tmp/detekt-test

# Initialize a test Gradle project
gradle init --type kotlin-library --dsl kotlin
```

Add to `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
    id("io.gitlab.arturbosch.detekt") version "1.23.5"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/androidbroadcast/Koin-Detekt")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    detektPlugins("io.github.krozov:detekt-koin:0.1.0-test.1")
}

detekt {
    config.setFrom(files("detekt-config.yml"))
}
```

Test the installation:

```bash
# Set GitHub credentials
export GITHUB_ACTOR=your-username
export GITHUB_TOKEN=your-token

# Run detekt to verify plugin loads
./gradlew detekt

# Expected: Plugin loads successfully, no errors
```

Verify:
- [ ] Gradle resolves the dependency successfully
- [ ] Detekt loads the custom rule provider
- [ ] Rules from detekt-koin are available
- [ ] No class loading errors or conflicts

## Version Format Tests

### Valid Version Formats

Test these version formats to ensure they trigger the workflow correctly:

1. **Standard release**: `v1.0.0`
   ```bash
   git tag v1.0.0 && git push origin v1.0.0
   ```
   Expected: `is_prerelease=false`

2. **Pre-release with alpha**: `v0.1.0-alpha.1`
   ```bash
   git tag v0.1.0-alpha.1 && git push origin v0.1.0-alpha.1
   ```
   Expected: `is_prerelease=true`

3. **Pre-release with beta**: `v0.1.0-beta.2`
   ```bash
   git tag v0.1.0-beta.2 && git push origin v0.1.0-beta.2
   ```
   Expected: `is_prerelease=true`

4. **Release candidate**: `v1.0.0-rc.1`
   ```bash
   git tag v1.0.0-rc.1 && git push origin v1.0.0-rc.1
   ```
   Expected: `is_prerelease=true`

5. **Test version**: `v0.1.0-test.1`
   ```bash
   git tag v0.1.0-test.1 && git push origin v0.1.0-test.1
   ```
   Expected: `is_prerelease=true`

### Invalid Version Formats

Verify these formats are REJECTED by validation:

1. **Missing 'v' prefix**: `1.0.0`
   Expected: Workflow fails at "Validate version format" step

2. **Two-part version**: `v1.0`
   Expected: Workflow fails at "Validate version format" step

3. **Non-numeric parts**: `vX.Y.Z`
   Expected: Workflow fails at "Validate version format" step

4. **Four-part version**: `v1.0.0.1`
   Expected: Workflow fails at "Validate version format" step

## Failure Scenario Testing

### 1. Build Failure

Test build failure handling:

```bash
# Introduce a compilation error in a Kotlin file
# Commit and push tag

# Expected: Build job fails at "Build with Gradle" step
# Expected: Publish and Release jobs are skipped
```

Verify:
- [ ] Workflow shows red X at build job
- [ ] Error message clearly shows compilation failure
- [ ] Subsequent jobs are skipped (not run)
- [ ] No artifacts uploaded
- [ ] No release created

### 2. Test Failure

Test test failure handling:

```bash
# Modify a test to fail
# Commit and push tag

# Expected: Build job fails at "Build with Gradle" step (tests run during build)
# Expected: Publish and Release jobs are skipped
```

Verify:
- [ ] Test failure is clearly reported in logs
- [ ] Failed test name and assertion shown
- [ ] No JAR artifacts created
- [ ] Workflow stops at build job

### 3. Checksum Generation Failure

Simulate checksum failure (requires modifying workflow temporarily):

```bash
# Manually delete one JAR before checksum step
# Expected: Checksum step succeeds but only generates 5 files instead of 6
# Expected: "Verify all artifacts present" step fails
```

Verify:
- [ ] Missing checksum is detected
- [ ] Clear error message: "Expected at least 6 checksum files"
- [ ] Release job fails before creating release

### 4. CHANGELOG Update Failure

Test CHANGELOG commit failure (e.g., due to branch protection):

```bash
# Enable branch protection on main requiring PR reviews
# Push tag
# Expected: Workflow completes but CHANGELOG commit step fails (continue-on-error: true)
```

Verify:
- [ ] Release is still created successfully
- [ ] Warning message in summary: "⚠️ CHANGELOG.md update failed or was skipped"
- [ ] `changelog_updated=false` in step outputs
- [ ] Manual CHANGELOG update required

## Success Criteria

Use this checklist to confirm a successful release:

### Pre-Release

- [ ] All CI workflows pass on main branch
- [ ] All 48 tests pass: `./gradlew test`
- [ ] Code compiles without warnings (allWarningsAsErrors=true)
- [ ] CHANGELOG.md has `## [Unreleased]` section with changes
- [ ] No uncommitted changes in working directory

### During Release

- [ ] Tag pushed successfully with `v` prefix
- [ ] Workflow triggered automatically
- [ ] All three jobs complete successfully (green checkmarks)
- [ ] No error messages in workflow logs
- [ ] Build job takes ~1-2 minutes
- [ ] Publish job takes ~30 seconds
- [ ] Release job takes ~30 seconds

### Post-Release

- [ ] Draft release created on GitHub
- [ ] 9 artifacts attached to release (3 JARs + 6 checksums)
- [ ] All checksums verify correctly
- [ ] Package published to GitHub Packages with correct version
- [ ] Maven coordinates are correct: `io.github.krozov:detekt-koin:X.Y.Z`
- [ ] CHANGELOG.md updated with new version section
- [ ] CHANGELOG commit pushed to main branch
- [ ] New section inserted after `## [Unreleased]`
- [ ] All JAR files downloadable and contain expected files
- [ ] Package installs successfully in test project
- [ ] Detekt loads the plugin without errors

### Cleanup Test Releases

After testing, clean up test releases:

```bash
# Delete the test tag locally
git tag -d v0.1.0-test.1

# Delete the test tag remotely
git push origin :refs/tags/v0.1.0-test.1

# Delete the GitHub release
gh release delete v0.1.0-test.1 --repo androidbroadcast/Koin-Detekt --yes

# Note: Cannot delete GitHub Packages versions (must remain or deprecate)
```

## Troubleshooting

### Workflow Not Triggering

**Check:**
- Tag format matches `v*.*.*` pattern
- Tag was pushed to remote: `git ls-remote --tags origin`
- Workflow file exists on main branch: `.github/workflows/release.yml`

**Solution:**
```bash
# Verify tag was pushed
git ls-remote --tags origin | grep v0.1.0-test.1

# Re-push if missing
git push origin v0.1.0-test.1
```

### Permission Errors

**Verify:**
- Repository settings → Actions → General → Workflow permissions
- "Read and write permissions" is enabled
- `GITHUB_TOKEN` has `contents:write` and `packages:write`

### Version Override Not Working

**Symptoms**: Published package has version `0.1.0-SNAPSHOT` instead of tag version

**Check:**
- Build step includes `-Pversion=X.Y.Z` flag
- Publish step includes `-Pversion=X.Y.Z` flag
- Gradle accepts version property override

**Solution:**
Verify in workflow logs:
```
./gradlew build -Pversion=0.1.0-test.1
```

### Package Publishing Fails

**Check:**
- `GITHUB_TOKEN` environment variable is set
- `GITHUB_ACTOR` environment variable is set
- Repository allows GitHub Packages
- No conflicting package version exists

**Solution:**
Check publish step logs for authentication errors or version conflicts.

### CHANGELOG Structure Broken

**Symptoms**: New release section appears before `# Changelog` title

**Check:**
- CHANGELOG.md has proper structure with `# Changelog` title
- Workflow uses `sed` to insert after `## [Unreleased]` or `# Changelog`

**Solution:**
Ensure CHANGELOG.md has this structure:
```markdown
# Changelog

## [Unreleased]

(rest of changelog)
```

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Packages - Maven Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [Semantic Versioning Specification](https://semver.org/)
- [Gradle Publishing Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Detekt Custom Rules](https://detekt.dev/docs/introduction/extensions/)

## Feedback and Improvements

After testing, document:
- Issues encountered during testing
- Suggestions for workflow improvements
- Edge cases not covered in this guide
- Documentation gaps or unclear instructions

Create issues on GitHub to track improvements to the release workflow and this testing guide.
