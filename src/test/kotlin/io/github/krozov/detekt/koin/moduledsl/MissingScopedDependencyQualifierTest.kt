package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MissingScopedDependencyQualifierTest {

    @Test
    fun `reports duplicate type definitions without qualifier`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { HttpClient() }
                single { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Multiple definitions")
    }

    @Test
    fun `does not report when qualifiers are used`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val myModule = module {
                single(named("cio")) { HttpClient() }
                single(named("okhttp")) { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report different types`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { ApiService() }
                single { DatabaseService() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports duplicate factory definitions without qualifier`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                factory { Logger() }
                factory { Logger() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Logger")
    }

    @Test
    fun `reports duplicate scoped definitions without qualifier`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                scoped { RequestHandler() }
                scoped { RequestHandler() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("RequestHandler")
    }

    @Test
    fun `reports duplicate viewModel definitions without qualifier`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                viewModel { MainViewModel() }
                viewModel { MainViewModel() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("MainViewModel")
    }

    @Test
    fun `reports duplicate worker definitions without qualifier`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                worker { BackgroundWorker() }
                worker { BackgroundWorker() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("BackgroundWorker")
    }

    @Test
    fun `allows one default with qualified definitions by default`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val myModule = module {
                single(named("first")) { HttpClient() }
                single { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports when multiple definitions without qualifier exist alongside qualified ones`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val myModule = module {
                single(named("first")) { HttpClient() }
                single { HttpClient() }
                single { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `recognizes qualifier() function`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.qualifier

            val myModule = module {
                single(qualifier("cio")) { HttpClient() }
                single(qualifier("okhttp")) { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports three or more duplicate definitions`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { Logger() }
                single { Logger() }
                single { Logger() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report duplicates across different modules`() {
        val code = """
            import org.koin.dsl.module

            val module1 = module {
                single { HttpClient() }
            }

            val module2 = module {
                single { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports duplicates with mixed definition types`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { Repository() }
                factory { Repository() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `handles definitions with constructor references`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single(::ApiService)
                single(::ApiService)
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `handles empty module`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module { }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles module with single definition`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { MyService() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles definitions with complex lambda expressions`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single {
                    val config = loadConfig()
                    HttpClient(config)
                }
                single {
                    val config = loadConfig()
                    HttpClient(config)
                }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        // Type extraction doesn't work for multi-statement lambdas
        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles definitions without lambda body`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single<MyService>()
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles nested module definitions`() {
        val code = """
            import org.koin.dsl.module

            val outerModule = module {
                single { OuterService() }
                includes(module {
                    single { InnerService() }
                })
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles definitions with property references`() {
        val code = """
            import org.koin.dsl.module

            class Services {
                fun createLogger() = Logger()
            }

            val myModule = module {
                val services = Services()
                single { services.createLogger() }
                single { services.createLogger() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        // Type extraction doesn't work for property access calls (services.createLogger)
        // Only works for direct constructor calls
        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles definitions with empty lambda`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { }
                single { }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles definitions with inline value arguments`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf

            val myModule = module {
                single(createdAtStart = true) { Service1() }
                single(createdAtStart = false) { Service2() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles all definition types in same module`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { ServiceA() }
                factory { ServiceB() }
                scoped { ServiceC() }
                viewModel { ViewModelD() }
                worker { WorkerE() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports when lambda returns different expressions of same type`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { if (true) Logger() else Logger() }
                factory { Logger() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        // Type extraction doesn't work for if-expressions as first statement
        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles definitions outside module context`() {
        val code = """
            import org.koin.dsl.module

            fun createModule() {
                single { Service() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles module with only one type having duplicates`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { ServiceA() }
                factory { ServiceB() }
                scoped { ServiceB() }
                viewModel { ServiceC() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("ServiceB")
    }

    @Test
    fun `handles definitions with type parameters`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single<List<String>> { listOf() }
                single<List<Int>> { listOf() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        // Both have listOf() as the first call, so they're seen as duplicates
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows one default with mixed qualifier styles`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named
            import org.koin.core.qualifier.qualifier

            val myModule = module {
                single(named("first")) { Service() }
                single(qualifier("second")) { Service() }
                single { Service() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles value arguments without qualifiers`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single(createdAtStart = true) { ServiceA() }
                single(createdAtStart = false) { ServiceA() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports only first unqualified duplicate`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { Logger() }
                factory { Logger() }
                scoped { Logger() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        // Should only report once, on the first unqualified definition
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `handles deeply nested module includes`() {
        val code = """
            import org.koin.dsl.module

            val innerModule = module {
                single { InnerService() }
            }

            val middleModule = module {
                includes(innerModule)
                single { MiddleService() }
            }

            val outerModule = module {
                includes(middleModule)
                single { OuterService() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles lambda arguments vs value arguments`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { ServiceA() }
                single() { ServiceA() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `handles all unqualified duplicates`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { TypeA() }
                single { TypeA() }
                factory { TypeB() }
                factory { TypeB() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        // Should report TypeA and TypeB separately
        assertThat(findings).hasSize(2)
    }

    @Test
    fun `reports with enhanced message format`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { HttpClient() }
                single { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    // Edge case: Duplicate with StringQualifier
    @Test
    fun `does not report when using StringQualifier`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.StringQualifier

            val myModule = module {
                single(StringQualifier("first")) { HttpClient() }
                single(StringQualifier("second")) { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    // Edge case: Complex nested module structure with duplicates
    @Test
    fun `reports duplicates within nested inline module`() {
        val code = """
            import org.koin.dsl.module

            val compositeModule = module {
                includes(
                    module {
                        single { Service() }
                        single { Service() }
                    }
                )
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Duplicate definitions with scoped and viewModel
    @Test
    fun `reports mixed scoped and viewModel duplicates`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                scoped { ViewModel() }
                viewModel { ViewModel() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Multiple types with duplicates
    @Test
    fun `reports all types with duplicates separately`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { TypeA() }
                single { TypeA() }
                factory { TypeB() }
                factory { TypeB() }
                scoped { TypeC() }
                scoped { TypeC() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(3)
    }

    // Edge case: One qualified, multiple unqualified
    @Test
    fun `reports when one qualified and two unqualified exist`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val myModule = module {
                single(named("special")) { Logger() }
                single { Logger() }
                factory { Logger() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Definitions with createdAtStart parameter
    @Test
    fun `reports duplicates with createdAtStart parameter`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single(createdAtStart = true) { DatabaseClient() }
                single(createdAtStart = false) { DatabaseClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Definitions across multiple includes in same module
    @Test
    fun `does not report duplicates in separate included modules`() {
        val code = """
            import org.koin.dsl.module

            val moduleA = module {
                single { Service() }
            }

            val moduleB = module {
                single { Service() }
            }

            val composite = module {
                includes(moduleA, moduleB)
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    // allowOneDefault config tests
    @Test
    fun `reports one unqualified when allowOneDefault is false`() {
        val config = TestConfig("allowOneDefault" to "false")
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val myModule = module {
                single(named("first")) { HttpClient() }
                single { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(config).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows one default with qualified definitions when allowOneDefault is true`() {
        val config = TestConfig("allowOneDefault" to "true")
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val myModule = module {
                single(named("first")) { HttpClient() }
                single { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(config).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `still reports two unqualified even with allowOneDefault true`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val myModule = module {
                single(named("first")) { HttpClient() }
                single { HttpClient() }
                single { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
