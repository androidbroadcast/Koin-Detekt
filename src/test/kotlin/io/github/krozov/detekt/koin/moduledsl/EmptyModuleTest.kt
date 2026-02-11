package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmptyModuleTest {

    @Test
    fun `reports empty module`() {
        val code = """
            import org.koin.dsl.module

            val emptyModule = module { }
        """.trimIndent()

        val findings = EmptyModule(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("empty")
    }

    @Test
    fun `does not report module with includes`() {
        val code = """
            import org.koin.dsl.module

            val featureModule = module {
                includes(networkModule)
            }
        """.trimIndent()

        val findings = EmptyModule(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report module with definitions`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { MyService() }
            }
        """.trimIndent()

        val findings = EmptyModule(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
}
