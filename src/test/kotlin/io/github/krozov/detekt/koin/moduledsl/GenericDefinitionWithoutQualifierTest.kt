package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GenericDefinitionWithoutQualifierTest {

    @Test
    fun `reports generic List without qualifier`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single { listOf<String>() }
                single { listOf<Int>() }
            }
        """.trimIndent()

        val findings = GenericDefinitionWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(2)
    }

    @Test
    fun `does not report generic with qualifier`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val m = module {
                single(named("strings")) { listOf<String>() }
            }
        """.trimIndent()

        val findings = GenericDefinitionWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports generic Set without qualifier`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single { setOf<String>() }
            }
        """.trimIndent()

        val findings = GenericDefinitionWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports generic Map without qualifier`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                factory { mapOf<String, Int>() }
            }
        """.trimIndent()

        val findings = GenericDefinitionWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report non-generic types`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single { MyService() }
            }
        """.trimIndent()

        val findings = GenericDefinitionWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports generic Array without qualifier`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single { arrayOf<String>() }
            }
        """.trimIndent()

        val findings = GenericDefinitionWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
