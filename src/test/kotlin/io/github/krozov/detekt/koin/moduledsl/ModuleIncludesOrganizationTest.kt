package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ModuleIncludesOrganizationTest {

    @Test
    fun `reports module with too many includes and definitions`() {
        val code = """
            import org.koin.dsl.module

            val appModule = module {
                includes(networkModule, dbModule, featureAModule, featureBModule)
                single { AppConfig() }
                factory { Logger() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("God Module")
    }

    @Test
    fun `does not report module with few includes and definitions`() {
        val code = """
            import org.koin.dsl.module

            val appModule = module {
                includes(networkModule, dbModule)
                single { AppConfig() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report module with only includes`() {
        val code = """
            import org.koin.dsl.module

            val appModule = module {
                includes(networkModule, dbModule, featureAModule, featureBModule, featureCModule)
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `respects custom threshold`() {
        val config = TestConfig("maxIncludesWithDefinitions" to 5)

        val code = """
            import org.koin.dsl.module

            val appModule = module {
                includes(a, b, c, d, e, f)
                single { Config() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(config)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report exactly at threshold`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                includes(a, b, c)  // Exactly 3
                single { Service() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports one over threshold`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                includes(a, b, c, d)  // 4 includes
                single { Service() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `handles module with value argument syntax`() {
        val code = """
            import org.koin.dsl.module

            val m = module({
                includes(a, b, c, d)
                single { Service() }
            })
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `counts multiple definition types`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                includes(a, b, c, d)
                factory { FactoryService() }
                scoped { ScopedService() }
                viewModel { MyViewModel() }
                worker { MyWorker() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `ignores non-call statements`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                includes(a, b, c, d)
                val x = 5
                single { Service() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `ignores non-module call expressions`() {
        val code = """
            fun myFunction() {
                includes(a, b, c)
                single { Service() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports with enhanced message format`() {
        val code = """
            import org.koin.dsl.module

            val appModule = module {
                includes(a, b, c, d)
                single { AppConfig() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    // Edge case: Multiple includes() calls in same module
    @Test
    fun `reports module with multiple includes calls and definitions`() {
        val code = """
            import org.koin.dsl.module

            val appModule = module {
                includes(moduleA, moduleB)
                includes(moduleC, moduleD)
                single { Service1() }
                factory { Service2() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Nested module() call with includes
    @Test
    fun `reports nested module with too many includes and definitions`() {
        val code = """
            import org.koin.dsl.module

            val outerModule = module {
                includes(
                    module {
                        includes(a, b, c, d)
                        single { InnerService() }
                    }
                )
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Large module with all definition types
    @Test
    fun `reports module with many includes and diverse definitions`() {
        val code = """
            import org.koin.dsl.module

            val megaModule = module {
                includes(a, b, c, d, e)
                single { ServiceA() }
                factory { ServiceB() }
                scoped { ServiceC() }
                viewModel { ViewModelD() }
                worker { WorkerE() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Just at threshold with multiple definition types
    @Test
    fun `does not report when exactly at threshold with mixed definitions`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                includes(a, b, c)
                factory { FactoryService() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    // Edge case: Custom high threshold
    @Test
    fun `respects custom high threshold`() {
        val config = TestConfig("maxIncludesWithDefinitions" to 10)

        val code = """
            import org.koin.dsl.module

            val appModule = module {
                includes(a, b, c, d, e, f)
                single { Config() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(config).lint(code)
        assertThat(findings).isEmpty()
    }

    // Edge case: Module with includes and non-definition statements
    @Test
    fun `counts only definition statements not variable declarations`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                includes(a, b, c, d)
                val logger = Logger()
                val config = Config()
                single { Service() }
            }
        """.trimIndent()

        val findings = ModuleIncludesOrganization(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
