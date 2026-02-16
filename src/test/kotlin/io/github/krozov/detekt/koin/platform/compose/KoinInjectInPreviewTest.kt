package io.github.krozov.detekt.koin.platform.compose

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoinInjectInPreviewTest {

    @Test
    fun `reports koinInject in Preview function`() {
        val code = """
            @Preview
            @Composable
            fun MyScreenPreview() {
                val repository = koinInject<Repository>()
                MyScreen(repository)
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `allows koinInject in non-Preview Composable`() {
        val code = """
            @Composable
            fun MyScreen() {
                val repository = koinInject<Repository>()
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows regular parameters in Preview`() {
        val code = """
            @Preview
            @Composable
            fun MyScreenPreview() {
                MyScreen(FakeRepository())
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    // Edge Case: Multiple Preview annotations
    @Test
    fun `reports koinInject with multiple Preview annotations`() {
        val code = """
            @Preview(name = "Light")
            @Preview(name = "Dark")
            @Composable
            fun MyScreenPreview() {
                val repository = koinInject<Repository>()
                MyScreen(repository)
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports koinInject in Preview with parameters`() {
        val code = """
            @Preview(showBackground = true, backgroundColor = 0xFFFFFF)
            @Composable
            fun MyScreenPreview() {
                val repository = koinInject<Repository>()
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports koinInject with qualified injection in Preview`() {
        val code = """
            @Preview
            @Composable
            fun MyScreenPreview() {
                val repository = koinInject<Repository>(qualifier = named("fake"))
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows remember in Preview`() {
        val code = """
            @Preview
            @Composable
            fun MyScreenPreview() {
                val state = remember { mutableStateOf(false) }
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports koinInject with type parameters in Preview`() {
        val code = """
            @Preview
            @Composable
            fun MyScreenPreview() {
                val service = koinInject<Service<String, Int>>()
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports multiple koinInject calls in Preview`() {
        val code = """
            @Preview
            @Composable
            fun MyScreenPreview() {
                val repository = koinInject<Repository>()
                val service = koinInject<Service>()
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).hasSize(2)
    }

    @Test
    fun `reports koinInject in nested lambda within Preview`() {
        val code = """
            @Preview
            @Composable
            fun MyScreenPreview() {
                Column {
                    val repository = koinInject<Repository>()
                }
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports koinViewModel in Preview function`() {
        val code = """
            @Preview
            @Composable
            fun MyScreenPreview() {
                val vm = koinViewModel<MyViewModel>()
                MyScreen(vm)
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("koinViewModel()")
    }

    @Test
    fun `reports koinInject in PreviewLightDark function`() {
        val code = """
            @PreviewLightDark
            @Composable
            fun MyScreenPreview() {
                val repo = koinInject<Repository>()
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports koinInject in PreviewFontScale function`() {
        val code = """
            @PreviewFontScale
            @Composable
            fun MyScreenPreview() {
                val repo = koinInject<Repository>()
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports koinInject in PreviewScreenSizes function`() {
        val code = """
            @PreviewScreenSizes
            @Composable
            fun MyScreenPreview() {
                val repo = koinInject<Repository>()
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports koinViewModel in PreviewLightDark function`() {
        val code = """
            @PreviewLightDark
            @Composable
            fun MyScreenPreview() {
                val vm = koinViewModel<MyViewModel>()
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows koinViewModel in non-Preview Composable`() {
        val code = """
            @Composable
            fun MyScreen() {
                val vm = koinViewModel<MyViewModel>()
            }
        """.trimIndent()

        val findings = KoinInjectInPreview(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
