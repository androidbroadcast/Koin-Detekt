package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConstructorDslAmbiguousParametersTest {

    @Test
    fun `reports factoryOf with duplicate parameter types`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.factoryOf

            class MyService(val a: Int, val b: Int)

            val m = module {
                factoryOf(::MyService)
            }
        """.trimIndent()

        val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("ambiguous")
    }

    @Test
    fun `reports viewModelOf with Int and nullable Int parameters`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.androidx.viewmodel.dsl.viewModelOf
            import androidx.lifecycle.ViewModel

            class MyViewModel(val required: Int, val optional: Int?) : ViewModel()

            val m = module {
                viewModelOf(::MyViewModel)
            }
        """.trimIndent()

        val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report factoryOf with different parameter types`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.factoryOf

            class MyService(val name: String, val count: Int)

            val m = module {
                factoryOf(::MyService)
            }
        """.trimIndent()

        val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report lambda-based factory`() {
        val code = """
            import org.koin.dsl.module

            class MyService(val a: Int, val b: Int)

            val m = module {
                factory { MyService(get(), get()) }
            }
        """.trimIndent()

        val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports singleOf with String String parameters`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf

            class Config(val host: String, val port: String)

            val m = module {
                singleOf(::Config)
            }
        """.trimIndent()

        val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report single parameter constructor`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.factoryOf

            class MyService(val name: String)

            val m = module {
                factoryOf(::MyService)
            }
        """.trimIndent()

        val findings = ConstructorDslAmbiguousParameters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
