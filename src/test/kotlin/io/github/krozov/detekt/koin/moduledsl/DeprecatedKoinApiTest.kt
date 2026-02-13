package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DeprecatedKoinApiTest {

    @Test
    fun `reports checkModules usage`() {
        val code = """
            import org.koin.test.check.checkModules

            fun test() {
                checkModules { }
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("verify()")
    }

    @Test
    fun `reports koinNavViewModel usage`() {
        val code = """
            val vm = koinNavViewModel<MyViewModel>()
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("koinViewModel()")
    }

    @Test
    fun `does not report current API`() {
        val code = """
            import org.koin.test.verify.verify

            fun test() {
                verify { }
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports stateViewModel usage`() {
        val code = """
            val vm = stateViewModel<MyViewModel>()
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("viewModel()")
    }

    @Test
    fun `reports viewModel usage`() {
        val code = """
            import org.koin.androidx.viewmodel.dsl.viewModel

            val m = module {
                viewModel { MyViewModel() }
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports getViewModel usage`() {
        val code = """
            class MyActivity {
                val vm = getViewModel<MyViewModel>()
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
