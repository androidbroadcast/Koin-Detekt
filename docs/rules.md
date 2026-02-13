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

**Edge Cases:**
- ✅ Detects `get()`, `getOrNull()`, and `getAll()` variants
- ✅ Detects in init blocks and property initializers
- ✅ Detects in companion objects
- ✅ Allows `get()` inside `single {}`, `factory {}`, and other module definitions

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

**Edge Cases:**
- ✅ Detects both `inject()` and `injectOrNull()` delegates
- ✅ Detects in companion objects
- ✅ Detects multiple inject delegates in one class
- ✅ Detects lazy inject with type parameter: `by inject<Repository>()`

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

**Edge Cases:**
- ✅ Generic types: `class MyViewModel<T> : ViewModel(), KoinComponent`
- ✅ Fully qualified names: `androidx.activity.ComponentActivity`
- ✅ Multiple inheritance: `class A : Activity(), SomeInterface, KoinComponent`
- ✅ Companion objects implementing KoinComponent are detected
- ✅ Partial name matches don't count (NonActivity doesn't match Activity)

---

### NoGlobalContextAccess

**Severity:** Warning
**Active by default:** Yes

Detects direct `GlobalContext.get()` or `KoinPlatformTools` access.

❌ **Bad:**
```kotlin
val koin = GlobalContext.get()
```

**Edge Cases:**
- ✅ Detects all GlobalContext methods: `get()`, `getOrNull()`, `stopKoin()`, `getKoinApplicationOrNull()`
- ✅ Detects `KoinPlatformTools.defaultContext().get()`
- ✅ Detects in nested functions and lambdas
- ✅ Ignores similar-named classes that aren't the real GlobalContext
- ✅ Does not report `startKoin` usage (allowed)

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

**Edge Cases:**
- ✅ Detects both `get()` and `inject()` inside `startKoin` and `koinConfiguration` blocks
- ✅ Detects with qualifiers: `get<MyService>(named("special"))`
- ✅ Detects with parameters: `inject { parametersOf("param") }`
- ✅ Detects in nested lambdas inside startKoin
- ✅ Does not report `get()` in module definitions passed to `modules()`

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

**Edge Cases:**
- ✅ Whitespace-only modules: `module {   }`
- ✅ Comments-only modules: `module { /* TODO */ }`
- ✅ Value argument syntax: `module({ })`
- ✅ Does not report modules with `includes()` only

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

**Edge Cases:**
- ✅ Detects both `single {}` and `singleOf()`
- ✅ Supports regex patterns: `.*UseCase`, `.*Mapper`, `.*Command`, `.*Handler`
- ✅ Respects custom configuration patterns
- ✅ Default patterns include UseCase, Mapper, Interactor, Worker, Handler

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

**Edge Cases:**
- ✅ Detects all definition types: `single`, `factory`, `scoped`, `viewModel`, `worker`
- ✅ Detects mixed definition types for same type: `single { Repo() }` + `factory { Repo() }`
- ✅ Recognizes both `named()` and `qualifier()` functions
- ✅ Reports when one has qualifier but another doesn't
- ✅ Handles constructor references: `single(::ApiService)`
- ✅ Does not report duplicates across different modules
- ✅ Handles value argument syntax: `single(createdAtStart = true) { Service() }`

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

**Edge Cases:**
- ✅ Detects `checkModules()`, `koinNavViewModel()`, `stateViewModel()`
- ✅ Detects deprecated `viewModel {}` DSL (Koin 3.x style)
- ✅ Detects `getViewModel<T>()`
- ✅ Detects `application.koin` in Ktor (use `application.koinModules()` instead)
- ✅ Ignores non-application property accesses like `someOther.koin`

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

**Edge Cases:**
- ✅ Counts includes and definitions together
- ✅ Counts all definition types: `single`, `factory`, `scoped`, `viewModel`, `worker`
- ✅ Handles value argument syntax: `module({ includes(a, b) })`
- ✅ Ignores non-call statements like variable declarations
- ✅ Default threshold is 3 includes with definitions

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

**Edge Cases:**
- ✅ Detects `createScope()` and `getOrCreateScope()` without matching `close()`
- ✅ Detects scope creation in conditional blocks and nested classes
- ✅ Recognizes safe qualified calls: `koin?.createScope()` and `scope?.close()`
- ✅ Recognizes nested property close: `myObject.scope.close()`
- ✅ Reports class-level detection (multiple scopes = one finding per class)

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

**Edge Cases:**
- ✅ Recognizes `scope<T> {}`, `activityScope {}`, and `fragmentScope {}` blocks
- ✅ Detects `scoped {}` at module level (outside any scope block)
- ✅ Allows `scoped {}` inside any recognized scope block

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

**Edge Cases:**
- ✅ Detects both `factory {}` and `factoryOf()`
- ✅ Detects inside `scope<T> {}` and `activityScope {}` blocks
- ✅ Does not report factory outside scope blocks
- ✅ Does not report `scoped {}` inside scope blocks (allowed)

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

**Edge Cases:**
- ✅ Detects both `single {}` and `singleOf()` inside `requestScope {}`
- ✅ Detects with qualifiers: `single(named("logger")) { ... }`
- ✅ Detects multiple violations and nested requestScope blocks
- ✅ Allows `scoped {}`, `factory {}`, `viewModel {}`, and `worker {}` inside requestScope
- ✅ Detects single in lambdas inside requestScope: `forEach { single { ... } }`
