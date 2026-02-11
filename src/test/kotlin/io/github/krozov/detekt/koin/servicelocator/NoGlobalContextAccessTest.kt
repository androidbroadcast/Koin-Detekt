package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoGlobalContextAccessTest {

    @Test
    fun `reports GlobalContext get() access`() {
        val code = """
            import org.koin.core.context.GlobalContext

            fun getService() {
                val koin = GlobalContext.get()
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("GlobalContext")
    }

    @Test
    fun `reports GlobalContext getKoinApplicationOrNull`() {
        val code = """
            import org.koin.core.context.GlobalContext

            val app = GlobalContext.getKoinApplicationOrNull()
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report startKoin usage`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    modules(appModule)
                }
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
}
