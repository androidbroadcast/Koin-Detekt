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
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
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

    @Test
    fun `reports koinScope without receiver`() {
        val code = """
            fun Application.module() {
                val scope = koinScope()
            }
        """.trimIndent()

        val findings = KtorRouteScopeMisuse(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("state leaks")
    }

    @Test
    fun `allows call koinScope stored in property`() {
        val code = """
            fun Application.module() {
                routing {
                    get("/api") {
                        val requestScope = call.koinScope()
                        val service = requestScope.get<Service>()
                    }
                }
            }
        """.trimIndent()

        val findings = KtorRouteScopeMisuse(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `ignores other scope calls`() {
        val code = """
            fun Application.module() {
                val scope = myScope()
            }
        """.trimIndent()

        val findings = KtorRouteScopeMisuse(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports koinScope in class property`() {
        val code = """
            class MyService {
                val scope = koinScope()
            }
        """.trimIndent()

        val findings = KtorRouteScopeMisuse(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
