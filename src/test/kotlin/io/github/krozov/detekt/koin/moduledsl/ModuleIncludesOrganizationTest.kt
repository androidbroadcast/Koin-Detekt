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
}
