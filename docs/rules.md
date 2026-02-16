# Koin Rules Documentation

Complete reference for all 51 Detekt rules for Koin.

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

### UnassignedQualifierInWithOptions

**Severity:** Warning
**Active by default:** Yes

Detects `named()` calls in `withOptions {}` without assignment to `qualifier` property.

❌ **Bad:**
```kotlin
module {
    factory { Service() } withOptions {
        named("myService")  // Dead code - qualifier not applied
    }
}
```

✅ **Good:**
```kotlin
module {
    factory { Service() } withOptions {
        qualifier = named("myService")
    }
}
```

**Edge Cases:**
- ✅ Detects both `named()` and `qualifier()` calls
- ✅ Does not report when assigned: `qualifier = named("x")`
- ✅ Does not report other `withOptions` properties like `createdAtStart`

**Related Issue:** [Koin#2331](https://github.com/InsertKoinIO/koin/issues/2331)

---

### DuplicateBindingWithoutQualifier

**Severity:** Warning
**Active by default:** Yes

Detects multiple bindings to same type without qualifiers (silent override).

❌ **Bad:**
```kotlin
module {
    single { ServiceA() } bind Foo::class
    single { ServiceB() } bind Foo::class  // Silently overrides ServiceA
}
```

✅ **Good:**
```kotlin
module {
    single { ServiceA() } bind Foo::class named("a")
    single { ServiceB() } bind Foo::class named("b")
}
```

**Edge Cases:**
- ✅ Detects duplicates across different definition types (`single`, `factory`, `scoped`)
- ✅ Does not report when bindings have qualifiers
- ✅ Does not report single binding without qualifier
- ✅ Does not report bindings to different types
- ✅ Only reports within same module scope

**Related Issue:** [Koin#2115](https://github.com/InsertKoinIO/koin/issues/2115)

---

### GenericDefinitionWithoutQualifier

**Severity:** Warning
**Active by default:** Yes

Detects generic types without qualifiers causing type erasure collisions.

❌ **Bad:**
```kotlin
module {
    single { listOf<String>() }  // Type erased to List
    single { listOf<Int>() }     // Overwrites previous List
}
```

✅ **Good:**
```kotlin
module {
    single(named("strings")) { listOf<String>() }
    single(named("ints")) { listOf<Int>() }
}
```

**Edge Cases:**
- ✅ Detects `List`, `Set`, `Map`, `Array` and their factory functions
- ✅ Detects `listOf`, `setOf`, `mapOf`, `arrayOf` with type parameters
- ✅ Works with `single`, `factory`, and `scoped`
- ✅ Does not report when qualifier is present
- ✅ Does not report non-generic types

**Related Issue:** [Koin#188](https://github.com/InsertKoinIO/koin/issues/188)

---

### EnumQualifierCollision

**Severity:** Warning
**Active by default:** Yes

Detects enum qualifiers with same value name from different enum types (collision risk with R8/ProGuard).

❌ **Bad:**
```kotlin
enum class Type1 { VALUE }
enum class Type2 { VALUE }

module {
    single(named(Type1.VALUE)) { ServiceA() }
    single(named(Type2.VALUE)) { ServiceB() }  // Collision: both use "VALUE"
}
```

✅ **Good:**
```kotlin
module {
    single(named("type1_value")) { ServiceA() }
    single(named("type2_value")) { ServiceB() }
}
```

**Edge Cases:**
- ✅ Detects collisions across different enum types
- ✅ Does not report same enum value used multiple times
- ✅ Does not report enum values with different names
- ✅ Does not report string qualifiers
- ✅ Uses heuristic pattern matching (no semantic analysis required)

**Related Issue:** [Koin#2364](https://github.com/InsertKoinIO/koin/issues/2364)

---

### ConstructorDslAmbiguousParameters

**Severity:** Warning
**Active by default:** Yes

Detects `factoryOf(::)` / `singleOf(::)` / `viewModelOf(::)` with duplicate parameter types.

❌ **Bad:**
```kotlin
class MyService(val a: Int, val b: Int)

val m = module {
    factoryOf(::MyService)  // b gets value of a
}
```

✅ **Good:**
```kotlin
val m = module {
    factory { MyService(get(), get()) }
}
```

**Why this matters:**
Koin's constructor DSL incorrectly resolves parameters of the same type. Use lambda syntax for explicit parameter resolution.

**Edge Cases:**
- ✅ Detects factoryOf, singleOf, and viewModelOf
- ✅ Detects duplicate types including nullable variants (Int and Int? are treated as duplicates)
- ✅ Reports all duplicate types in the message
- ✅ Does not report when all parameter types are different
- ✅ Does not report lambda-based factories

**Related Issues:** [Koin#1372](https://github.com/InsertKoinIO/koin/issues/1372), [Koin#2347](https://github.com/InsertKoinIO/koin/issues/2347)

---

### ParameterTypeMatchesReturnType

**Severity:** Warning
**Active by default:** Yes

Detects factory/single/scoped definitions where the return type matches a parameter type.

❌ **Bad:**
```kotlin
factory<Int>(named("random")) { limit: Int ->
    Random.nextInt(limit)  // Never executes - returns `limit`
}
```

✅ **Good:**
```kotlin
factory(named("random")) { params ->
    val limit = params.get<Int>()
    Random.nextInt(limit)
}
```

**Why this matters:**
Koin has undocumented short-circuit behavior: when parametersOf() provides a value matching the definition's return type, Koin returns that value directly without executing the lambda.

**Edge Cases:**
- ✅ Detects factory, single, and scoped definitions
- ✅ Normalizes nullable types (Int and Int? are treated as same type)
- ✅ Does not report definitions without explicit type arguments
- ✅ Does not report when parameter type differs from return type

**Related Issue:** [Koin#2328](https://github.com/InsertKoinIO/koin/issues/2328)

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

### ViewModelAsSingleton

**Severity:** Warning
**Active by default:** Yes

Detects ViewModel classes defined with `single {}` instead of `viewModel {}`.

❌ **Bad:**
```kotlin
class MyViewModel : ViewModel()

val appModule = module {
    single { MyViewModel() }
}
```

✅ **Good:**
```kotlin
class MyViewModel : ViewModel()

val appModule = module {
    viewModel { MyViewModel() }
}
```

**Why this matters:**
When a ViewModel is defined as a singleton, its `viewModelScope` becomes invalid after `popBackStack()`, causing coroutine launches to fail silently. Use `viewModel {}` to ensure proper lifecycle management.

**Edge Cases:**
- ✅ Detects both `single { }` and `singleOf(::)` patterns
- ✅ Works with custom ViewModel subclasses
- ✅ Allows `viewModel { }` and `viewModelOf(::)` (correct usage)

---

### CloseableWithoutOnClose

**Severity:** Warning
**Active by default:** Yes

Detects Closeable/AutoCloseable types in `single {}` or `scoped {}` blocks without `onClose` callback.

❌ **Bad:**
```kotlin
class DatabaseConnection : Closeable {
    override fun close() { /* cleanup */ }
}

val appModule = module {
    single { DatabaseConnection() }
}
```

✅ **Good:**
```kotlin
val appModule = module {
    single { DatabaseConnection() } onClose { it?.close() }
}
```

**Why this matters:**
Resources implementing Closeable are not automatically cleaned up by Koin. Without `onClose`, connections remain open, causing resource leaks.

---

### ScopeAccessInOnDestroy

**Severity:** Warning
**Active by default:** Yes

Detects scope access (`get()`, `inject()`) in `onDestroy()` or `onDestroyView()` methods.

❌ **Bad:**
```kotlin
class MyFragment : Fragment(), KoinComponent {
    override fun onDestroy() {
        val service = get<MyService>()
        service.cleanup()
    }
}
```

✅ **Good:**
```kotlin
class MyFragment : Fragment(), KoinComponent {
    private val service: MyService by inject()

    override fun onDestroy() {
        service.cleanup()
    }
}
```

**Why this matters:**
In nested fragments or ViewPager2, the scope may be closed before `onDestroy()` executes, causing `ClosedScopeException`. Access dependencies in `onCreate()` or as class properties instead.

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

---

## Platform Rules

### Compose Rules

#### KoinViewModelOutsideComposable

**Severity:** Warning
**Active by default:** Yes

Detects `koinViewModel()` calls outside `@Composable` functions.

❌ **Bad:**
```kotlin
fun MyScreen() {
    val vm = koinViewModel<MyViewModel>() // Runtime crash!
}
```

✅ **Good:**
```kotlin
@Composable
fun MyScreen() {
    val vm = koinViewModel<MyViewModel>()
}
```

**Edge Cases:**
- ✅ Detects in init blocks and property initializers
- ✅ Works with inline Composable functions
- ✅ Handles qualified koinViewModel calls with parameters

---

#### KoinInjectInPreview

**Severity:** Warning
**Active by default:** Yes

Detects `koinInject()` in `@Preview` functions.

❌ **Bad:**
```kotlin
@Preview
@Composable
fun MyScreenPreview() {
    val repo = koinInject<Repository>() // Preview crash!
    MyScreen(repo)
}
```

✅ **Good:**
```kotlin
@Preview
@Composable
fun MyScreenPreview() {
    MyScreen(FakeRepository())
}
```

**Edge Cases:**
- ✅ Detects koinInject in any Preview-annotated function
- ✅ Regular Composable functions without @Preview are allowed

---

#### RememberKoinModulesLeak

**Severity:** Warning
**Active by default:** Yes

Detects `loadKoinModules()` inside `remember {}` without corresponding unload.

❌ **Bad:**
```kotlin
@Composable
fun FeatureScreen() {
    remember { loadKoinModules(featureModule) } // Memory leak!
}
```

✅ **Good:**
```kotlin
@Composable
fun FeatureScreen() {
    DisposableEffect(Unit) {
        loadKoinModules(featureModule)
        onDispose { unloadKoinModules(featureModule) }
    }
}
```

**Edge Cases:**
- ✅ Detects loadKoinModules specifically inside remember blocks
- ✅ DisposableEffect with onDispose is the recommended pattern

---

### Ktor Rules

#### KtorApplicationKoinInit

**Severity:** Warning
**Active by default:** Yes

Detects `install(Koin)` in routing blocks or route handlers.

❌ **Bad:**
```kotlin
fun Application.module() {
    routing {
        install(Koin) { } // Wrong place!
        get("/api") { }
    }
}
```

✅ **Good:**
```kotlin
fun Application.module() {
    install(Koin) { }
    routing {
        get("/api") { }
    }
}
```

**Edge Cases:**
- ✅ Detects install(Koin) at any depth inside routing blocks
- ✅ Koin should be initialized once at Application level

---

#### KtorRouteScopeMisuse

**Severity:** Warning
**Active by default:** Yes

Detects shared `koinScope()` across HTTP requests.

❌ **Bad:**
```kotlin
val sharedScope = koinScope() // Shared across requests!
get("/api") {
    val service = sharedScope.get<Service>()
}
```

✅ **Good:**
```kotlin
get("/api") {
    call.koinScope().get<Service>() // Request-scoped
}
```

**Edge Cases:**
- ✅ Detects koinScope stored in properties outside route handlers
- ✅ call.koinScope() is the correct request-scoped pattern

---

### Android Rules

#### AndroidContextNotFromKoin

**Severity:** Warning
**Active by default:** Yes

Detects `androidContext()` / `androidApplication()` called outside `startKoin`.

❌ **Bad:**
```kotlin
val myModule = module {
    single { androidContext() } // Wrong context!
}
```

✅ **Good:**
```kotlin
class MyApp : Application() {
    override fun onCreate() {
        startKoin {
            androidContext(this@MyApp)
            modules(appModule)
        }
    }
}
```

**Edge Cases:**
- ✅ Detects both androidContext() and androidApplication()
- ✅ These should only be called once in startKoin block

---

#### ActivityFragmentKoinScope

**Severity:** Warning
**Active by default:** Yes

Detects misuse of `activityScope()` / `fragmentScope()`.

❌ **Bad:**
```kotlin
class MyFragment : Fragment() {
    val vm by activityScope().inject<MyViewModel>() // Wrong scope!
}
```

✅ **Good:**
```kotlin
class MyFragment : Fragment() {
    val vm by fragmentScope().inject<MyViewModel>()
}
```

**Edge Cases:**
- ✅ Scopes must match component lifecycle
- ✅ Detects activityScope in Fragment and fragmentScope in Activity
- ✅ Prevents memory leaks from lifecycle mismatches

---

## Architecture Rules

### LayerBoundaryViolation

**Severity:** Warning
**Active by default:** No (requires configuration)
**Configurable:** Yes

Enforces Clean Architecture by restricting Koin imports in specified layers.

**Configuration:**
```yaml
LayerBoundaryViolation:
  active: true
  restrictedLayers:
    - 'com.example.domain'
    - 'com.example.core'
  allowedImports:
    - 'org.koin.core.qualifier.Qualifier'
```

❌ **Bad:**
```kotlin
package com.example.domain
import org.koin.core.component.get

class UseCase {
    val repo = get<Repository>() // Violates Clean Architecture!
}
```

✅ **Good:**
```kotlin
package com.example.domain

class UseCase(
    private val repo: Repository // Constructor injection
)
```

**Edge Cases:**
- ✅ Only active when restrictedLayers is configured
- ✅ Supports allowedImports whitelist for specific APIs
- ✅ Detects all org.koin.* imports in restricted packages
- ✅ Star imports are always violations in restricted layers

---

### PlatformImportRestriction

**Severity:** Warning
**Active by default:** No (requires configuration)
**Configurable:** Yes

Restricts platform-specific Koin imports to appropriate modules.

**Configuration:**
```yaml
PlatformImportRestriction:
  active: true
  restrictions:
    - import: 'org.koin.android.*'
      allowedPackages: ['com.example.app']
    - import: 'org.koin.compose.*'
      allowedPackages: ['com.example.ui']
```

❌ **Bad:**
```kotlin
package com.example.shared
import org.koin.android.ext.koin.androidContext // Wrong module!
```

✅ **Good:**
```kotlin
package com.example.app
import org.koin.android.ext.koin.androidContext
```

**Edge Cases:**
- ✅ Only active when restrictions are configured
- ✅ Supports wildcard patterns (org.koin.android.*)
- ✅ Multiple restrictions can be defined
- ✅ Prevents accidental platform dependencies in shared code

---

### CircularModuleDependency

**Severity:** Warning
**Active by default:** Yes

Detects circular dependencies between Koin modules via `includes()`.

❌ **Bad:**
```kotlin
val moduleA = module {
    includes(moduleB)
}

val moduleB = module {
    includes(moduleA) // Circular!
}
```

✅ **Good:**
```kotlin
val coreModule = module { }

val featureModule = module {
    includes(coreModule)
}
```

**Edge Cases:**
- ✅ Detects self-referencing modules
- ✅ Detects direct circular dependencies (A -> B -> A)
- ✅ Analyzes module dependencies within a single file
- ✅ Ensures clean module hierarchy

---

## Koin Annotations Rules

### MixingDslAndAnnotations

**Severity:** Warning
**Active by default:** Yes

Detects mixing DSL (`module {}`) and Annotations (`@Module`, `@Single`) approaches in the same file.

Mixing both approaches in the same file creates inconsistency and makes code harder to maintain. Choose one approach per file for clarity.

❌ **Bad:**
```kotlin
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.dsl.module

@Module
class AnnotatedModule {
    @Single
    fun provideRepo(): Repository = RepositoryImpl()
}

val dslModule = module {
    single { ApiService() }
}
```

✅ **Good:**
```kotlin
// Either all DSL:
val module = module {
    single { Repository() }
    single { ApiService() }
}

// Or all Annotations:
@Module
class MyModule {
    @Single
    fun provideRepo(): Repository = RepositoryImpl()
    @Single
    fun provideApi(): ApiService = ApiServiceImpl()
}
```

**Edge Cases:**
- ✅ Detects `module {}` DSL calls
- ✅ Detects `@Module`, `@Single`, `@Factory` annotations
- ✅ Reports at file level when both approaches are mixed
- ✅ Files with only DSL or only Annotations pass without issues
- ✅ Detection works across different declarations in the same file

---

### MissingModuleAnnotation

**Severity:** Warning
**Active by default:** Yes

Detects classes with `@Single`, `@Factory`, or `@Scoped` annotations but missing the `@Module` annotation.

The Koin annotation processor requires `@Module` on the class to discover the definitions inside. Without it, your definitions will be silently ignored at runtime.

❌ **Bad:**
```kotlin
import org.koin.core.annotation.Single

class MyServices {
    @Single
    fun provideRepo(): Repository = RepositoryImpl() // Won't be discovered!
}
```

✅ **Good:**
```kotlin
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class MyServices {
    @Single
    fun provideRepo(): Repository = RepositoryImpl()
}
```

**Edge Cases:**
- ✅ Detects `@Single`, `@Factory`, and `@Scoped` without `@Module`
- ✅ Reports on the class that's missing `@Module`
- ✅ Checks both function-level and property-level annotations
- ✅ Allows classes without Koin annotations (no false positives)
- ✅ Works with nested classes and companion objects

---

### ConflictingBindings

**Severity:** Warning
**Active by default:** Yes

Detects the same type being defined in both DSL (`module {}`) and Annotations (`@Single`, etc.).

When the same type is defined in both approaches, you create a runtime conflict. Depending on module loading order, one definition will override the other unpredictably.

❌ **Bad:**
```kotlin
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.dsl.module

@Module
class AnnotatedModule {
    @Single
    fun provideRepo(): Repository = RepositoryImpl()
}

val dslModule = module {
    single<Repository> { RepositoryImpl() } // Conflict!
}
```

✅ **Good:**
```kotlin
// Choose one approach per type:
@Module
class AnnotatedModule {
    @Single
    fun provideRepo(): Repository = RepositoryImpl()
}

// Different types are fine:
val dslModule = module {
    single { ApiService() }
}
```

**Edge Cases:**
- ✅ Detects type conflicts from function return types in annotations
- ✅ Detects type conflicts from type parameters in DSL (`single<Type>`)
- ✅ Compares simple type names (ignores packages)
- ✅ Reports conflicts for `single`, `factory`, and `scoped` definitions
- ✅ Allows the same type with different qualifiers (not detected as conflict)
- ✅ Analyzes entire file for conflicts

---

### ScopedWithoutQualifier

**Severity:** Warning
**Active by default:** Yes

Detects `@Scoped` annotations without explicit scope name or qualifier.

Using `@Scoped` without specifying which scope can lead to confusion about the actual scope lifecycle. Being explicit improves code clarity and prevents scope-related bugs.

❌ **Bad:**
```kotlin
import org.koin.core.annotation.Scoped

@Scoped
class MyService // Which scope?
```

✅ **Good:**
```kotlin
import org.koin.core.annotation.Scoped

@Scoped(name = "userScope")
class MyService
```

**Edge Cases:**
- ✅ Detects `@Scoped` without any parameters
- ✅ Allows `@Scoped` with `name` parameter
- ✅ Does not flag `@Single` or `@Factory` (they don't need qualifiers)
- ✅ Reports on the annotation itself
- ✅ Parameterless `@Scoped()` is also detected

---

### AnnotationProcessorNotConfigured

**Severity:** Warning
**Active by default:** Yes
**Configurable:** Yes

Warns when Koin annotations are used but the annotation processor (KSP/KAPT) may not be configured.

This rule provides informational warnings since Detekt cannot reliably detect if the annotation processor is configured in your build system. If you've already configured KSP/KAPT, you can disable this rule via configuration.

❌ **Bad:**
```kotlin
// build.gradle.kts missing KSP setup

import org.koin.core.annotation.Single

@Single
class MyService // Won't work without processor!
```

✅ **Good:**
```kotlin
// build.gradle.kts:
plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
}

dependencies {
    ksp("io.insert-koin:koin-ksp-compiler:2.0.0")
}

// MyService.kt:
import org.koin.core.annotation.Single

@Single
class MyService
```

**Configuration:**
```yaml
AnnotationProcessorNotConfigured:
  active: true
  skipCheck: false  # Set to true to disable if processor is configured
```

**Edge Cases:**
- ✅ Detects `@Single`, `@Factory`, `@Scoped`, `@Module`, `@KoinViewModel`, `@KoinWorker`, `@ComponentScan`, `@Configuration`, `@KoinApplication` annotations
- ✅ Reports on every class with Koin annotations (informational)
- ✅ Can be disabled globally via `skipCheck: true` configuration
- ✅ Useful for projects adopting Koin Annotations to ensure proper setup
- ✅ Does not check actual build configuration (limitation of static analysis)

---

### SingleAnnotationOnObject

**Severity:** Warning
**Active by default:** Yes

Detects Koin definition annotations (`@Single`, `@Factory`, etc.) on Kotlin `object` declarations. Objects are language-level singletons — annotating them generates invalid code like `single { MyObject() }` calling a non-existent constructor.

❌ **Bad:**
```kotlin
@Single
object MySingleton // Generates invalid: single { MySingleton() }
```

✅ **Good:**
```kotlin
@Single
class MySingleton
```

**Edge Cases:**
- ✅ Detects `@Single`, `@Factory`, `@Scoped`, `@KoinViewModel`, `@KoinWorker`
- ✅ Skips companion objects
- ✅ Allows `@Single` on regular classes

---

### TooManyInjectedParams

**Severity:** Warning
**Active by default:** Yes
**Configurable:** Yes

Detects classes with more than 5 `@InjectedParam` parameters. `ParametersHolder` only supports destructuring up to `component5()`.

❌ **Bad:**
```kotlin
@Single
class MyService(
    @InjectedParam val a: String,
    @InjectedParam val b: Int,
    @InjectedParam val c: Long,
    @InjectedParam val d: Float,
    @InjectedParam val e: Double,
    @InjectedParam val f: Boolean // 6th — too many!
)
```

✅ **Good:**
```kotlin
@Single
class MyService(@InjectedParam val params: MyServiceParams)
```

**Configuration:**
```yaml
TooManyInjectedParams:
  maxInjectedParams: 5
```

**Edge Cases:**
- ✅ Only counts `@InjectedParam` annotated parameters
- ✅ Regular constructor parameters are ignored
- ✅ Default threshold is 5 (configurable)

---

### InvalidNamedQualifierCharacters

**Severity:** Warning
**Active by default:** Yes

Detects `@Named` values containing characters invalid in generated Kotlin identifiers. Hyphens, spaces, and special characters cause KSP code generation failures.

❌ **Bad:**
```kotlin
@Named("ricky-morty")
@Single
class MyService
```

✅ **Good:**
```kotlin
@Named("rickyMorty")
@Single
class MyService
```

**Edge Cases:**
- ✅ Validates against pattern `[a-zA-Z][a-zA-Z0-9_.]*`
- ✅ Allows underscores and dots
- ✅ Detects on class-level and function parameter-level `@Named`

---

### KoinAnnotationOnExtensionFunction

**Severity:** Warning
**Active by default:** Yes

Detects Koin definition annotations on extension functions. KSP code generator ignores the receiver parameter, producing invalid generated code.

❌ **Bad:**
```kotlin
@Module
class MyModule {
    @Single
    fun Scope.provideDatastore(): DataStore = DataStore()
}
```

✅ **Good:**
```kotlin
@Module
class MyModule {
    @Single
    fun provideDatastore(scope: Scope): DataStore = DataStore()
}
```

**Edge Cases:**
- ✅ Detects `@Single`, `@Factory`, `@Scoped`, `@KoinViewModel`, `@KoinWorker`
- ✅ Allows regular (non-extension) functions
- ✅ Allows extension functions without Koin annotations

---

### ViewModelAnnotatedAsSingle

**Severity:** Warning
**Active by default:** Yes

Detects ViewModel classes annotated with `@Single` or `@Factory` instead of `@KoinViewModel`. ViewModels as singletons cause coroutine scope issues: `viewModelScope` is cancelled on navigation but the singleton persists.

❌ **Bad:**
```kotlin
@Single
class MyViewModel : ViewModel() // Coroutine failures after navigation!
```

✅ **Good:**
```kotlin
@KoinViewModel
class MyViewModel : ViewModel()
```

**Edge Cases:**
- ✅ Detects classes extending `ViewModel` and `AndroidViewModel`
- ✅ Detects both `@Single` and `@Factory` as wrong annotations
- ✅ Allows `@KoinViewModel` annotation
- ✅ Only checks direct supertypes (PSI limitation)

---

### AnnotatedClassImplementsNestedInterface

**Severity:** Warning
**Active by default:** Yes

Detects Koin-annotated classes that implement nested/inner interfaces. KSP code generator drops the parent qualifier in `bind()` call, generating `bind(ChildInterface::class)` instead of `bind(Parent.ChildInterface::class)`.

❌ **Bad:**
```kotlin
@Single
class MyImpl : Parent.ChildInterface // KSP generates bind(ChildInterface::class) — wrong!
```

✅ **Good:**
```kotlin
interface ChildInterface // Extract to top-level

@Single
class MyImpl : ChildInterface
```

**Edge Cases:**
- ✅ Detects dot-qualified type references in supertypes
- ✅ Works with sealed interface members
- ✅ Allows top-level interface implementations
- ✅ Only reports when Koin annotations are present

---

### InjectedParamWithNestedGenericType

**Severity:** Warning
**Active by default:** Yes

Detects `@InjectedParam` with nested generic types or star projections. KSP code generator has a known bug where nested type arguments are dropped.

❌ **Bad:**
```kotlin
@Single
class MyService(@InjectedParam val items: List<List<String>>) // KSP bug!
```

✅ **Good:**
```kotlin
typealias StringList = List<String>

@Single
class MyService(@InjectedParam val items: List<StringList>)
```

**Edge Cases:**
- ✅ Detects nested generics: `List<List<String>>`, `Map<String, List<Int>>`
- ✅ Detects star projections: `List<*>`
- ✅ Allows simple generics: `List<String>`
- ✅ Allows non-generic types
- ✅ Only checks `@InjectedParam` annotated parameters

---
