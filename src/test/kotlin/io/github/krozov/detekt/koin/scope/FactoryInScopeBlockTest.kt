package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FactoryInScopeBlockTest {

    @Test
    fun `reports factory inside scope block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                scope<MyActivity> {
                    factory { Presenter(get()) }
                }
            }
        """.trimIndent()

        val findings = FactoryInScopeBlock(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("factory")
    }

    @Test
    fun `reports factoryOf inside scope block`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.factoryOf

            val myModule = module {
                scope<MyActivity> {
                    factoryOf(::MyPresenter)
                }
            }
        """.trimIndent()

        val findings = FactoryInScopeBlock(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report scoped inside scope block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                scope<MyActivity> {
                    scoped { Presenter(get()) }
                }
            }
        """.trimIndent()

        val findings = FactoryInScopeBlock(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report factory outside scope block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                factory { UseCase() }
            }
        """.trimIndent()

        val findings = FactoryInScopeBlock(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports factoryOf inside activityScope`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.factoryOf

            val m = module {
                activityScope {
                    factoryOf(::MyService)
                }
            }
        """.trimIndent()

        val findings = FactoryInScopeBlock(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report factory outside any scope`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                factory { MyService() }
            }
        """.trimIndent()

        val findings = FactoryInScopeBlock(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
