# Release Process

This document describes the release process for the detekt-rules-koin library.

## Overview

Releases are automated through GitHub Actions. When a version tag is pushed, the workflow:
1. Builds the library (main JAR, sources JAR, javadoc JAR)
2. Generates SHA-256 and SHA-512 checksums for all JARs
3. Publishes artifacts to GitHub Packages (Maven repository)
4. Creates a draft GitHub Release with artifacts and auto-generated release notes
5. Updates CHANGELOG.md in the main branch

## Prerequisites

Before creating a release, ensure:

- [ ] All changes are merged to `main` branch
- [ ] CI/CD pipeline is passing on `main`
- [ ] All tests pass (`./gradlew test`)
- [ ] Code quality checks pass (explicit API mode enforced)
- [ ] Version number follows semantic versioning
- [ ] CHANGELOG.md is up to date with unreleased changes

## Release Process

### 1. Version Number

The version is defined in `build.gradle.kts`:

```kotlin
version = "0.1.0-SNAPSHOT"
```

**Important**: The workflow overrides this version with the git tag version during the release build. You don't need to manually update `build.gradle.kts` before tagging.

Update the version in `build.gradle.kts` after the release to prepare for the next development cycle:

```kotlin
version = "0.2.0-SNAPSHOT"
```

Follow [Semantic Versioning](https://semver.org/):
- **MAJOR**: Breaking changes to public API
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

### 2. Update CHANGELOG.md

Ensure CHANGELOG.md has an `## [Unreleased]` section with changes for this release:

```markdown
# Changelog

## [Unreleased]

### Added
- New detekt rule for Koin module validation

### Fixed
- Fixed false positive in dependency injection check
```

The workflow will automatically insert a new release section after the `[Unreleased]` heading.

### 3. Create and Push Tag

Create a semantic version tag with `v` prefix:

```bash
# For stable releases
git tag -a v0.1.0 -m "Release version 0.1.0"
git push origin v0.1.0

# For pre-releases (alpha, beta, rc)
git tag -a v0.1.0-alpha.1 -m "Release version 0.1.0-alpha.1"
git push origin v0.1.0-alpha.1
```

**Important**: The tag must:
- Start with `v` (e.g., `v0.1.0`)
- Follow semantic versioning format: `vMAJOR.MINOR.PATCH` or `vMAJOR.MINOR.PATCH-prerelease.N`

### 4. Monitor Release Workflow

1. Go to the [Actions tab](../../actions) in GitHub
2. Watch the "Release" workflow execution
3. Verify all jobs complete successfully:
   - **build**: Validates version, builds JARs, runs tests
   - **publish**: Generates checksums, publishes to GitHub Packages
   - **release**: Creates draft GitHub Release, updates CHANGELOG.md

### 5. Review and Publish Draft Release

After the workflow completes:

1. Navigate to [Releases](../../releases)
2. Find the new **draft** release
3. Review the auto-generated release notes
4. Verify all artifacts are attached:
   - `detekt-rules-koin-X.Y.Z.jar` (main library)
   - `detekt-rules-koin-X.Y.Z-sources.jar` (sources)
   - `detekt-rules-koin-X.Y.Z-javadoc.jar` (javadoc)
   - `*.sha256` and `*.sha512` checksum files (6 files total)
5. Click **"Publish release"** to make it public

### 6. Verify Release

After publishing the release:

#### Check GitHub Release
1. Navigate to [Releases](../../releases)
2. Verify the release is published
3. Confirm artifacts are downloadable
4. Verify checksums match

#### Check GitHub Packages
1. Navigate to the [Packages](../../packages) page
2. Verify the new version is published
3. Check package metadata (group, artifact, version)

#### Check CHANGELOG Update
```bash
git pull origin main
head -30 CHANGELOG.md
```

Verify:
- New version section appears after `## [Unreleased]`
- Release date is correct
- Auto-generated release notes are included

#### Test Package Installation

Add GitHub Packages repository to your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/androidbroadcast/Koin-Detekt")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    detektPlugins("io.github.krozov:detekt-rules-koin:0.1.0")
}
```

Run detekt to verify the rules are loaded:
```bash
./gradlew detekt
```

## Version Formats

### Stable Releases
Format: `v0.1.0`, `v1.0.0`, `v2.1.3`

Creates **stable releases** with:
- Pre-release flag: `false`
- Production-ready designation
- Published to GitHub Packages

### Pre-releases
Format: `v0.1.0-alpha.1`, `v1.0.0-beta.2`, `v0.2.0-rc.1`

Creates **pre-releases** with:
- Pre-release flag: `true`
- Development/testing designation
- Published to GitHub Packages (can be installed with specific version)

Common pre-release identifiers:
- `alpha`: Early testing versions, API may change
- `beta`: Feature-complete testing versions, API mostly stable
- `rc`: Release candidates, API stable, final testing

## What Gets Published

### GitHub Packages (Maven)
- Main JAR: `detekt-rules-koin-X.Y.Z.jar`
- Sources JAR: `detekt-rules-koin-X.Y.Z-sources.jar`
- Javadoc JAR: `detekt-rules-koin-X.Y.Z-javadoc.jar`
- POM file with metadata and dependencies
- Maven coordinates: `io.github.krozov:detekt-rules-koin:X.Y.Z`

### GitHub Release
- Auto-generated release notes from commits
- All 3 JARs as downloadable artifacts
- 6 checksum files (SHA-256 and SHA-512 for each JAR)
- Source code archives (automatic)
- Pre-release designation (if applicable)

### CHANGELOG.md
- New version section inserted after `## [Unreleased]`
- Release date in `YYYY-MM-DD` format
- Auto-generated release notes from GitHub
- Committed back to main branch

## Troubleshooting

### Workflow Fails at Build Step

**Symptoms**: Build job fails, no artifacts created

**Solutions**:
- Check build logs for compilation errors
- Verify all tests pass locally: `./gradlew test`
- Ensure Kotlin version is compatible
- Confirm all dependencies are available

### Workflow Fails at Publish Step

**Symptoms**: Build succeeds, but publish fails

**Solutions**:
- Verify `GITHUB_TOKEN` has `packages:write` permission
- Check GitHub Packages is enabled for the repository
- Ensure version doesn't already exist in GitHub Packages
- Verify Maven coordinates are correct in `build.gradle.kts`

### Version Mismatch Error

**Symptoms**: Workflow uses wrong version (0.1.0-SNAPSHOT instead of tag version)

**Solutions**:
- Verify tag format: must be `vX.Y.Z` or `vX.Y.Z-suffix`
- Check workflow passes `-Pversion=X.Y.Z` to Gradle commands
- Confirm `build.gradle.kts` accepts version override

### CHANGELOG Commit Fails

**Symptoms**: Release created but CHANGELOG.md not updated in main

**Solutions**:
- Check branch protection rules (may block github-actions bot)
- Verify `GITHUB_TOKEN` has `contents:write` permission
- Ensure no merge conflicts in CHANGELOG.md
- Check step output: `changelog_updated` should be `true`

### Git Checkout Fails

**Symptoms**: "Your local changes would be overwritten by checkout"

**Solutions**:
- Workflow now checks out main BEFORE modifying CHANGELOG.md (fixed)
- If still occurring, check for uncommitted changes in release job

## Best Practices

1. **Test Before Release**
   - Run full test suite: `./gradlew test`
   - Run detekt on the project itself: `./gradlew detekt`
   - Verify reproducible builds: `./gradlew clean build && ./gradlew clean build`

2. **Clear Communication**
   - Write descriptive commit messages
   - Keep CHANGELOG.md up to date continuously
   - Highlight breaking changes prominently

3. **Version Strategy**
   - Use pre-releases for testing (`alpha`, `beta`, `rc`)
   - Only create stable releases when fully tested
   - Follow semantic versioning strictly

4. **Regular Releases**
   - Release bug fixes quickly (PATCH versions)
   - Batch related features in MINOR versions
   - Communicate breaking changes well in advance of MAJOR versions

## Rollback Procedure

If a release has critical issues:

### Delete the GitHub Release
```bash
gh release delete v0.1.0 --yes
```

Or via GitHub UI:
1. Go to [Releases](../../releases)
2. Click "Edit" on the problematic release
3. Click "Delete this release"

### Delete the Git Tag
```bash
# Delete local tag
git tag -d v0.1.0

# Delete remote tag
git push origin :refs/tags/v0.1.0
```

### Delete Package Version from GitHub Packages

**Note**: GitHub Packages doesn't allow deleting public package versions. You must:
1. Publish a new fixed version (e.g., `v0.1.1`)
2. Mark the broken version as deprecated (if supported)
3. Document the issue in release notes

### Revert CHANGELOG.md Commit
```bash
# Find the CHANGELOG commit hash
git log --oneline -n 5

# Revert the commit
git revert <commit-hash>
git push origin main
```

### Create Fixed Release
1. Fix the issues in code
2. Increment version appropriately:
   - Critical bug: `v0.1.0` → `v0.1.1` (PATCH)
   - Major bug: `v0.1.0` → `v0.2.0` (MINOR if no breaking changes)
3. Update CHANGELOG.md with fix details
4. Follow normal release process

## Additional Resources

- [Semantic Versioning Specification](https://semver.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [GitHub Packages - Maven](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle Publishing Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)
