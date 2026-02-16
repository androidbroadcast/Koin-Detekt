package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnassignedQualifierInWithOptionsTest {

    @Test
    fun `reports named() call without assignment in withOptions`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val m = module {
                factory { Service() } withOptions {
                    named("myService")
                }
            }
        """.trimIndent()

        val findings = UnassignedQualifierInWithOptions(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("qualifier")
    }

    @Test
    fun `does not report when qualifier is assigned`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val m = module {
                factory { Service() } withOptions {
                    qualifier = named("myService")
                }
            }
        """.trimIndent()

        val findings = UnassignedQualifierInWithOptions(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports qualifier() call without assignment in withOptions`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.qualifier

            val m = module {
                single { Service() } withOptions {
                    qualifier("myService")
                }
            }
        """.trimIndent()

        val findings = UnassignedQualifierInWithOptions(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report withOptions with other properties`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                factory { Service() } withOptions {
                    createdAtStart = true
                }
            }
        """.trimIndent()

        val findings = UnassignedQualifierInWithOptions(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
