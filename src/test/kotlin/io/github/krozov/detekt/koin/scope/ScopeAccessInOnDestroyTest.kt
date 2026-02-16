package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ScopeAccessInOnDestroyTest {

    @Test
    fun `reports get() call in onDestroy`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyFragment : KoinComponent {
                override fun onDestroy() {
                    val service = get<MyService>()
                    service.cleanup()
                }
            }
        """.trimIndent()

        val findings = ScopeAccessInOnDestroy(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("onDestroy")
    }

    @Test
    fun `reports inject() call in onDestroyView`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyFragment : KoinComponent {
                override fun onDestroyView() {
                    val service by inject<MyService>()
                }
            }
        """.trimIndent()

        val findings = ScopeAccessInOnDestroy(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report get() in onCreate`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyFragment : KoinComponent {
                override fun onCreate() {
                    val service = get<MyService>()
                }
            }
        """.trimIndent()

        val findings = ScopeAccessInOnDestroy(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report property access in onDestroy`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class MyFragment : KoinComponent {
                lateinit var service: MyService

                override fun onDestroy() {
                    service.cleanup()
                }
            }
        """.trimIndent()

        val findings = ScopeAccessInOnDestroy(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
