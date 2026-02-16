package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DuplicateBindingWithoutQualifierTest {

    @Test
    fun `reports duplicate bindings to same type without qualifiers`() {
        val code = """
            import org.koin.dsl.module

            interface Foo

            val m = module {
                single { ServiceA() } bind Foo::class
                single { ServiceB() } bind Foo::class
            }
        """.trimIndent()

        val findings = DuplicateBindingWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report bindings with different qualifiers`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            interface Foo

            val m = module {
                single { ServiceA() } bind Foo::class named("a")
                single { ServiceB() } bind Foo::class named("b")
            }
        """.trimIndent()

        val findings = DuplicateBindingWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report single binding without qualifier`() {
        val code = """
            import org.koin.dsl.module

            interface Foo

            val m = module {
                single { ServiceA() } bind Foo::class
            }
        """.trimIndent()

        val findings = DuplicateBindingWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report bindings to different types`() {
        val code = """
            import org.koin.dsl.module

            interface Foo
            interface Bar

            val m = module {
                single { ServiceA() } bind Foo::class
                single { ServiceB() } bind Bar::class
            }
        """.trimIndent()

        val findings = DuplicateBindingWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports duplicate bindings across different definition types`() {
        val code = """
            import org.koin.dsl.module

            interface Foo

            val m = module {
                single { ServiceA() } bind Foo::class
                factory { ServiceB() } bind Foo::class
            }
        """.trimIndent()

        val findings = DuplicateBindingWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
