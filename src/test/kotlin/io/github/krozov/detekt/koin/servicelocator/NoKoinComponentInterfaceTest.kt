package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoKoinComponentInterfaceTest {

    @Test
    fun `reports KoinComponent in regular class`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class UserRepository : KoinComponent {
                fun getData() = get<ApiService>()
            }
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("KoinComponent")
    }

    @Test
    fun `reports KoinScopeComponent in regular class`() {
        val code = """
            import org.koin.core.component.KoinScopeComponent

            class MyService : KoinScopeComponent {
                override val scope = getKoin().createScope()
            }
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report when extending allowed super type`() {
        val config = io.gitlab.arturbosch.detekt.test.TestConfig(
            "allowedSuperTypes" to listOf("Application", "Activity")
        )

        val code = """
            import org.koin.core.component.KoinComponent

            class MainActivity : Activity(), KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(config)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report class without KoinComponent`() {
        val code = """
            class MyService(private val repo: Repository)
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports edge case - NonActivity should not match Activity`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class NonActivity : KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("NonActivity")
        assertThat(findings[0].message).contains("not a framework entry point")
    }
}
