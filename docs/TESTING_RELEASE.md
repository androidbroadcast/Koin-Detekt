# Release Workflow Testing Guide

This document provides a comprehensive guide for testing the automated release workflow for the TUI Skeleton project.

## Table of Contents

- [Overview](#overview)
- [Test Release Procedure](#test-release-procedure)
- [Workflow Execution Monitoring](#workflow-execution-monitoring)
- [Verification Steps](#verification-steps)
- [Version Format Tests](#version-format-tests)
- [Failure Scenario Testing](#failure-scenario-testing)
- [Success Criteria](#success-criteria)

## Overview

The release workflow (`release.yml`) automates the following tasks:

1. **Build and Validate**: Version validation, build, and test execution
2. **Publish**: Checksum generation and GitHub Packages publishing
3. **Release**: Draft GitHub release creation and CHANGELOG update

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

**Note**: Pre-release versions (containing hyphens) will trigger the workflow but can be easily cleaned up.

## Workflow Execution Monitoring

### 1. Monitor Workflow Start

Navigate to:
```
https://github.com/<owner>/<repo>/actions/workflows/release.yml
```

Verify:
- [ ] Workflow triggered by the tag push
- [ ] All three jobs appear: `build`, `publish`, `release`
- [ ] Jobs execute in the correct order

### 2. Monitor Build Job

Check the following steps:

- [ ] **Checkout code**: Repository cloned successfully
- [ ] **Set up Rust**: Correct Rust version installed
- [ ] **Validate version**: Tag matches Cargo.toml version
- [ ] **Build**: Binary built without errors
- [ ] **Run tests**: All tests pass
- [ ] **Upload artifact**: Binary artifact uploaded

Expected output for version validation:
```
Tag version: 0.1.0-test.1
Cargo version: 0.1.0
Version validation successful!
```

### 3. Monitor Publish Job

Check the following steps:

- [ ] **Download artifact**: Binary artifact retrieved
- [ ] **Generate checksums**: SHA256 checksums created
- [ ] **Publish to GitHub Packages**: Package published successfully

Expected checksum format:
```
abc123... tui-skeleton-x86_64-unknown-linux-gnu
def456... tui-skeleton-aarch64-unknown-linux-gnu
```

### 4. Monitor Release Job

Check the following steps:

- [ ] **Download artifact**: Binary artifact retrieved
- [ ] **Download checksums**: Checksum file retrieved
- [ ] **Create release**: Draft release created on GitHub
- [ ] **Update CHANGELOG**: CHANGELOG.md updated and committed

## Verification Steps

### 1. Verify GitHub Release

Navigate to:
```
https://github.com/<owner>/<repo>/releases
```

Verify the draft release:

- [ ] Release title matches: "Release vX.Y.Z"
- [ ] Release is marked as draft
- [ ] Pre-release flag is set correctly
- [ ] Release notes contain "Changes in this release:" header
- [ ] Release notes reference the tag comparison link

**Assets verification:**
- [ ] Binary artifact is attached
- [ ] `checksums.txt` file is attached
- [ ] Both files are downloadable

### 2. Verify GitHub Packages

Navigate to:
```
https://github.com/<owner>/<repo>/pkgs/cargo/tui-skeleton
```

Verify:
- [ ] Package version appears in the list
- [ ] Package size is reasonable
- [ ] Package is linked to the correct commit/tag
- [ ] Installation instructions are present

### 3. Verify CHANGELOG Update

Check the repository:

```bash
# Pull the latest changes
git pull origin main

# View the updated CHANGELOG
cat CHANGELOG.md
```

Verify:
- [ ] New version section added at the top
- [ ] Release date is correct (YYYY-MM-DD format)
- [ ] Section includes "Changes in this release:" header
- [ ] Git tag comparison link is present
- [ ] Unreleased section remains intact
- [ ] Commit message is correct: "chore: update CHANGELOG for vX.Y.Z"

### 4. Verify Checksums

Download and verify the checksums:

```bash
# Download the release assets
wget https://github.com/<owner>/<repo>/releases/download/v0.1.0-test.1/checksums.txt
wget https://github.com/<owner>/<repo>/releases/download/v0.1.0-test.1/tui-skeleton-x86_64-unknown-linux-gnu

# Verify the checksum
sha256sum -c checksums.txt --ignore-missing
```

Expected output:
```
tui-skeleton-x86_64-unknown-linux-gnu: OK
```

### 5. Verify Installation

Test installing the binary:

```bash
# Make the binary executable
chmod +x tui-skeleton-x86_64-unknown-linux-gnu

# Test execution
./tui-skeleton-x86_64-unknown-linux-gnu --version

# Expected output
tui-skeleton 0.1.0
```

## Version Format Tests

### Valid Version Formats

Test these version formats to ensure they trigger the workflow correctly:

1. **Standard release**: `v1.0.0`
   ```bash
   git tag v1.0.0 && git push origin v1.0.0
   ```

2. **Pre-release with hyphen**: `v0.1.0-alpha.1`
   ```bash
   git tag v0.1.0-alpha.1 && git push origin v0.1.0-alpha.1
   ```

3. **Pre-release with label**: `v0.1.0-rc.1`
   ```bash
   git tag v0.1.0-rc.1 && git push origin v0.1.0-rc.1
   ```

4. **Build metadata**: `v0.1.0+build.123`
   ```bash
   git tag v0.1.0+build.123 && git push origin v0.1.0+build.123
   ```

### Invalid Version Formats

Verify these formats do NOT trigger the workflow:

1. **Missing 'v' prefix**: `1.0.0`
2. **Two-part version**: `v1.0`
3. **Non-numeric parts**: `vX.Y.Z`
4. **No tag prefix**: `release-1.0.0`

## Failure Scenario Testing

### 1. Version Mismatch

Test version validation failure:

```bash
# Modify Cargo.toml version to not match tag
# Then push tag v0.2.0

# Expected: Build job fails at "Validate version" step
```

### 2. Build Failure

Test build failure handling:

```bash
# Introduce a syntax error in src/main.rs
# Commit and push tag

# Expected: Build job fails at "Build" step
# Expected: Publish and Release jobs are skipped
```

### 3. Test Failure

Test test failure handling:

```bash
# Modify a test to fail
# Commit and push tag

# Expected: Build job fails at "Run tests" step
# Expected: Publish and Release jobs are skipped
```

### 4. Network/API Failures

Monitor for transient failures:

- GitHub API rate limiting
- Package registry connectivity issues
- Release creation failures

Expected behavior:
- Clear error messages in workflow logs
- No partial releases (atomic failure)

## Success Criteria

Use this checklist to confirm a successful release:

### Pre-Release

- [ ] All CI workflows pass on main branch
- [ ] Version in Cargo.toml is updated
- [ ] CHANGELOG.md has "Unreleased" section with changes

### During Release

- [ ] Tag pushed successfully
- [ ] Workflow triggered automatically
- [ ] All three jobs complete successfully
- [ ] No error messages in workflow logs

### Post-Release

- [ ] Draft release created on GitHub
- [ ] Binary artifacts attached to release
- [ ] Checksums file attached to release
- [ ] Package published to GitHub Packages
- [ ] CHANGELOG.md updated with new version
- [ ] CHANGELOG commit pushed to main branch
- [ ] Binary downloads and executes correctly
- [ ] Checksums verify correctly

### Cleanup Test Releases

After testing, clean up test releases:

```bash
# Delete the test tag locally
git tag -d v0.1.0-test.1

# Delete the test tag remotely
git push origin :refs/tags/v0.1.0-test.1

# Delete the GitHub release (via web UI or gh CLI)
gh release delete v0.1.0-test.1 --yes

# Delete the GitHub Package version (via web UI)
# Navigate to Packages → tui-skeleton → Package settings
```

## Troubleshooting

### Workflow Not Triggering

Check:
- Tag format matches `v*.*.*` pattern
- Tag was pushed to remote: `git ls-remote --tags origin`
- Workflow file is on the default branch

### Permission Errors

Verify:
- Repository settings → Actions → General → Workflow permissions
- "Read and write permissions" is enabled
- "Allow GitHub Actions to create and approve pull requests" is checked (if needed)

### Package Publishing Fails

Check:
- GITHUB_TOKEN has packages:write permission
- Package visibility settings in repository
- No conflicting package versions

### CHANGELOG Commit Fails

Verify:
- No branch protection rules blocking the bot
- GITHUB_TOKEN has contents:write permission
- No merge conflicts in CHANGELOG.md

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Packages Documentation](https://docs.github.com/en/packages)
- [Semantic Versioning Specification](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)

## Feedback and Improvements

After testing, document:
- Any issues encountered
- Suggested improvements to the workflow
- Additional test cases to consider
- Documentation gaps or unclear instructions

Create issues or submit PRs to improve this guide and the release workflow.
