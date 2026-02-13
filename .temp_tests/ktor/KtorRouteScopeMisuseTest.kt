package io.github.krozov.detekt.koin.platform.ktor

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KtorRouteScopeMisuseTest {

    @Test
    fun `reports shared koinScope outside route handler`() {
        val code = """
            fun Application.module() {
                val sharedScope = koinScope()
                routing {
                    get("/api") {
                        val service = sharedScope.get<Service>()
                    }
                }
            }
        """.trimIndent()

        val findings = KtorRouteScopeMisuse(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("koinScope")
        assertThat(findings[0].message).contains("shared")
    }

    @Test
    fun `allows call koinScope in route handler`() {
        val code = """
            fun Application.module() {
                routing {
                    get("/api") {
                        call.koinScope().get<Service>()
                    }
                }
            }
        """.trimIndent()

        val findings = KtorRouteScopeMisuse(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
