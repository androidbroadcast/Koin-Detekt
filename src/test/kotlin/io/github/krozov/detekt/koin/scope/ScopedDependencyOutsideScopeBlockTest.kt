package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ScopedDependencyOutsideScopeBlockTest {

    @Test
    fun `reports scoped outside scope block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                scoped { UserSession() }
            }
        """.trimIndent()

        val findings = ScopedDependencyOutsideScopeBlock(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `does not report scoped inside scope block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                scope<MainActivity> {
                    scoped { UserSession() }
                }
            }
        """.trimIndent()

        val findings = ScopedDependencyOutsideScopeBlock(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report scoped inside activityScope`() {
        val code = """
            import org.koin.androidx.scope.activityScope

            val myModule = module {
                activityScope {
                    scoped { Presenter() }
                }
            }
        """.trimIndent()

        val findings = ScopedDependencyOutsideScopeBlock(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report scoped inside fragmentScope`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                fragmentScope {
                    scoped { MyFragment() }
                }
            }
        """.trimIndent()

        val findings = ScopedDependencyOutsideScopeBlock(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports scoped at module level`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                scoped { MyService() }
            }
        """.trimIndent()

        val findings = ScopedDependencyOutsideScopeBlock(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
