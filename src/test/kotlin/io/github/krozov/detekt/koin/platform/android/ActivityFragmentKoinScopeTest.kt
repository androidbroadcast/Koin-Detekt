package io.github.krozov.detekt.koin.platform.android

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ActivityFragmentKoinScopeTest {

    @Test
    fun `reports activityScope in Fragment`() {
        val code = """
            class MyFragment : Fragment() {
                val viewModel by activityScope().inject<MyViewModel>()
            }
        """.trimIndent()

        val findings = ActivityFragmentKoinScope(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `allows fragmentScope in Fragment`() {
        val code = """
            class MyFragment : Fragment() {
                val viewModel by fragmentScope().inject<MyViewModel>()
            }
        """.trimIndent()

        val findings = ActivityFragmentKoinScope(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows activityScope in Activity`() {
        val code = """
            class MyActivity : AppCompatActivity() {
                val viewModel by activityScope().inject<MyViewModel>()
            }
        """.trimIndent()

        val findings = ActivityFragmentKoinScope(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
