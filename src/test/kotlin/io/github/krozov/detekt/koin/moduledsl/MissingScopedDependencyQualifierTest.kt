package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MissingScopedDependencyQualifierTest {

    @Test
    fun `reports duplicate type definitions without qualifier`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { HttpClient() }
                single { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Multiple definitions")
    }

    @Test
    fun `does not report when qualifiers are used`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val myModule = module {
                single(named("cio")) { HttpClient() }
                single(named("okhttp")) { HttpClient() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report different types`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { ApiService() }
                single { DatabaseService() }
            }
        """.trimIndent()

        val findings = MissingScopedDependencyQualifier(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
}
