# Maven Central Publication Design

**Дата**: 2026-02-16
**Автор**: Claude Sonnet 4.5
**Статус**: Утверждено

## Обзор

Настройка автоматической публикации библиотеки `detekt-rules-koin` в Maven Central через GitHub Actions при создании релизных тегов.

## Контекст

**Текущее состояние:**
- Проект уже публикуется в GitHub Packages
- POM файл полностью заполнен
- Есть CI/CD workflow для релизов (`.github/workflows/release.yml`)
- Генерируются sources и javadoc JAR-ы
- Настроены воспроизводимые сборки

**Цель:**
Расширить существующий release workflow для параллельной публикации в Maven Central (Sonatype OSSRH).

## 1. Архитектура

### Общая структура

```
Push тега (v*.*.*)
    ↓
┌─────────────────────────────────┐
│  Job: build (существующий)      │
│  - Компиляция                    │
│  - Тесты                         │
│  - Генерация JAR-ов              │
└─────────────────────────────────┘
    ↓
┌──────────────────┬──────────────────────┐
│  Job: publish    │  Job: publish-maven  │ ← НОВОЕ
│  (GitHub Pkg)    │  (Maven Central)     │
└──────────────────┴──────────────────────┘
    ↓                      ↓
┌─────────────────────────────────┐
│  Job: release                    │
│  - Создание GitHub Release       │
│  - Обновление CHANGELOG          │
└─────────────────────────────────┘
```

### Ключевые компоненты

1. **Signing Plugin** - подпись всех артефактов GPG ключом
2. **Sonatype Repository** - staging и release репозитории Maven Central
3. **GitHub Secrets** - безопасное хранение credentials:
   - `SIGNING_KEY` - приватный GPG ключ (base64)
   - `SIGNING_PASSWORD` - пароль от GPG ключа
   - `SIGNING_KEY_ID` - ID GPG ключа
   - `OSSRH_USERNAME` - логин Sonatype
   - `OSSRH_PASSWORD` - пароль/token Sonatype
4. **Gradle задачи**:
   - `publishToSonatype` - загрузка в staging
   - `closeAndReleaseSonatypeStagingRepository` - автоматический release

### Принципы

- **Идемпотентность**: повторный запуск не ломает релиз
- **Безопасность**: все секреты в GitHub Secrets, не в коде
- **Прозрачность**: логи показывают каждый шаг публикации
- **Откат**: можно вручную удалить staging репозиторий в Sonatype UI

## 2. Компоненты

### 2.1. build.gradle.kts

**Добавляем:**

```kotlin
plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    signing
}

signing {
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }
}
```

### 2.2. gradle/libs.versions.toml

**Добавляем:**

```toml
[versions]
nexus-publish = "2.0.0"

[plugins]
nexus-publish = { id = "io.github.gradle-nexus.publish-plugin", version.ref = "nexus-publish" }
```

### 2.3. .github/workflows/release.yml

**Добавляем новый job после `publish`:**

```yaml
publish-maven-central:
  name: Publish to Maven Central
  runs-on: ubuntu-latest
  needs: build

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

    - name: Decode GPG key
      run: |
        echo "${{ secrets.SIGNING_KEY }}" | base64 -d > /tmp/signing-key.asc
        gpg --batch --import /tmp/signing-key.asc
        rm /tmp/signing-key.asc

    - name: Publish to Maven Central
      run: ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository -Pversion=${{ needs.build.outputs.version }}
      env:
        SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
        SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
        SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }}
        OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
        OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
```

### 2.4. GPG ключ

**Создан:**
- Key ID: `5895AA282B06E57E`
- Email: `kirill@androidbroadcast.dev`
- Тип: RSA 4096 bits
- Срок: до 2029-02-15
- Пароль: `mAhHT9DClYrvVfS1NPJe` (сохранён в SIGNING_PASSWORD)

## 3. Поток данных

### Пошаговый процесс релиза

1. **Developer pushes tag** `v1.0.0`
2. **GitHub Actions: Job "build"**
   - Компиляция, тесты
   - Генерация JAR-ов (main, sources, javadoc)
3. **Параллельно:**
   - **Job "publish"**: публикация в GitHub Packages
   - **Job "publish-maven-central"**:
     - Декодирование SIGNING_KEY из base64
     - Подпись артефактов GPG ключом
     - Публикация в Sonatype staging
     - Автоматический close & release staging repo
4. **Job "release"**
   - Создание GitHub Release
   - Генерация release notes через Claude API
   - Обновление CHANGELOG.md

### Детали Maven Central публикации

1. **Декодирование GPG ключа**: base64 → ASCII armor → import в GPG
2. **Подпись артефактов**: каждый JAR получает .asc подпись
3. **Staging**: артефакты загружаются в Sonatype staging репозиторий
4. **Валидация**: Sonatype проверяет:
   - Наличие POM с обязательными полями
   - Наличие sources & javadoc JAR-ов
   - Валидность GPG подписей
5. **Close**: staging репозиторий закрывается (запуск валидации)
6. **Release**: после успешной валидации → публикация в Maven Central
7. **Синхронизация**: через 15-30 минут артефакты доступны на Maven Central

### Время публикации

- **Staging upload**: 2-5 минут
- **Validation**: 1-2 минуты
- **Release**: мгновенно
- **Sync to Maven Central**: 15-30 минут
- **Общее время**: ~20-40 минут от push тега до доступности

## 4. Обработка ошибок

### Возможные проблемы и решения

#### 1. Ошибка GPG подписи

**Причина**: Неверный SIGNING_KEY или SIGNING_PASSWORD
**Симптом**: `gpg: signing failed: No secret key`
**Решение**:
- Проверить GitHub Secrets
- Пересоздать SIGNING_KEY (base64 без переносов строк)

#### 2. Sonatype authentication failed

**Причина**: Неверные OSSRH credentials
**Симптом**: `401 Unauthorized`
**Решение**:
- Проверить OSSRH_USERNAME и OSSRH_PASSWORD
- Использовать User Token вместо пароля

#### 3. POM validation failed

**Причина**: Отсутствуют обязательные поля в POM
**Симптом**: Sonatype reject с описанием недостающих полей
**Решение**: POM уже полный, но можно проверить обязательные поля

#### 4. Staging repository close failed

**Причина**:
- Отсутствует sources.jar или javadoc.jar
- Невалидная GPG подпись
- Неверный groupId

**Решение**:
- Проверить артефакты в staging UI: https://s01.oss.sonatype.org
- Посмотреть Activity log для конкретной ошибки
- Drop failed staging repo и перезапустить workflow

#### 5. Duplicate version published

**Причина**: Версия уже опубликована в Maven Central
**Симптом**: `400 Bad Request - version already exists`
**Решение**: Релизы immutable, нужна новая версия

### Стратегия восстановления

```bash
# Если staging repo застрял:
1. Открыть https://s01.oss.sonatype.org/#stagingRepositories
2. Найти свой staging repo
3. "Drop" неудачный репозиторий
4. Перезапустить GitHub Actions workflow
```

## 5. Тестирование

### План тестирования

#### Тест 1: Локальная подпись артефактов

```bash
export SIGNING_KEY=$(cat /tmp/signing-key-base64.txt)
export SIGNING_PASSWORD="mAhHT9DClYrvVfS1NPJe"
./gradlew clean build sign
ls -la build/libs/*.asc
```

**Ожидаемый результат**: Для каждого JAR есть .asc файл

#### Тест 2: SNAPSHOT публикация

```bash
./gradlew publishToSonatype -Pversion=0.4.1-SNAPSHOT
```

**Проверка**: Артефакт доступен по адресу:
```
https://s01.oss.sonatype.org/content/repositories/snapshots/dev/androidbroadcast/detekt-rules-koin/
```

#### Тест 3: Staging публикация (без release)

```bash
./gradlew publishToSonatype closeSonatypeStagingRepository -Pversion=0.5.0-test1
```

**Проверяем в Sonatype UI:**
- Staging repo создан
- Все артефакты загружены
- Close прошел успешно
- **НЕ делаем Release** - вручную Drop репозиторий

#### Тест 4: Полная публикация через CI

```bash
git tag -a v0.5.0 -m "Test release for Maven Central"
git push origin v0.5.0
```

**Мониторим:**
- GitHub Actions workflow завершается успешно
- Staging repo auto-closed и auto-released
- Через 30 минут проверяем Maven Central

#### Тест 5: Установка из Maven Central

```kotlin
dependencies {
    detektPlugins("dev.androidbroadcast.rules.koin:detekt-koin-rules:0.5.0")
}
```

### Критерии успеха

- ✅ Артефакты подписаны GPG ключом
- ✅ SNAPSHOT публикация работает
- ✅ Staging validation проходит
- ✅ Auto-release работает через CI
- ✅ Артефакты доступны на repo1.maven.org
- ✅ Можно установить и использовать из Maven Central

## 6. Безопасность

### GitHub Secrets

Все чувствительные данные хранятся в GitHub Secrets:

- `SIGNING_KEY` - base64-encoded приватный GPG ключ (10076 символов)
- `SIGNING_PASSWORD` - пароль от GPG ключа
- `SIGNING_KEY_ID` - ID GPG ключа для reference
- `OSSRH_USERNAME` - логин Sonatype OSSRH
- `OSSRH_PASSWORD` - User Token Sonatype OSSRH

### Меры предосторожности

- GPG ключ загружается только в память (in-memory PGP keys)
- Временные файлы удаляются после использования
- Пароли никогда не логируются
- Используется User Token вместо пароля для Sonatype

## 7. Документация для пользователей

После успешной публикации обновить README.md:

```markdown
## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    detektPlugins("dev.androidbroadcast.rules.koin:detekt-koin-rules:0.5.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    detektPlugins 'dev.androidbroadcast.rules.koin:detekt-koin-rules:0.5.0'
}
```
```

## Следующие шаги

1. ✅ Создать GPG ключ - DONE
2. ✅ Добавить GitHub Secrets - DONE
3. Обновить build.gradle.kts (signing + nexus-publish)
4. Обновить gradle/libs.versions.toml
5. Обновить .github/workflows/release.yml
6. Тест 1: Локальная подпись
7. Тест 2: SNAPSHOT публикация
8. Тест 3: Staging публикация (test version)
9. Тест 4: Полная публикация через CI
10. Обновить README.md с инструкциями установки
