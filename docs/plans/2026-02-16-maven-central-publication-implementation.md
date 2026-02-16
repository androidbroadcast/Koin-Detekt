# Maven Central Publication Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Настроить автоматическую публикацию библиотеки detekt-rules-koin в Maven Central через GitHub Actions при создании релизных тегов.

**Architecture:** Расширяем существующий release workflow, добавляя параллельный job для публикации в Maven Central через Sonatype OSSRH. Используем signing plugin для GPG подписи артефактов и nexus-publish plugin для автоматизации staging/release процесса.

**Tech Stack:**
- Gradle 8.5+
- Gradle Signing Plugin
- Gradle Nexus Publish Plugin 2.0.0
- GPG для подписи артефактов
- GitHub Actions
- Sonatype OSSRH (Maven Central)

**Prerequisites:**
- ✅ GPG ключ создан (Key ID: 5895AA282B06E57E)
- ✅ GitHub Secrets настроены (SIGNING_KEY, SIGNING_PASSWORD, SIGNING_KEY_ID)
- ⏳ OSSRH_USERNAME и OSSRH_PASSWORD нужно проверить

---

## Task 1: Добавить Nexus Publish Plugin в Version Catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

**Step 1: Добавить версию nexus-publish plugin**

```toml
# В секцию [versions]
nexus-publish = "2.0.0"
```

**Step 2: Добавить plugin в каталог**

```toml
# В секцию [plugins]
nexus-publish = { id = "io.github.gradle-nexus.publish-plugin", version.ref = "nexus-publish" }
```

**Step 3: Проверить синтаксис**

Run: `./gradlew --version`
Expected: Команда выполняется без ошибок (проверка парсинга toml)

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: add nexus-publish plugin to version catalog

- Add io.github.gradle-nexus.publish-plugin v2.0.0
- Required for Maven Central publication via Sonatype OSSRH

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Настроить Signing и Nexus Publishing в Build Script

**Files:**
- Modify: `build.gradle.kts`

**Step 1: Добавить плагины в секцию plugins**

После строки 4 (`alias(libs.plugins.kover)`) добавить:

```kotlin
alias(libs.plugins.nexus.publish)
id("signing")
```

**Step 2: Добавить конфигурацию signing после секции publishing (после строки 77)**

```kotlin
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Signing configuration for Maven Central
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

signing {
    // Use in-memory PGP keys from environment variables (for CI/CD)
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    } else {
        // For local development: use GPG agent
        // GPG key must be available in local keyring
        val signingKeyId = System.getenv("SIGNING_KEY_ID")
        if (signingKeyId != null) {
            useGpgCmd()
            sign(publishing.publications["maven"])
        }
    }
}
```

**Step 3: Добавить конфигурацию nexusPublishing после signing**

```kotlin
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Nexus Publishing configuration for Maven Central
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

nexusPublishing {
    repositories {
        sonatype {
            // Use s01.oss.sonatype.org for newer projects
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

            // Credentials from environment variables
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }

    // Configuration for staging repositories
    this.packageGroup.set(project.group.toString())

    // Timeout for operations (staging close can take time)
    transitionCheckOptions {
        maxRetries.set(100)
        delayBetween.set(java.time.Duration.ofSeconds(5))
    }
}
```

**Step 4: Проверить синтаксис и наличие задач**

Run: `./gradlew tasks --group publishing`
Expected: Видим новые задачи:
- `publishToSonatype`
- `closeAndReleaseSonatypeStagingRepository`
- `closeSonatypeStagingRepository`
- `releaseSonatypeStagingRepository`

**Step 5: Commit**

```bash
git add build.gradle.kts
git commit -m "build: configure signing and Maven Central publishing

- Add signing plugin with in-memory PGP keys support for CI
- Add nexus-publish plugin for automated staging/release
- Configure Sonatype OSSRH repositories (s01.oss.sonatype.org)
- Set up transition check options for staging operations

Required env vars:
- SIGNING_KEY (base64-encoded GPG private key)
- SIGNING_PASSWORD (GPG key passphrase)
- OSSRH_USERNAME (Sonatype username)
- OSSRH_PASSWORD (Sonatype password/token)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Тест - Локальная подпись артефактов

**Files:**
- None (testing only)

**Step 1: Установить переменные окружения**

```bash
export SIGNING_KEY=$(cat /tmp/signing-key-base64.txt)
export SIGNING_PASSWORD="mAhHT9DClYrvVfS1NPJe"
```

**Step 2: Собрать и подписать артефакты**

Run: `./gradlew clean build sign`
Expected:
- BUILD SUCCESSFUL
- No signing errors

**Step 3: Проверить наличие .asc файлов**

Run: `ls -la build/libs/*.asc`
Expected: Видим .asc файлы для каждого JAR:
```
-rw-r--r--  detekt-rules-koin-0.4.1-SNAPSHOT.jar.asc
-rw-r--r--  detekt-rules-koin-0.4.1-SNAPSHOT-javadoc.jar.asc
-rw-r--r--  detekt-rules-koin-0.4.1-SNAPSHOT-sources.jar.asc
```

**Step 4: Проверить валидность подписи**

Run: `gpg --verify build/libs/detekt-rules-koin-0.4.1-SNAPSHOT.jar.asc build/libs/detekt-rules-koin-0.4.1-SNAPSHOT.jar`
Expected:
```
gpg: Good signature from "Kirill Rozov <kirill@androidbroadcast.dev>"
```

**Step 5: Очистить переменные окружения**

```bash
unset SIGNING_KEY
unset SIGNING_PASSWORD
```

**Verification:** ✅ Артефакты успешно подписываются локально

---

## Task 4: Добавить Maven Central Publication Job в GitHub Workflow

**Files:**
- Modify: `.github/workflows/release.yml`

**Step 1: Добавить новый job после job "publish" (после строки 138)**

```yaml
  publish-maven-central:
    name: Publish to Maven Central
    runs-on: ubuntu-latest
    needs: build
    # Only publish on non-pre-release tags
    if: needs.build.outputs.is_prerelease == 'false'

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

      - name: Publish to Maven Central
        run: |
          echo "Publishing version ${{ needs.build.outputs.version }} to Maven Central..."
          ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository \
            --no-daemon \
            -Pversion=${{ needs.build.outputs.version }}
        env:
          SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
          SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
          SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }}
          OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}

      - name: Maven Central publication summary
        run: |
          echo "## Maven Central Publication" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "✅ Published version ${{ needs.build.outputs.version }} to Maven Central" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "**Artifact coordinates:**" >> $GITHUB_STEP_SUMMARY
          echo "\`\`\`" >> $GITHUB_STEP_SUMMARY
          echo "dev.androidbroadcast.rules.koin:detekt-koin-rules:${{ needs.build.outputs.version }}" >> $GITHUB_STEP_SUMMARY
          echo "\`\`\`" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "**Note:** Artifacts will be available on Maven Central within 15-30 minutes." >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "Check availability: https://repo1.maven.org/maven2/dev/androidbroadcast/detekt-rules-koin/" >> $GITHUB_STEP_SUMMARY
```

**Step 2: Обновить job "release" - добавить зависимость от publish-maven-central**

Изменить строку 142 с:
```yaml
    needs: [build, publish]
```

на:
```yaml
    needs: [build, publish, publish-maven-central]
```

**Step 3: Обновить release summary - добавить информацию о Maven Central**

В секцию "Next Steps" job "release" (после строки 382) добавить:

```yaml
          echo "4. Verify Maven Central publication (available in 15-30 minutes)" >> $GITHUB_STEP_SUMMARY
```

**Step 4: Проверить синтаксис YAML**

Run: `yamllint .github/workflows/release.yml` (если установлен)
Или проверить онлайн: https://www.yamllint.com/

Expected: No syntax errors

**Step 5: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: add Maven Central publication to release workflow

- Add publish-maven-central job after build
- Publish to Sonatype OSSRH with auto-close and auto-release
- Only publish stable releases (skip pre-releases)
- Add Maven Central publication summary to job output
- Update release job dependencies

Required secrets:
- SIGNING_KEY, SIGNING_PASSWORD, SIGNING_KEY_ID
- OSSRH_USERNAME, OSSRH_PASSWORD

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Проверить GitHub Secrets

**Files:**
- None (GitHub UI verification)

**Step 1: Проверить список secrets через gh CLI**

Run: `gh secret list`
Expected: Должны быть:
- SIGNING_KEY
- SIGNING_KEY_ID
- SIGNING_PASSWORD
- OSSRH_USERNAME
- OSSRH_PASSWORD

**Step 2: Если OSSRH secrets отсутствуют - добавить**

Если OSSRH_USERNAME и OSSRH_PASSWORD отсутствуют:
```bash
# Пользователь должен предоставить свои credentials
gh secret set OSSRH_USERNAME
gh secret set OSSRH_PASSWORD
```

**Step 3: Проверить через веб-интерфейс**

1. Открыть https://github.com/androidbroadcast/Koin-Detekt/settings/secrets/actions
2. Убедиться, что все 5 секретов присутствуют

**Verification:** ✅ Все необходимые секреты настроены

---

## Task 6: Тест SNAPSHOT Публикация (опционально)

**Files:**
- None (manual test)

**Prerequisites:**
- OSSRH credentials настроены
- Локальная подпись работает

**Step 1: Установить OSSRH credentials локально**

```bash
export OSSRH_USERNAME="your-username"
export OSSRH_PASSWORD="your-token"
export SIGNING_KEY=$(cat /tmp/signing-key-base64.txt)
export SIGNING_PASSWORD="mAhHT9DClYrvVfS1NPJe"
```

**Step 2: Опубликовать SNAPSHOT версию**

Run: `./gradlew publishToSonatype -Pversion=0.4.1-SNAPSHOT`
Expected:
- BUILD SUCCESSFUL
- Артефакты загружены в snapshot репозиторий

**Step 3: Проверить наличие в Sonatype**

Открыть: https://s01.oss.sonatype.org/content/repositories/snapshots/dev/androidbroadcast/detekt-rules-koin/0.4.1-SNAPSHOT/

Expected: Видим загруженные артефакты

**Step 4: Очистить переменные**

```bash
unset OSSRH_USERNAME OSSRH_PASSWORD SIGNING_KEY SIGNING_PASSWORD
```

**Verification:** ✅ SNAPSHOT публикация работает (опционально)

---

## Task 7: Создать Test Release для Проверки CI

**Files:**
- None (Git operations)

**Step 1: Создать тестовый тег**

```bash
git tag -a v0.5.0-test1 -m "Test release for Maven Central CI validation

Testing:
- GPG signing in CI environment
- Sonatype OSSRH authentication
- Auto staging close and release
- GitHub Actions workflow integration

This is a test release and will be deleted after validation."
```

**Step 2: Push тега**

Run: `git push origin v0.5.0-test1`
Expected: GitHub Actions workflow запускается

**Step 3: Мониторить выполнение workflow**

Run: `gh run watch`
Or open: https://github.com/androidbroadcast/Koin-Detekt/actions

Expected:
- Job "build" ✅ успешно
- Job "publish" ✅ успешно (GitHub Packages)
- Job "publish-maven-central" ✅ успешно
- Job "release" ✅ успешно

**Step 4: Проверить Sonatype staging**

1. Открыть https://s01.oss.sonatype.org/#stagingRepositories
2. Найти staging repo для dev.androidbroadcast
3. Убедиться, что статус "Released" (или "Closed" если auto-release не сработал)

**Step 5: Удалить тестовый тег и release после проверки**

```bash
# Удалить тег локально
git tag -d v0.5.0-test1

# Удалить тег на remote
git push origin :refs/tags/v0.5.0-test1

# Удалить GitHub Release через gh CLI
gh release delete v0.5.0-test1 --yes
```

**Step 6: Удалить тестовую версию из Maven Central (если успела синхронизироваться)**

Note: Удаление из Maven Central невозможно автоматически. Если версия 0.5.0-test1 синхронизировалась в Maven Central, она останется там навсегда. Поэтому лучше использовать версии с суффиксом `-test` или `-alpha` для тестов.

**Verification:** ✅ CI публикация в Maven Central работает

---

## Task 8: Обновить README.md с Инструкциями Установки

**Files:**
- Modify: `README.md`

**Step 1: Обновить версию в примерах установки**

Изменить строки 25 и 33 с версии `0.4.0` на актуальную (например `0.5.0` после первого реального релиза).

**Step 2: Добавить badge Maven Central**

После строки 3 (после badge Latest Release) добавить:

```markdown
[![Maven Central](https://img.shields.io/maven-central/v/dev.androidbroadcast/detekt-rules-koin)](https://central.sonatype.com/artifact/dev.androidbroadcast/detekt-rules-koin)
```

**Step 3: Добавить секцию о доступности**

После секции "Installation" (после строки 36) добавить:

```markdown
### Availability

This library is published to:
- **Maven Central** - primary distribution (recommended)
- **GitHub Packages** - alternative source

For Maven Central, no additional repository configuration is needed - it's available by default in all Gradle projects.
```

**Step 4: Проверить форматирование**

Run: `cat README.md | head -50`
Expected: Форматирование корректное, ссылки валидные

**Step 5: Commit**

```bash
git add README.md
git commit -m "docs: update README with Maven Central availability

- Add Maven Central badge
- Add availability section explaining distribution channels
- Update installation instructions

Maven Central is now the primary distribution channel,
with GitHub Packages as an alternative.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 9: Обновить CHANGELOG.md

**Files:**
- Modify: `CHANGELOG.md` (если существует)

**Step 1: Проверить наличие CHANGELOG**

Run: `ls -la CHANGELOG.md`
Expected: Файл существует или нужно создать

**Step 2: Добавить запись о Maven Central публикации**

В секции `## [Unreleased]` добавить:

```markdown
### Infrastructure

- **Maven Central Publication**: Library is now published to Maven Central alongside GitHub Packages
  - Automatic publication via GitHub Actions on release tags
  - GPG-signed artifacts for security and authenticity
  - Available at `dev.androidbroadcast.rules.koin:detekt-koin-rules` coordinates
  - Synchronization to Maven Central within 15-30 minutes of release
```

**Step 3: Commit**

```bash
git add CHANGELOG.md
git commit -m "docs: add Maven Central publication to CHANGELOG

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 10: Финальная Проверка и Cleanup

**Files:**
- None (verification only)

**Step 1: Проверить статус репозитория**

Run: `git status`
Expected: Working tree clean

**Step 2: Проверить что все коммиты на main**

Run: `git log --oneline -10`
Expected: Видим все коммиты из этого плана

**Step 3: Push всех изменений**

Run: `git push origin main`
Expected: All changes pushed successfully

**Step 4: Проверить последний workflow run**

Run: `gh run list --limit 5`
Expected: Последний run успешный (если был push)

**Step 5: Очистить временные файлы**

```bash
rm -f /tmp/signing-key-base64.txt
rm -f /tmp/private-key.asc
rm -f /tmp/create-gpg-key-batch.sh
```

**Verification:** ✅ Все изменения закоммичены и запушены

---

## Post-Implementation Checklist

После выполнения всего плана проверить:

- [ ] `gradle/libs.versions.toml` содержит nexus-publish plugin
- [ ] `build.gradle.kts` настроен signing и nexusPublishing
- [ ] `.github/workflows/release.yml` содержит publish-maven-central job
- [ ] Все 5 GitHub Secrets настроены (SIGNING_*, OSSRH_*)
- [ ] Локальная подпись артефактов работает
- [ ] README.md обновлён с Maven Central badge и инструкциями
- [ ] CHANGELOG.md содержит запись о Maven Central
- [ ] Test release прошел успешно (опционально)

## Next Release Process

Для следующего релиза (например v0.5.0):

1. Убедитесь что все изменения закоммичены
2. Создайте тег: `git tag -a v0.5.0 -m "Release v0.5.0"`
3. Push тега: `git push origin v0.5.0`
4. GitHub Actions автоматически:
   - Соберёт и протестирует
   - Опубликует в GitHub Packages
   - Опубликует в Maven Central
   - Создаст GitHub Release
   - Обновит CHANGELOG
5. Через 15-30 минут проверьте доступность на Maven Central:
   https://repo1.maven.org/maven2/dev/androidbroadcast/detekt-rules-koin/

## Troubleshooting

### Если signing не работает в CI:
- Проверить что SIGNING_KEY это base64 строка БЕЗ переносов строк
- Проверить что SIGNING_PASSWORD корректный
- Посмотреть логи GitHub Actions для деталей ошибки

### Если Sonatype authentication failed:
- Проверить OSSRH_USERNAME и OSSRH_PASSWORD
- Убедиться что используете User Token, а не пароль
- Проверить что groupId dev.androidbroadcast верифицирован в Sonatype

### Если staging close failed:
- Открыть https://s01.oss.sonatype.org/#stagingRepositories
- Найти staging repo и посмотреть Activity log
- Проверить что все артефакты присутствуют (jar, sources, javadoc, .asc, .pom)
- Drop failed repo и попробовать снова

---

**Plan Status:** Ready for Execution
**Estimated Time:** 1-2 hours (including testing)
**Risk Level:** Low (all prerequisites met, design approved)
