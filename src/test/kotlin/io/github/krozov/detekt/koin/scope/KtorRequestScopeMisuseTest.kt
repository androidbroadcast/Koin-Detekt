package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KtorRequestScopeMisuseTest {

    @Test
    fun `reports single inside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    single { RequestLogger() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("singleton")
    }

    @Test
    fun `reports singleOf inside requestScope`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf

            val myModule = module {
                requestScope {
                    singleOf(::RequestHandler)
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report scoped inside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    scoped { RequestLogger() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report single outside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { Logger() }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
}
