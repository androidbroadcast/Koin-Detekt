package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ViewModelAsSingletonTest {

    @Test
    fun `reports ViewModel defined with single`() {
        val code = """
            import org.koin.dsl.module
            import androidx.lifecycle.ViewModel

            class MyViewModel : ViewModel()

            val appModule = module {
                single { MyViewModel() }
            }
        """.trimIndent()

        val findings = ViewModelAsSingleton(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("ViewModel")
        assertThat(findings[0].message).contains("viewModel")
    }
}
