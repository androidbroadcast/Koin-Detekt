package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoInjectDelegateTest {

    @Test
    fun `reports inject() delegate usage`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyRepository : KoinComponent {
                val api: ApiService by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("inject()")
    }

    @Test
    fun `does not report constructor injection`() {
        val code = """
            class MyRepository(
                private val api: ApiService
            )
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report regular by lazy`() {
        val code = """
            class MyRepository {
                val api: ApiService by lazy { ApiServiceImpl() }
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports injectOrNull() delegate usage`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.injectOrNull

            class MyRepository : KoinComponent {
                val api: ApiService? by injectOrNull()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("injectOrNull()")
    }

    @Test
    fun `reports inject in companion object`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyClass : KoinComponent {
                companion object {
                    val repo: Repository by inject()
                }
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports multiple inject delegates`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyClass : KoinComponent {
                val repo: Repository by inject()
                val api: ApiService by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).hasSize(2)
    }

    @Test
    fun `reports lazy inject delegate`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyClass : KoinComponent {
                val repo by inject<Repository>()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
