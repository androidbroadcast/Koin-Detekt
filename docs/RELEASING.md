# Release Process

This document describes the release process for the NuGet Package Template project.

## Overview

Releases are automated through GitHub Actions. When a version tag is pushed, the workflow:
1. Builds the package
2. Publishes to GitHub Packages
3. Creates a GitHub Release with changelog
4. Generates release artifacts

## Prerequisites

Before creating a release, ensure:

- [ ] All changes are merged to `main` branch
- [ ] CI/CD pipeline is passing on `main`
- [ ] Code quality checks pass
- [ ] Version number follows semantic versioning
- [ ] CHANGELOG.md is up to date with unreleased changes

## Release Process

### 1. Update Version Number

The version number is defined in `src/Directory.Build.props`:

```xml
<Version>1.0.0</Version>
```

Update this to your target release version following [Semantic Versioning](https://semver.org/):
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

### 2. Update CHANGELOG.md

Move items from the `[Unreleased]` section to a new version section:

```markdown
## [1.0.0] - 2024-01-15

### Added
- New feature description

### Changed
- Changed behavior description

### Fixed
- Bug fix description
```

### 3. Commit Changes

```bash
git add src/Directory.Build.props CHANGELOG.md
git commit -m "chore: prepare release v1.0.0"
git push origin main
```

### 4. Create and Push Tag

```bash
# Create an annotated tag
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push the tag to trigger the release workflow
git push origin v1.0.0
```

**Important**: The tag must start with `v` followed by the version number (e.g., `v1.0.0`).

### 5. Monitor Release Workflow

1. Go to the [Actions tab](../../actions) in GitHub
2. Watch the "Release" workflow execution
3. Verify all jobs complete successfully:
   - **build**: Compiles and packages the library
   - **publish**: Uploads to GitHub Packages
   - **release**: Creates GitHub Release with changelog

### 6. Verify Release

After the workflow completes:

#### Check GitHub Release
1. Navigate to [Releases](../../releases)
2. Verify the new release is listed
3. Confirm the changelog is included
4. Download and inspect the `.nupkg` artifact

#### Check GitHub Packages
1. Navigate to the [Packages](../../packages) page
2. Verify the new version is published
3. Confirm package metadata is correct

#### Test Package Installation
```bash
# Add GitHub Packages source (if not already configured)
dotnet nuget add source \
  --username YOUR_GITHUB_USERNAME \
  --password YOUR_GITHUB_TOKEN \
  --store-password-in-clear-text \
  --name github \
  "https://nuget.pkg.github.com/OWNER/index.json"

# Install the package in a test project
dotnet add package YourPackageName --version 1.0.0
```

## Version Formats

### Stable Releases
Format: `v1.0.0`, `v2.1.3`, etc.

Tags matching this pattern create **stable releases** with:
- Pre-release flag: `false`
- Full changelog extraction
- Production-ready designation

### Pre-releases
Format: `v1.0.0-alpha.1`, `v2.0.0-beta.2`, `v1.5.0-rc.1`, etc.

Tags with pre-release identifiers create **pre-releases** with:
- Pre-release flag: `true`
- Limited changelog (since last stable release)
- Development/testing designation

Common pre-release identifiers:
- `alpha`: Early testing versions
- `beta`: Feature-complete testing versions
- `rc`: Release candidates

## What Gets Published

### GitHub Packages
- NuGet package (`.nupkg`)
- Symbols package (`.snupkg`)
- Package metadata and description
- Dependency information

### GitHub Release
- Release notes from CHANGELOG.md
- Downloadable package artifacts
- Source code archives (automatic)
- Pre-release designation (if applicable)

### CHANGELOG.md
The workflow extracts relevant sections from CHANGELOG.md:
- **Stable releases**: All changes for that version
- **Pre-releases**: All changes since the last stable release

## Troubleshooting

### Workflow Fails at Build Step

**Symptoms**: Build job fails, no package created

**Solutions**:
- Check build logs for compilation errors
- Verify all tests pass locally: `dotnet test`
- Ensure `Directory.Build.props` version is valid
- Confirm all dependencies are available

### Workflow Fails at Publish Step

**Symptoms**: Build succeeds, but package doesn't upload

**Solutions**:
- Verify `GITHUB_TOKEN` permissions in workflow
- Check package name doesn't conflict with existing packages
- Ensure GitHub Packages is enabled for the repository
- Verify package version doesn't already exist

### Workflow Fails at Release Step

**Symptoms**: Package published, but GitHub Release not created

**Solutions**:
- Verify CHANGELOG.md has an entry for the version
- Check changelog format matches expected structure
- Ensure tag version matches CHANGELOG version
- Verify `contents: write` permission is granted

### Release Created but Empty Changelog

**Symptoms**: Release exists but has no description

**Solutions**:
- Verify CHANGELOG.md format:
  ```markdown
  ## [1.0.0] - 2024-01-15

  ### Added
  - Feature description
  ```
- Ensure version in tag matches CHANGELOG exactly
- Check for proper heading levels (##)
- Verify date format is correct

### Package Not Showing in GitHub Packages

**Symptoms**: Release successful, but package not visible

**Solutions**:
- Wait a few minutes (packages may take time to index)
- Check repository settings > Packages
- Verify package visibility settings
- Confirm you have permissions to view packages

## Manual Verification Steps

After each release, manually verify:

1. **Version Consistency**
   - Tag version matches `Directory.Build.props`
   - Package version matches release version
   - CHANGELOG version matches release version

2. **Package Contents**
   - Download `.nupkg` from release
   - Extract and inspect assembly version
   - Verify all expected files are included
   - Check assembly attributes and metadata

3. **Installation Test**
   - Create a new test project
   - Install the released package
   - Verify package installs without errors
   - Test basic functionality

4. **Documentation**
   - Release notes are accurate and complete
   - Links in release notes work correctly
   - Package description is correct on GitHub Packages

## Rollback Procedure

If a release has critical issues:

### Delete the Release (GitHub UI)
1. Go to [Releases](../../releases)
2. Click "Edit" on the problematic release
3. Click "Delete this release"
4. Confirm deletion

### Delete the Tag
```bash
# Delete local tag
git tag -d v1.0.0

# Delete remote tag
git push origin :refs/tags/v1.0.0
```

### Delete Package Version
1. Navigate to the package on GitHub
2. Click "Package settings"
3. Find the version under "Manage versions"
4. Click "Delete" for that version

### Create Fixed Release
1. Fix the issues in code
2. Increment version (e.g., `v1.0.0` → `v1.0.1`)
3. Follow the normal release process

## Best Practices

1. **Test Before Release**
   - Always run full CI/CD pipeline on `main` before tagging
   - Perform manual testing of critical functionality
   - Review all changes in the release

2. **Clear Communication**
   - Write detailed, user-focused changelog entries
   - Highlight breaking changes prominently
   - Include migration guides for major versions

3. **Version Strategy**
   - Use pre-releases for testing (`alpha`, `beta`, `rc`)
   - Only create stable releases when fully tested
   - Follow semantic versioning strictly

4. **Regular Releases**
   - Release early and often for minor changes
   - Don't batch too many changes in one release
   - Consider release cadence (weekly, bi-weekly, etc.)

5. **Documentation**
   - Keep CHANGELOG.md up to date continuously
   - Document breaking changes immediately
   - Update version in `Directory.Build.props` just before release

## Additional Resources

- [Semantic Versioning Specification](https://semver.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [GitHub Packages Documentation](https://docs.github.com/packages)
- [GitHub Actions Documentation](https://docs.github.com/actions)
