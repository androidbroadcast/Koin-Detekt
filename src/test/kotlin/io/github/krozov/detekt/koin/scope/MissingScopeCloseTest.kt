package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MissingScopeCloseTest {

    @Test
    fun `reports class with createScope but no close`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class SessionManager : KoinComponent {
                val scope = getKoin().createScope("session")
                fun getService() = scope.get<SessionService>()
            }
        """.trimIndent()

        val findings = MissingScopeClose(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("close()")
    }

    @Test
    fun `does not report class with createScope and close`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class SessionManager : KoinComponent {
                val scope = getKoin().createScope("session")
                fun getService() = scope.get<SessionService>()
                fun destroy() { scope.close() }
            }
        """.trimIndent()

        val findings = MissingScopeClose(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports getOrCreateScope without close`() {
        val code = """
            class Manager {
                val scope = koin.getOrCreateScope("id")
            }
        """.trimIndent()

        val findings = MissingScopeClose(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }
}
