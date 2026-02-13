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

    @Test
    fun `reports application dot koin in Ktor`() {
        val code = """
            import io.ktor.server.application.*

            fun Application.module() {
                val koinInstance = application.koin
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("application.koinModules()")
    }

    @Test
    fun `does not report non-application dot koin`() {
        val code = """
            class MyClass {
                val koinInstance = someOther.koin
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles call expression without name`() {
        val code = """
            val result = unknownFunction()
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports with enhanced message format for checkModules`() {
        val code = """
            import org.koin.test.check.checkModules

            fun test() {
                checkModules { }
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports with enhanced message format for application dot koin`() {
        val code = """
            import io.ktor.server.application.*

            fun Application.module() {
                val koinInstance = application.koin
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    // Edge case: Multiple deprecated APIs in one file
    @Test
    fun `reports multiple deprecated API usages in single file`() {
        val code = """
            import org.koin.test.check.checkModules

            class MyTest {
                fun test1() {
                    checkModules { }
                    val vm1 = koinNavViewModel<ViewModel1>()
                    val vm2 = stateViewModel<ViewModel2>()
                    val vm3 = getViewModel<ViewModel3>()
                }
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).hasSize(4)
    }

    // Edge case: Deprecated API in companion object
    @Test
    fun `reports deprecated API in companion object`() {
        val code = """
            import org.koin.test.check.checkModules

            class AppTest {
                companion object {
                    fun setup() {
                        checkModules { }
                    }
                }
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: application.koin in nested function
    @Test
    fun `reports application dot koin in nested function`() {
        val code = """
            import io.ktor.server.application.*

            fun Application.configureKoin() {
                fun getKoin() = application.koin
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Deprecated viewModel in multiple modules
    @Test
    fun `reports viewModel usage in multiple modules`() {
        val code = """
            import org.koin.androidx.viewmodel.dsl.viewModel
            import org.koin.dsl.module

            val moduleA = module {
                viewModel { ViewModelA() }
            }

            val moduleB = module {
                viewModel { ViewModelB() }
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).hasSize(2)
    }

    // Edge case: Mix of deprecated and current APIs
    @Test
    fun `reports only deprecated APIs when mixed with current ones`() {
        val code = """
            import org.koin.test.verify.verify
            import org.koin.test.check.checkModules

            fun test() {
                verify { }  // Current API
                checkModules { }  // Deprecated
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("checkModules")
    }

    // Edge case: Deprecated API with qualified imports
    @Test
    fun `reports deprecated API with fully qualified call`() {
        val code = """
            fun test() {
                org.koin.test.check.checkModules { }
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: stateViewModel in extension function
    @Test
    fun `reports stateViewModel in extension function`() {
        val code = """
            fun ComponentActivity.setupViewModel() {
                val vm = stateViewModel<MainViewModel>()
            }
        """.trimIndent()

        val findings = DeprecatedKoinApi(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
