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

    @Test
    fun `allows koinViewModel in inline Composable`() {
        val code = """
            @Composable
            inline fun MyScreen() {
                val viewModel = koinViewModel<MyViewModel>()
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports koinViewModel in init block`() {
        val code = """
            class MyClass {
                init {
                    val viewModel = koinViewModel<MyViewModel>()
                }
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `ignores other viewModel calls`() {
        val code = """
            fun MyScreen() {
                val viewModel = viewModel<MyViewModel>()
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles qualified koinViewModel call`() {
        val code = """
            @Composable
            fun MyScreen() {
                val viewModel = koinViewModel<MyViewModel>(qualifier = named("test"))
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
