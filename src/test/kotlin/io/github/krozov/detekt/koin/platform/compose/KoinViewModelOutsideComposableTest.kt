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
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
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

    // Edge Case: Nested composables
    @Test
    fun `allows koinViewModel in nested Composable function`() {
        val code = """
            @Composable
            fun ParentScreen() {
                @Composable
                fun NestedScreen() {
                    val viewModel = koinViewModel<MyViewModel>()
                }
                NestedScreen()
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows koinViewModel in Composable lambda parameter`() {
        val code = """
            @Composable
            fun ScreenWithLambda(content: @Composable () -> Unit) {
                content()
            }

            @Composable
            fun MyScreen() {
                ScreenWithLambda {
                    val viewModel = koinViewModel<MyViewModel>()
                }
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows koinViewModel with multiple type parameters`() {
        val code = """
            @Composable
            fun MyScreen() {
                val viewModel = koinViewModel<MyViewModel<String, Int>>()
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports koinViewModel in companion object`() {
        val code = """
            class MyClass {
                companion object {
                    val viewModel = koinViewModel<MyViewModel>()
                }
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows koinViewModel in Composable extension function`() {
        val code = """
            @Composable
            fun MyScreen.Content() {
                val viewModel = koinViewModel<MyViewModel>()
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports koinViewModel in regular extension function`() {
        val code = """
            fun MyScreen.content() {
                val viewModel = koinViewModel<MyViewModel>()
            }
        """.trimIndent()

        val findings = KoinViewModelOutsideComposable(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
