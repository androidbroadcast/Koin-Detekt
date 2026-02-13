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
}
