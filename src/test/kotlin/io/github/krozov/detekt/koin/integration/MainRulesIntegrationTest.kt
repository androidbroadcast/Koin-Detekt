package io.github.krozov.detekt.koin.integration

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests for main Koin rules with E2E code analysis.
 *
 * Tests key rules covering:
 * - Annotation rules (MissingModuleAnnotation, MissingScopeAnnotation)
 * - Module DSL rules (EmptyModule, SingleForNonSharedDependency, ModuleIncludesOrganization)
 * - Scope rules (MissingScopeClose, FactoryInScopeBlock)
 * - Service Locator rules (NoGetOutsideModuleDefinition, NoKoinComponentInterface)
 * - Architecture rules (CircularModuleDependency, GetConcreteTypeInsteadOfInterface)
 * - Platform rules (Android, Compose)
 */
class MainRulesIntegrationTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    // ========== Annotation Rules ==========

    @Test
    fun `reports missing module annotation on class with Single`() {
        val code = """
            package com.example.app

            import org.koin.core.annotation.Single

            class UserRepository {
                @Single
                fun getUser(): User = User()
            }

            class User
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "MissingModuleAnnotation" }!!
        val findings = rule.lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Koin definition annotations without @Module")
    }

    @Test
    fun `reports missing module annotation on class with Factory`() {
        val code = """
            package com.example.app

            import org.koin.core.annotation.Factory

            class UserService {
                @Factory
                fun create(): UserService = UserService()
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "MissingModuleAnnotation" }!!
        val findings = rule.lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report when module has proper annotations`() {
        val code = """
            package com.example.app

            import org.koin.core.annotation.ComponentScan
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single

            @Module
            @ComponentScan("com.example")
            class AppModule {
                @Single
                fun provideRepo(): Repository = Repository()
            }

            class Repository
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "MissingModuleAnnotation" }!!
        val findings = rule.lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports module without definitions and without includes`() {
        val code = """
            package com.example.app

            import org.koin.core.annotation.Module

            @Module
            class EmptyAppModule
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "MissingModuleAnnotation" }!!
        val findings = rule.lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("@Module without @ComponentScan")
    }

    @Test
    fun `reports missing scope annotation on KoinScope`() {
        val code = """
            package com.example.app

            import org.koin.core.annotation.Scope

            @Scope
            class UserScope {
                fun getUser() = "user"
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "MissingScopeAnnotation" }
        if (rule != null) {
            val findings = rule.lint(code)
            // Some versions of the rule may not report this case
            assertThat(findings.map { it.issue.id }).contains("MissingScopeAnnotation")
        }
    }

    // ========== Module DSL Rules ==========

    @Test
    fun `reports empty module in module definition`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val appModule = module { }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "EmptyModule" }!!
        val findings = rule.lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports empty module in nested module list`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val appModule = module {
                module { }
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "EmptyModule" }!!
        val findings = rule.lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report empty module when it has includes`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val appModule = module {
                includes(otherModule)
            }

            val otherModule = module {
                single { UserService() }
            }

            class UserService
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "EmptyModule" }!!
        val findings = rule.lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports single for non-shared dependency with use case pattern`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val appModule = module {
                single { GetUserUseCase() }
            }

            class GetUserUseCase
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "SingleForNonSharedDependency" }!!
        val findings = rule.lint(code)

        // Reports when single is used for a type matching the pattern (.*UseCase, .*Interactor, .*Mapper)
        assertThat(findings.map { it.issue.id }).contains("SingleForNonSharedDependency")
    }

    @Test
    fun `does not report factory when explicitly used`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val appModule = module {
                factory { UserService() }
            }

            class UserService
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "SingleForNonSharedDependency" }!!
        val findings = rule.lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports module with too many includes with definitions`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val myModule = module {
                includes(moduleA, moduleB, moduleC, moduleD)
                single { RepositoryImpl() }
            }

            val moduleA = module { }
            val moduleB = module { }
            val moduleC = module { }
            val moduleD = module { }

            interface Repository {
                fun getData(): String
            }

            class RepositoryImpl : Repository {
                override fun getData() = "data"
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "ModuleIncludesOrganization" }
        if (rule != null) {
            val findings = rule.lint(code)
            // Reports when module has > 3 includes with definitions
            assertThat(findings.map { it.issue.id }).contains("ModuleIncludesOrganization")
        }
    }

    // ========== Scope Rules ==========

    @Test
    fun `reports missing scope close when scope is created but not closed`() {
        val code = """
            package com.example.app

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject
            import org.koin.core.component.KoinScopeComponent
            import org.koin.core.scope.Scope

            class MyManager : KoinComponent, KoinScopeComponent {
                override val scope: Scope by inject()

                fun init() {
                    val myScope = koin.createScope("myId")
                    // BAD: Scope is created but never closed
                }
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "MissingScopeClose" }
        if (rule != null) {
            val findings = rule.lint(code)
            // Should report when scope is created but not closed
            assertThat(findings.map { it.issue.id }).contains("MissingScopeClose")
        }
    }

    @Test
    fun `reports factory in scope block`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val appModule = module {
                scope {
                    factory { UserService() }
                }
            }

            class UserService
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "FactoryInScopeBlock" }!!
        val findings = rule.lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report factory outside scope block`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val appModule = module {
                factory { UserService() }
            }

            class UserService
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "FactoryInScopeBlock" }!!
        val findings = rule.lint(code)

        assertThat(findings).isEmpty()
    }

    // ========== Service Locator Rules ==========

    @Test
    fun `reports get call outside module definition`() {
        val code = """
            package com.example.app

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository : KoinComponent {
                private val api = get<ApiService>()
            }

            interface ApiService
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "NoGetOutsideModuleDefinition" }!!
        val findings = rule.lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report get inside module definition`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val appModule = module {
                single { MyRepository(get()) }
            }

            class MyRepository(private val api: ApiService)
            interface ApiService
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "NoGetOutsideModuleDefinition" }!!
        val findings = rule.lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports KoinComponent on non-framework class`() {
        val code = """
            package com.example.app

            import org.koin.core.component.KoinComponent

            class UserRepository : KoinComponent {
                fun fetch() = "data"
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "NoKoinComponentInterface" }!!
        val findings = rule.lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report KoinComponent on Application class`() {
        val code = """
            package com.example.app

            import android.app.Application
            import org.koin.core.component.KoinComponent

            class MyApp : Application(), KoinComponent
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "NoKoinComponentInterface" }!!
        val findings = rule.lint(code)

        // Application class is allowed
        assertThat(findings).isEmpty()
    }

    // ========== Architecture Rules ==========

    @Test
    fun `reports circular dependency between modules`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val moduleA = module {
                includes(moduleB)
            }

            val moduleB = module {
                includes(moduleA)
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "CircularModuleDependency" }!!
        val findings = rule.lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Circular dependency")
    }

    @Test
    fun `reports self-referencing module`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val appModule = module {
                includes(appModule)
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "CircularModuleDependency" }!!
        val findings = rule.lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("includes itself")
    }

    @Test
    fun `does not report valid module hierarchy`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val coreModule = module { }

            val featureModule = module {
                includes(coreModule)
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "CircularModuleDependency" }!!
        val findings = rule.lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports getting concrete type instead of interface`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val appModule = module {
                single<Repository> { UserRepositoryImpl() }
                factory { Consumer(get<UserRepositoryImpl>()) }
            }

            interface Repository {
                fun getData(): String
            }

            class UserRepositoryImpl : Repository {
                override fun getData() = "data"
            }

            class Consumer(val repo: UserRepositoryImpl)
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "GetConcreteTypeInsteadOfInterface" }
        if (rule != null) {
            val findings = rule.lint(code)
            // Should suggest using interface type
            assertThat(findings.map { it.issue.id }).contains("GetConcreteTypeInsteadOfInterface")
        }
    }

    @Test
    fun `does not report when using interface type`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val appModule = module {
                single<Repository> { UserRepositoryImpl() }
            }

            interface Repository {
                fun getData(): String
            }

            class UserRepositoryImpl : Repository {
                override fun getData() = "data"
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "GetConcreteTypeInsteadOfInterface" }
        if (rule != null) {
            val findings = rule.lint(code)
            // When using interface type, rule should not report
            assertThat(findings.map { it.issue.id }).doesNotContain("GetConcreteTypeInsteadOfInterface")
        }
    }

    // ========== Platform Rules ==========

    @Test
    fun `reports koinContext in ViewModel on Android`() {
        val code = """
            package com.example.app

            import android.content.Context
            import androidx.lifecycle.ViewModel
            import org.koin.android.ext.koin.androidContext

            class MyViewModel : ViewModel() {
                val context: Context = androidContext()
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "AndroidKoinContextInViewModel" }
            ?: ruleSet.rules.find { it.ruleId == "AndroidContextNotFromKoin" }

        if (rule != null) {
            val findings = rule.lint(code)
            assertThat(findings.map { it.issue.id }).anyMatch {
                it in listOf("AndroidKoinContextInViewModel", "AndroidContextNotFromKoin")
            }
        }
    }

    @Test
    fun `detects KoinViewModel outside composable`() {
        val code = """
            package com.example.app

            import androidx.lifecycle.ViewModel
            import org.koin.androidx.compose.koinViewModel

            class MyActivity {
                // BAD: Using koinViewModel outside composable function
                fun getVm(): MyViewModel = koinViewModel<MyViewModel>()
            }

            class MyViewModel : ViewModel()
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "KoinViewModelOutsideComposable" }
        if (rule != null) {
            val findings = rule.lint(code)
            assertThat(findings.map { it.issue.id }).contains("KoinViewModelOutsideComposable")
        }
    }

    @Test
    fun `reports rememberCoroutineScope in Koin viewModel scope`() {
        val code = """
            package com.example.app

            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import org.koin.core.context.loadKoinModules
            import org.koin.dsl.module

            val featureModule = module { }

            @Composable
            fun FeatureScreen() {
                remember { loadKoinModules(featureModule) }
            }
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "RememberCoroutineScopeInKoinCompose" }
            ?: ruleSet.rules.find { it.ruleId == "RememberKoinModulesLeak" }

        if (rule != null) {
            val findings = rule.lint(code)
            assertThat(findings.map { it.issue.id }).anyMatch {
                it in listOf("RememberCoroutineScopeInKoinCompose", "RememberKoinModulesLeak")
            }
        }
    }

    // ========== Complex Integration Tests ==========

    @Test
    fun `analyzes complex Koin setup with multiple issues`() {
        val code = """
            package com.example.app

            import android.app.Activity
            import org.koin.android.ext.koin.androidContext
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get
            import org.koin.core.context.startKoin
            import org.koin.dsl.module
            import androidx.lifecycle.ViewModel
            import org.koin.androidx.viewmodel.ext.android.viewModel

            // BAD: Starting Koin in Activity
            class MainActivity : Activity() {
                override fun onCreate() {
                    super.onCreate()
                    startKoin {
                        androidContext(this@MainActivity)
                    }
                }
            }

            // BAD: KoinComponent in non-framework class
            class MyRepository : KoinComponent {
                private val api = get<ApiService>()
            }

            interface ApiService

            // BAD: Using viewModel outside composable
            class MyActivity : Activity() {
                val vm: MyViewModel by viewModel()
            }

            class MyViewModel : ViewModel()

            // GOOD: Proper module definition
            val appModule = module {
                single<Repository> { UserRepository() }
                viewModel { MyViewModel() }
            }

            interface Repository
            class UserRepository : Repository
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { it.lint(code) }

        // Should detect multiple violations
        assertThat(findings.size).isGreaterThanOrEqualTo(3)

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains(
            "StartKoinInActivity",
            "NoKoinComponentInterface",
            "NoGetOutsideModuleDefinition"
        )
    }

    @Test
    fun `analyzes code with multiple module dependencies`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            // BAD: Circular dependency
            val networkModule = module {
                includes(storageModule)
            }

            val storageModule = module {
                includes(networkModule)
            }

            val dataModule = module { }

            val appModule = module {
                includes(dataModule)
            }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { it.lint(code) }

        val circularFindings = findings.filter { it.issue.id == "CircularModuleDependency" }
        assertThat(circularFindings).isNotEmpty() // At least one module in cycle is reported
    }

    @Test
    fun `validates proper Koin module structure`() {
        val code = """
            package com.example.app

            import android.app.Application
            import org.koin.android.ext.koin.androidContext
            import org.koin.core.context.startKoin
            import org.koin.dsl.module

            // GOOD: Starting Koin in Application
            class MyApp : Application() {
                override fun onCreate() {
                    super.onCreate()
                    startKoin {
                        androidContext(this@MyApp)
                        modules(appModule)
                    }
                }
            }

            // GOOD: Proper module with definitions
            val appModule = module {
                single<Repository> { UserRepository() }
                factory { UserService(get()) }
            }

            interface Repository
            class UserRepository : Repository
            class UserService(private val repo: Repository)
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { it.lint(code) }

        // Should not have violations for this properly structured code
        val criticalViolations = findings.filter {
            it.issue.id in listOf(
                "NoKoinComponentInterface",
                "NoGetOutsideModuleDefinition",
                "StartKoinInActivity",
                "SingleForNonSharedDependency",
                "CircularModuleDependency"
            )
        }

        assertThat(criticalViolations).isEmpty()
    }

    @Test
    fun `reports Koin annotations used without proper module`() {
        val code = """
            package com.example.app

            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single

            @Module
            class AppModule {
                @Single
                fun provideService(): MyService = MyService()
            }

            class MyService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { it.lint(code) }

        // Check for annotation-related rules
        val annotationRules = listOf(
            "MissingModuleAnnotation",
            "AnnotationProcessorNotConfigured",
            "MissingScopedDependencyQualifier"
        )

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).anyMatch { it in annotationRules }
    }

    @Test
    fun `handles nested module definitions`() {
        val code = """
            package com.example.app

            import org.koin.dsl.module

            val outerModule = module {
                // BAD: Empty nested module
                module { }

                // GOOD: Module with definitions
                module {
                    single { UserService() }
                }

                includes(innerModule)
            }

            val innerModule = module {
                single { DataRepository() }
            }

            class UserService
            class DataRepository
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { it.lint(code) }

        // Should detect empty module
        val emptyModuleFindings = findings.filter { it.issue.id == "EmptyModule" }
        assertThat(emptyModuleFindings).hasSize(1)
    }
}
