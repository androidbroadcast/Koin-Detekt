package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OverrideInIncludedModuleTest {

    @Test
    fun `reports bind attempt in module with includes`() {
        val code = """
            import org.koin.dsl.module

            val baseModule = module {
                single { ServiceA() } bind Service::class
            }

            val overrideModule = module {
                includes(baseModule)
                single { ServiceB() } bind Service::class
            }
        """.trimIndent()

        val findings = OverrideInIncludedModule(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("includes()")
        assertThat(findings[0].message).contains("override")
    }

    @Test
    fun `reports duplicate type in module with includes`() {
        val code = """
            import org.koin.dsl.module

            val module1 = module {
                single<String> { "original" }
            }

            val module2 = module {
                includes(module1)
                single<String> { "override" }
            }
        """.trimIndent()

        val findings = OverrideInIncludedModule(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report when no includes`() {
        val code = """
            import org.koin.dsl.module

            val module1 = module {
                single<String> { "value1" }
            }

            val module2 = module {
                single<String> { "value2" }
            }
        """.trimIndent()

        val findings = OverrideInIncludedModule(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report different types in module with includes`() {
        val code = """
            import org.koin.dsl.module

            val baseModule = module {
                single<String> { "text" }
            }

            val extendedModule = module {
                includes(baseModule)
                single<Int> { 42 }
            }
        """.trimIndent()

        val findings = OverrideInIncludedModule(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports multiple potential overrides`() {
        val code = """
            import org.koin.dsl.module

            val base = module {
                single<String> { "str" }
                single<Int> { 1 }
            }

            val override = module {
                includes(base)
                single<String> { "new" }
                single<Int> { 2 }
            }
        """.trimIndent()

        val findings = OverrideInIncludedModule(Config.empty).lint(code)
        assertThat(findings).hasSizeGreaterThanOrEqualTo(1)
    }

    @Test
    fun `does not report when override flag is used`() {
        val code = """
            import org.koin.dsl.module

            val base = module {
                single<String> { "original" }
            }

            val override = module {
                includes(base)
                single<String>(override = true) { "new" }
            }
        """.trimIndent()

        val findings = OverrideInIncludedModule(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports bind syntax with includes`() {
        val code = """
            import org.koin.dsl.module

            interface Api
            class ApiV1 : Api
            class ApiV2 : Api

            val v1Module = module {
                single { ApiV1() } bind Api::class
            }

            val v2Module = module {
                includes(v1Module)
                single { ApiV2() } bind Api::class
            }
        """.trimIndent()

        val findings = OverrideInIncludedModule(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
