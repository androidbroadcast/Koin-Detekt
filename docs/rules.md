# Koin Rules Documentation

Complete reference for all 14 Detekt rules for Koin.

---

## Service Locator Rules

### NoGetOutsideModuleDefinition

**Severity:** Warning
**Active by default:** Yes

Detects `get()` / `getOrNull()` / `getAll()` calls outside Koin module definition blocks.

❌ **Bad:**
```kotlin
class MyRepository : KoinComponent {
    val api = get<ApiService>()
}
```

✅ **Good:**
```kotlin
module {
    single { MyRepository(get()) }
}
```

---

### NoInjectDelegate

**Severity:** Warning
**Active by default:** Yes

Detects `by inject()` property delegate usage.

❌ **Bad:**
```kotlin
class MyService : KoinComponent {
    val repo: Repository by inject()
}
```

✅ **Good:**
```kotlin
class MyService(private val repo: Repository)
```

---

### NoKoinComponentInterface

**Severity:** Warning
**Active by default:** Yes
**Configurable:** Yes

Detects `KoinComponent` / `KoinScopeComponent` in non-framework classes.

❌ **Bad:**
```kotlin
class UserRepository : KoinComponent
```

✅ **Good:**
```kotlin
class MainActivity : Activity(), KoinComponent
```

**Configuration:**
```yaml
NoKoinComponentInterface:
  allowedSuperTypes:
    - 'Application'
    - 'ViewModel'
```

---

### NoGlobalContextAccess

**Severity:** Warning
**Active by default:** Yes

Detects direct `GlobalContext.get()` or `KoinPlatformTools` access.

❌ **Bad:**
```kotlin
val koin = GlobalContext.get()
```

---

### NoKoinGetInApplication

**Severity:** Warning
**Active by default:** Yes

Detects `get()` / `inject()` inside `startKoin {}` blocks.

❌ **Bad:**
```kotlin
startKoin {
    val service = get<MyService>()
}
```

---

## Module DSL Rules

### EmptyModule

**Severity:** Warning
**Active by default:** Yes

Detects modules without definitions or includes.

❌ **Bad:**
```kotlin
val emptyModule = module { }
```

---

### SingleForNonSharedDependency

**Severity:** Warning
**Active by default:** Yes
**Configurable:** Yes

Detects `single {}` for types that shouldn't be singletons.

❌ **Bad:**
```kotlin
module {
    single { GetUserUseCase(get()) }
}
```

✅ **Good:**
```kotlin
module {
    factory { GetUserUseCase(get()) }
}
```

**Configuration:**
```yaml
SingleForNonSharedDependency:
  namePatterns:
    - '.*UseCase'
    - '.*Command'
```

---

### MissingScopedDependencyQualifier

**Severity:** Warning
**Active by default:** Yes

Detects duplicate type definitions without qualifiers.

❌ **Bad:**
```kotlin
module {
    single { HttpClient(CIO) }
    single { HttpClient(OkHttp) }
}
```

✅ **Good:**
```kotlin
module {
    single(named("cio")) { HttpClient(CIO) }
    single(named("okhttp")) { HttpClient(OkHttp) }
}
```

---

### DeprecatedKoinApi

**Severity:** Warning
**Active by default:** Yes

Detects deprecated Koin 4.x APIs.

| Deprecated | Replacement |
|------------|-------------|
| `checkModules()` | `verify()` |
| `koinNavViewModel()` | `koinViewModel()` |
| `stateViewModel()` | `viewModel()` |

---

### ModuleIncludesOrganization

**Severity:** Style
**Active by default:** No
**Configurable:** Yes

Detects "God Modules" with many includes + definitions.

**Configuration:**
```yaml
ModuleIncludesOrganization:
  active: true
  maxIncludesWithDefinitions: 3
```

---

## Scope Management Rules

### MissingScopeClose

**Severity:** Warning
**Active by default:** Yes

Detects scopes without `close()` calls (memory leak).

❌ **Bad:**
```kotlin
class SessionManager : KoinComponent {
    val scope = getKoin().createScope("session")
}
```

✅ **Good:**
```kotlin
class SessionManager : KoinComponent {
    val scope = getKoin().createScope("session")
    fun destroy() { scope.close() }
}
```

---

### ScopedDependencyOutsideScopeBlock

**Severity:** Warning
**Active by default:** Yes

Detects `scoped {}` outside `scope {}` blocks.

❌ **Bad:**
```kotlin
module {
    scoped { UserSession() }
}
```

✅ **Good:**
```kotlin
module {
    scope<MainActivity> {
        scoped { UserSession() }
    }
}
```

---

### FactoryInScopeBlock

**Severity:** Style
**Active by default:** No

Detects `factory {}` inside `scope {}` blocks (possibly unintended).

⚠️ **Warning:**
```kotlin
module {
    scope<MyActivity> {
        factory { Presenter() }
    }
}
```

---

### KtorRequestScopeMisuse

**Severity:** Warning
**Active by default:** Yes

Detects `single {}` inside `requestScope {}` (Ktor).

❌ **Bad:**
```kotlin
module {
    requestScope {
        single { RequestLogger() }
    }
}
```

✅ **Good:**
```kotlin
module {
    requestScope {
        scoped { RequestLogger() }
    }
}
```
