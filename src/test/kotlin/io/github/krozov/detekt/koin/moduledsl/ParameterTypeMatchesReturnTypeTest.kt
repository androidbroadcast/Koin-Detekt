package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParameterTypeMatchesReturnTypeTest {

    @Test
    fun `reports factory with return type matching parameter type`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                factory<Int> { limit: Int ->
                    kotlin.random.Random.nextInt(limit)
                }
            }
        """.trimIndent()

        val findings = ParameterTypeMatchesReturnType(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("parametersOf")
    }

    @Test
    fun `does not report when return type differs from parameter`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                factory<String> { count: Int ->
                    "Result: ${'$'}count"
                }
            }
        """.trimIndent()

        val findings = ParameterTypeMatchesReturnType(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report factory without type parameter`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                factory { Service() }
            }
        """.trimIndent()

        val findings = ParameterTypeMatchesReturnType(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
