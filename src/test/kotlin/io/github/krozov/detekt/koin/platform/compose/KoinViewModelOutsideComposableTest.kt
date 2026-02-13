package io.github.krozov.detekt.koin.platform.compose

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoinViewModelOutsideComposableTest {

    @Test
    fun `reports koinViewModel outside Composable function`() {
        val code = """
            fun MyScreen() {
                val viewModel = koinViewModel<MyViewModel>()
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("koinViewModel")
        assertThat(findings[0].message).contains("@Composable")
    }

    @Test
    fun `allows koinViewModel in Composable function`() {
        val code = """
            @Composable
            fun MyScreen() {
                val viewModel = koinViewModel<MyViewModel>()
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }
}
