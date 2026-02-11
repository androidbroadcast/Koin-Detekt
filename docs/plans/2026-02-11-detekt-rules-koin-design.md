# detekt-rules-koin — Design Document

## Overview

Detekt 1.x extension library with a set of rules for Koin 4.x to enforce best practices
and catch common anti-patterns via static analysis.

**No existing analogs** — there are no Detekt plugins for Koin on GitHub or Detekt Marketplace.

### Key Parameters

| Parameter | Value |
|-----------|-------|
| Detekt | 1.23.8 (latest stable 1.x) |
| Koin | 4.x (all platforms: Core, Android, Compose, Ktor) |
| Analysis | Syntactic only (Kotlin PSI), no Koin runtime dependency |
| License | Apache 2.0 |
| Distribution | Open-source, Maven Central, Detekt Marketplace |

---

## Project Structure

Single Gradle module. Rules organized by packages:

```
detekt-rules-koin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── LICENSE
├── src/
│   ├── main/
│   │   ├── kotlin/io/github/<user>/detekt/koin/
│   │   │   ├── KoinRuleSetProvider.kt
│   │   │   ├── servicelocator/
│   │   │   ├── moduledsl/
│   │   │   ├── scope/
│   │   │   └── util/
│   │   └── resources/
│   │       ├── config/config.yml
│   │       └── META-INF/services/
│   │           └── io.gitlab.arturbosch.detekt.api.RuleSetProvider
│   └── test/
│       └── kotlin/io/github/<user>/detekt/koin/
│           ├── servicelocator/
│           ├── moduledsl/
│           └── scope/
```

### Dependencies

```kotlin
compileOnly("io.gitlab.arturbosch.detekt:detekt-api:1.23.8")
testImplementation("io.gitlab.arturbosch.detekt:detekt-test:1.23.8")
testImplementation("org.assertj:assertj-core:3.27.3")
testImplementation(kotlin("test"))
```

No Koin dependencies — analysis is purely syntactic via Kotlin PSI.

---

## Rules

### Category 1: Service Locator (5 rules)

#### Rule 1: `NoGetOutsideModuleDefinition`

Detects `get()` / `getOrNull()` / `getAll()` calls outside module definition lambdas
(`single {}`, `factory {}`, `scoped {}`, `viewModel {}`, `worker {}`).

```kotlin
// BAD
class MyRepository : KoinComponent {
    val api = get<ApiService>()
}

// OK
module {
    single { MyRepository(get()) }
    factoryOf(::MyUseCase)
}
```

**Implementation**: Visitor tracks nesting — sets `insideDefinitionBlock = true` when
entering `single {}` / `factory {}` / `scoped {}` / `viewModel {}` / `worker {}`.
`get()` outside this context is a finding.

---

#### Rule 2: `NoInjectDelegate`

Detects `by inject()` property delegate usage. Always a service locator pattern —
constructor injection is preferred.

```kotlin
// BAD
class MyService : KoinComponent {
    val repo: Repository by inject()
}

// GOOD
class MyService(private val repo: Repository)
```

**Implementation**: Finds property delegate expressions where the called function is `inject`.

---

#### Rule 3: `NoKoinComponentInterface`

Detects `KoinComponent` / `KoinScopeComponent` implementation in classes that are not
framework entry points. Configurable list of allowed super types.

```kotlin
// BAD
class UserRepository : KoinComponent { ... }

// OK (configured)
class MainActivity : AppCompatActivity(), KoinComponent { ... }
```

**Configuration**:
```yaml
NoKoinComponentInterface:
  active: true
  allowedSuperTypes:
    - 'Application'
    - 'Activity'
    - 'Fragment'
    - 'Service'
    - 'BroadcastReceiver'
    - 'ViewModel'
```

---

#### Rule 4: `NoGlobalContextAccess`

Detects direct access to `GlobalContext.get()`, `GlobalContext.getKoinApplicationOrNull()`,
`KoinPlatformTools` — the most egregious service locator variant.

```kotlin
// BAD
val koin = GlobalContext.get()
val service = koin.get<MyService>()
```

---

#### Rule 5: `NoKoinGetInApplication`

Detects `get()` / `inject()` calls inside `startKoin {}` or `koinConfiguration {}` blocks
for obtaining dependencies instead of defining modules.

---

### Category 2: Module DSL (5 rules)

#### Rule 6: `EmptyModule`

Detects modules without any definitions or `includes()`.

```kotlin
// BAD
val emptyModule = module { }

// OK
val featureModule = module {
    includes(networkModule)
}
```

---

#### Rule 7: `SingleForNonSharedDependency`

Detects `single {}` / `singleOf()` for types that should not be singletons by naming
convention (UseCase, Interactor, Mapper, etc.).

```kotlin
// BAD
module {
    single { GetUserUseCase(get()) }
    singleOf(::ValidateEmailUseCase)
}

// GOOD
module {
    factory { GetUserUseCase(get()) }
    factoryOf(::ValidateEmailUseCase)
}
```

**Configuration**:
```yaml
SingleForNonSharedDependency:
  active: true
  namePatterns:
    - '.*UseCase'
    - '.*Interactor'
    - '.*Mapper'
```

---

#### Rule 8: `MissingScopedDependencyQualifier`

Detects multiple definitions of the same type in one module without `named()` qualifier.
Leads to runtime `DefinitionOverrideException`.

```kotlin
// BAD
module {
    single { HttpClient(CIO) }
    single { HttpClient(OkHttp) }
}

// GOOD
module {
    single(named("cio")) { HttpClient(CIO) }
    single(named("okhttp")) { HttpClient(OkHttp) }
}
```

Severity: warning (limited type resolution without compiler, possible false positives).

---

#### Rule 9: `DeprecatedKoinApi`

Detects usage of APIs deprecated in Koin 4.x with suggested replacements.

| Deprecated | Replacement |
|-----------|-------------|
| `checkModules()` | `verify()` |
| `KoinContext {}` | `KoinApplication {}` |
| `KoinAndroidContext {}` | `KoinApplication {}` |
| `koinNavViewModel()` | `koinViewModel()` |
| `stateViewModel()` | `viewModel()` |
| `Application.koin` (Ktor) | `Application.koinModules()` |

---

#### Rule 10: `ModuleIncludesOrganization`

Detects modules that mix `includes()` with direct definitions when includes count exceeds
a threshold — sign of a "God Module".

```kotlin
// WARNING
val appModule = module {
    includes(networkModule, dbModule, featureAModule, featureBModule)
    single { AppConfig() }
    factory { Logger() }
}

// BETTER
val coreModule = module {
    single { AppConfig() }
    factory { Logger() }
}
val appModule = module {
    includes(coreModule, networkModule, dbModule, featureAModule, featureBModule)
}
```

**Configuration**:
```yaml
ModuleIncludesOrganization:
  active: false
  maxIncludesWithDefinitions: 3
```

---

### Category 3: Scope Management (4 rules)

#### Rule 11: `MissingScopeClose`

Detects classes that create or obtain a `Scope` (`createScope()`, `getOrCreateScope()`)
but never call `scope.close()`.

```kotlin
// BAD — memory leak
class SessionManager : KoinComponent {
    val scope = getKoin().createScope("session", named("session"))
    fun getService() = scope.get<SessionService>()
}

// GOOD
class SessionManager : KoinComponent {
    val scope = getKoin().createScope("session", named("session"))
    fun getService() = scope.get<SessionService>()
    fun destroy() { scope.close() }
}
```

Severity: warning (heuristic — possible false positives).

---

#### Rule 12: `ScopedDependencyOutsideScopeBlock`

Detects `scoped {}` / `scopedOf()` outside a `scope {}` / `activityScope {}` /
`fragmentScope {}` / `viewModelScope {}` / `requestScope {}` block.

```kotlin
// BAD
module {
    scoped { UserSession() }
}

// GOOD
module {
    scope<MainActivity> {
        scoped { UserSession() }
    }
}
```

---

#### Rule 13: `FactoryInScopeBlock`

Detects `factory {}` / `factoryOf()` inside `scope {}` blocks — possibly unintended,
since factory creates a new instance on every call regardless of scope.

```kotlin
// WARNING
module {
    scope<MyActivity> {
        factory { Presenter(get()) }
    }
}
```

**Configuration**:
```yaml
FactoryInScopeBlock:
  active: false
```

---

#### Rule 14: `KtorRequestScopeMisuse`

Detects `single {}` / `singleOf()` inside `requestScope {}` in Ktor.
Singleton in a request scope is semantically incorrect.

```kotlin
// BAD
module {
    requestScope {
        single { RequestLogger() }
    }
}

// GOOD
module {
    requestScope {
        scoped { RequestLogger() }
    }
}
```

---

## Default Configuration

```yaml
koin-rules:
  # Service Locator
  NoGetOutsideModuleDefinition:
    active: true
  NoInjectDelegate:
    active: true
  NoKoinComponentInterface:
    active: true
    allowedSuperTypes:
      - 'Application'
      - 'Activity'
      - 'Fragment'
      - 'Service'
      - 'BroadcastReceiver'
      - 'ViewModel'
  NoGlobalContextAccess:
    active: true
  NoKoinGetInApplication:
    active: true

  # Module DSL
  EmptyModule:
    active: true
  SingleForNonSharedDependency:
    active: true
    namePatterns:
      - '.*UseCase'
      - '.*Interactor'
      - '.*Mapper'
  MissingScopedDependencyQualifier:
    active: true
  DeprecatedKoinApi:
    active: true
  ModuleIncludesOrganization:
    active: false
    maxIncludesWithDefinitions: 3

  # Scope Management
  MissingScopeClose:
    active: true
  ScopedDependencyOutsideScopeBlock:
    active: true
  FactoryInScopeBlock:
    active: false
  KtorRequestScopeMisuse:
    active: true
```

---

## Testing Strategy

Each rule tested via `detekt-test` API:
- 1-2 positive cases (code with violation -> finding)
- 1-2 negative cases (correct code -> no findings)
- 1 edge case (nested lambdas, call chains)
- 1 configuration test (if rule is configurable)

Minimum ~60 tests for 14 rules.

---

## Build & Publishing

- **Kotlin**: 2.0.21 (compatible with Detekt 1.23.8)
- **Publishing**: Maven Central via `io.github.gradle-nexus.publish-plugin`
- **CI**: GitHub Actions — build + test on every PR, publish on GitHub Release tag

### Consumer Usage

```kotlin
dependencies {
    detektPlugins("io.github.<user>:detekt-rules-koin:0.1.0")
}
```

---

## Summary

| Category | Rules | Default Active |
|----------|-------|---------------|
| Service Locator | 5 | 5 |
| Module DSL | 5 | 4 |
| Scope Management | 4 | 3 |
| **Total** | **14** | **12** |

---

## Roadmap

| Version | Scope |
|---------|-------|
| 0.1.0 | Initial release — 14 rules across 3 categories |
| 0.2.0 | Architecture rules (Koin imports in domain layer) |
| 0.3.0 | Koin Annotations rules (`@Single` / `@Factory` / `@Scoped` validation) |
| 1.0.0 | API stabilization, Detekt 2.x compatibility |
