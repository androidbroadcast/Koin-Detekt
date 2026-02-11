# GitHub Release Automation Design

**Date**: 2026-02-11
**Status**: Approved
**Author**: Design Session

## Overview

Automated release system for detekt-rules-koin that publishes to GitHub Packages and creates GitHub Releases triggered by semantic version tags.

## Goals

1. Automate build and publication to GitHub Maven repository (GitHub Packages)
2. Create GitHub Releases with artifacts and auto-generated changelog
3. Support semantic versioning tags (X.Y.Z format)
4. Generate and maintain CHANGELOG.md
5. Validate versions and support pre-releases
6. Future-ready for Maven Central integration

## Non-Goals

- Maven Central publication (deferred to future work)
- Automated version bumping in source files
- Multi-platform builds (Kotlin Multiplatform)

## Architecture

### Workflow Structure

Single workflow file `.github/workflows/release.yml` with three sequential jobs:

```
Trigger: git tag v*.*.* push
    ↓
Build Job (validation + compilation)
    ↓
Publish Job (GitHub Packages + checksums)
    ↓
Release Job (Draft Release + CHANGELOG)
```

### Job 1: Build & Validation

**Responsibilities:**
- Validate tag format (semantic versioning)
- Detect pre-release versions (contains `-` suffix)
- Build all artifacts (main JAR, sources, javadoc)
- Run tests
- Upload artifacts for downstream jobs

**Key Steps:**

1. **Version Validation**
   ```bash
   TAG_VERSION=${GITHUB_REF#refs/tags/v}
   # Validate: X.Y.Z or X.Y.Z-suffix (e.g., 1.0.0-alpha.1)
   if ! [[ $TAG_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$ ]]; then
     exit 1
   fi
   ```

2. **Pre-release Detection**
   ```bash
   if [[ $TAG_VERSION =~ - ]]; then
     echo "is_prerelease=true" >> $GITHUB_OUTPUT
   fi
   ```

3. **Build Process**
   - `./gradlew build -x test` — compilation and packaging
   - `./gradlew test` — run test suite
   - `./gradlew jar sourcesJar javadocJar` — generate all JARs

4. **Artifact Upload**
   - Upload `build/libs/*.jar` as `build-artifacts`
   - Used by publish and release jobs

**Optimizations:**
- Gradle caching via `actions/setup-java@v4` with `cache: gradle`
- Fail fast on validation errors

### Job 2: Publish to GitHub Packages

**Responsibilities:**
- Generate checksums (SHA-256, SHA-512)
- Publish Maven artifacts to GitHub Packages
- Save checksums for Release job

**Dependencies:** `needs: build`

**Key Steps:**

1. **Download Build Artifacts**
   ```yaml
   uses: actions/download-artifact@v4
   with:
     name: build-artifacts
     path: build/libs/
   ```

2. **Generate Checksums**
   ```bash
   cd build/libs
   for file in *.jar; do
     sha256sum "$file" > "$file.sha256"
     sha512sum "$file" > "$file.sha512"
   done
   ```

3. **Publish to GitHub Packages**
   ```bash
   ./gradlew publish
   ```
   Uses environment:
   - `GITHUB_TOKEN`: `${{ secrets.GITHUB_TOKEN }}`
   - `VERSION`: `${{ github.ref_name }}`

4. **Upload Checksums**
   - Upload `*.sha256` and `*.sha512` as `checksums` artifact

**Gradle Configuration Required:**

```kotlin
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/androidbroadcast/Koin-Detekt")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

### Job 3: Create Draft Release

**Responsibilities:**
- Create draft GitHub Release
- Attach artifacts (JARs + checksums)
- Generate release notes using GitHub's built-in feature
- Update CHANGELOG.md in repository

**Dependencies:** `needs: [build, publish]`

**Key Steps:**

1. **Download All Artifacts**
   ```yaml
   uses: actions/download-artifact@v4
   with:
     pattern: '*'
     path: artifacts/
     merge-multiple: true
   ```

2. **Create Draft Release**
   ```yaml
   uses: softprops/action-gh-release@v2
   with:
     draft: true
     prerelease: ${{ needs.build.outputs.is_prerelease }}
     generate_release_notes: true
     files: |
       artifacts/*.jar
       artifacts/*.sha256
       artifacts/*.sha512
     fail_on_unmatched_files: true
   ```

3. **Extract Release Notes**
   - Use `actions/github-script@v7` to fetch release body
   - Store for CHANGELOG.md update

4. **Update CHANGELOG.md**
   ```bash
   echo "## ${{ github.ref_name }} - $(date +%Y-%m-%d)" > new_changelog.md
   echo "" >> new_changelog.md
   echo "${{ steps.release_notes.outputs.result }}" >> new_changelog.md
   echo "" >> new_changelog.md
   cat CHANGELOG.md >> new_changelog.md 2>/dev/null || true
   mv new_changelog.md CHANGELOG.md
   ```

5. **Commit CHANGELOG**
   ```bash
   git config user.name "github-actions[bot]"
   git config user.email "github-actions[bot]@users.noreply.github.com"
   git checkout main
   git add CHANGELOG.md
   git commit -m "docs: update CHANGELOG for ${{ github.ref_name }}"
   git push
   ```
   - Uses `continue-on-error: true` to not block Release creation

## Documentation Updates

### CHANGELOG.md

**Initial Creation:**
```markdown
# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

<!-- Releases are added automatically by GitHub Actions -->
```

**Automated Updates:**
- Prepended with new release section on each release
- Format: `## vX.Y.Z - YYYY-MM-DD`
- Content from GitHub auto-generated release notes

### README.md Badges

**Replace existing static badge with dynamic:**

```markdown
[![Latest Release](https://img.shields.io/github/v/release/androidbroadcast/Koin-Detekt)](https://github.com/androidbroadcast/Koin-Detekt/releases/latest)
[![Release Date](https://img.shields.io/github/release-date/androidbroadcast/Koin-Detekt)](https://github.com/androidbroadcast/Koin-Detekt/releases)
```

**Benefits:**
- Auto-updates without commits
- Clickable link to latest release
- Shows release freshness

**Version in Installation Examples:**
- Update manually or via optional automation
- Low priority (can be done at major releases)

## Error Handling

### Build Failure
- **Impact**: Workflow stops, no publish or release
- **Recovery**: Fix code, delete tag, recreate tag
- **Prevention**: Local testing before tagging

### Publish Failure
- **Impact**: Release job doesn't run (dependency)
- **Recovery**:
  - Fix issue and recreate tag, OR
  - Manually re-run publish job from GitHub UI
- **Artifacts**: Build artifacts already saved

### Release Creation Failure
- **Impact**: Artifacts published but no Release
- **Recovery**:
  - Create Release manually via GitHub UI, OR
  - Re-run release job
- **Note**: Artifacts in GitHub Packages are permanent

### CHANGELOG Commit Failure
- **Impact**: Release exists but CHANGELOG not updated
- **Configuration**: `continue-on-error: true` (non-blocking)
- **Recovery**: Manual CHANGELOG update or next release

### Rollback Strategy

**Complete Failure (nothing published):**
1. Delete tag: `git tag -d vX.Y.Z && git push --delete origin vX.Y.Z`
2. Delete draft Release via GitHub UI
3. Fix issue, recreate tag

**Partial Success (published to Packages):**
- **DO NOT** delete from GitHub Packages (breaks dependencies)
- Create hotfix version (e.g., v1.0.1 if v1.0.0 is broken)
- Mark problematic release as pre-release in GitHub

**Notification on Failure:**
```yaml
- name: Notify on failure
  if: failure()
  run: echo "::error::Release failed for ${{ github.ref_name }}"
  # Optional: Add Slack/Discord webhook
```

## Pre-release Support

### Detection
Tag format with suffix: `v1.0.0-alpha.1`, `v2.0.0-rc.2`, `v1.5.0-beta.3`

### Behavior
- Marked as "Pre-release" in GitHub Release
- Published to GitHub Packages (same repository)
- Included in CHANGELOG.md with pre-release marker
- Not shown as "Latest" in GitHub Releases UI

### Regex Pattern
```regex
^[0-9]+\.[0-9]+\.[0-9]+-[a-zA-Z0-9.]+$
```

## Release Artifacts

### Included Files
1. `detekt-rules-koin-X.Y.Z.jar` — main artifact
2. `detekt-rules-koin-X.Y.Z-sources.jar` — source code
3. `detekt-rules-koin-X.Y.Z-javadoc.jar` — documentation
4. `*.sha256` — SHA-256 checksums for all JARs
5. `*.sha512` — SHA-512 checksums for all JARs

### Checksum Format
```
<hash>  <filename>
```

Example:
```
abc123def456...  detekt-rules-koin-1.0.0.jar
```

## Workflow Permissions

```yaml
permissions:
  contents: write  # Create releases, commit CHANGELOG
  packages: write  # Publish to GitHub Packages
```

## Developer Workflow

### Creating a Release

1. **Ensure code is ready**
   - All tests passing
   - CI green on main branch
   - Version bumped in appropriate files (if applicable)

2. **Create and push tag**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. **Monitor workflow**
   - Check Actions tab (~5-10 minutes)
   - Verify all three jobs succeed

4. **Review draft Release**
   - Check artifacts are attached
   - Verify release notes are accurate
   - Preview CHANGELOG content

5. **Publish Release**
   - Click "Publish release" in GitHub UI
   - CHANGELOG.md auto-commits to main

### For Pre-releases

```bash
git tag v1.0.0-alpha.1
git push origin v1.0.0-alpha.1
```

## Files to Create/Modify

### New Files
1. `.github/workflows/release.yml` (~200-250 lines)
2. `CHANGELOG.md` (initial template)

### Modified Files
1. `build.gradle.kts` — add `publishing` block
2. `README.md` — update badges (one-time change)

## Future Enhancements

### Planned for Later
- Maven Central publication (separate workflow or additional job)
- GPG signing for Maven Central compliance
- SBOM (Software Bill of Materials) generation
- Dependency vulnerability scanning
- Slack/Discord notifications
- Automated version bumping in source files

### Not Planned
- Automated tag creation from commits
- Changelog from conventional commits (using GitHub's native feature)
- Multi-repository releases
- Docker image publication

## Security Considerations

1. **GITHUB_TOKEN**: Automatically provided, scoped to repository
2. **No secrets required**: GitHub Packages uses built-in token
3. **Checksums**: Provide integrity verification for artifacts
4. **Draft releases**: Manual review before public announcement
5. **Version validation**: Prevents accidental invalid tags

## Testing Strategy

### Pre-deployment Testing
1. Test on feature branch with test tags (`v0.0.1-test`)
2. Verify workflow syntax with `act` (local Actions runner)
3. Dry-run publish step locally

### Post-deployment Validation
1. Create test release (`v0.1.0-test`)
2. Verify all artifacts in GitHub Packages
3. Check draft Release content
4. Validate checksums manually
5. Test CHANGELOG.md commit

## Success Metrics

- Release creation time < 10 minutes
- Zero manual steps (except final publish click)
- 100% artifact upload success rate
- CHANGELOG always in sync with releases
- Clear rollback path for failures

## References

- [GitHub Actions: Publishing packages](https://docs.github.com/en/actions/publishing-packages)
- [softprops/action-gh-release](https://github.com/softprops/action-gh-release)
- [Semantic Versioning](https://semver.org/)
- [GitHub Auto-generated release notes](https://docs.github.com/en/repositories/releasing-projects-on-github/automatically-generated-release-notes)
