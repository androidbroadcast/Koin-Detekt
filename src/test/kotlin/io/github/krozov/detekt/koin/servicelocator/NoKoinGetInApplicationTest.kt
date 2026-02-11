package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoKoinGetInApplicationTest {

    @Test
    fun `reports get() inside startKoin block`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    val service = get<MyService>()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("startKoin")
    }

    @Test
    fun `does not report modules() inside startKoin`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    modules(appModule)
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
}
