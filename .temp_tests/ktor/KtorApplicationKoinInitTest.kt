package io.github.krozov.detekt.koin.platform.ktor

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KtorApplicationKoinInitTest {

    @Test
    fun `reports install Koin in routing block`() {
        val code = """
            fun Application.module() {
                routing {
                    install(Koin) { }
                    get("/api") { }
                }
            }
        """.trimIndent()

        val findings = KtorApplicationKoinInit(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("install(Koin)")
        assertThat(findings[0].message).contains("routing")
    }

    @Test
    fun `allows install Koin in Application module`() {
        val code = """
            fun Application.module() {
                install(Koin) { }
                routing {
                    get("/api") { }
                }
            }
        """.trimIndent()

        val findings = KtorApplicationKoinInit(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports install Koin in route handler`() {
        val code = """
            fun Application.module() {
                routing {
                    get("/api") {
                        install(Koin) { }
                    }
                }
            }
        """.trimIndent()

        val findings = KtorApplicationKoinInit(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
