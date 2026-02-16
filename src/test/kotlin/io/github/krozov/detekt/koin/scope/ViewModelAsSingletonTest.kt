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

    @Test
    fun `reports ViewModel defined with singleOf`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf
            import androidx.lifecycle.ViewModel

            class MyViewModel : ViewModel()

            val appModule = module {
                singleOf(::MyViewModel)
            }
        """.trimIndent()

        val findings = ViewModelAsSingleton(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report ViewModel defined with viewModel`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.androidx.viewmodel.dsl.viewModel
            import androidx.lifecycle.ViewModel

            class MyViewModel : ViewModel()

            val appModule = module {
                viewModel { MyViewModel() }
            }
        """.trimIndent()

        val findings = ViewModelAsSingleton(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report ViewModel defined with viewModelOf`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.androidx.viewmodel.dsl.viewModelOf
            import androidx.lifecycle.ViewModel

            class MyViewModel : ViewModel()

            val appModule = module {
                viewModelOf(::MyViewModel)
            }
        """.trimIndent()

        val findings = ViewModelAsSingleton(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report regular class defined with single`() {
        val code = """
            import org.koin.dsl.module

            class MyRepository

            val appModule = module {
                single { MyRepository() }
            }
        """.trimIndent()

        val findings = ViewModelAsSingleton(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }
}
