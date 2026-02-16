package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AnnotatedClassImplementsNestedInterfaceTest {

    @Test
    fun `reports Single class implementing nested interface`() {
        val code = """
            import org.koin.core.annotation.Single

            class Parent {
                interface ChildInterface
            }

            @Single
            class MyImpl : Parent.ChildInterface
        """.trimIndent()

        val findings = AnnotatedClassImplementsNestedInterface(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("nested")
    }

    @Test
    fun `reports Factory class implementing sealed interface member`() {
        val code = """
            import org.koin.core.annotation.Factory

            sealed interface Transformer {
                interface TextTransformer
            }

            @Factory
            class MyTransformer : Transformer.TextTransformer
        """.trimIndent()

        val findings = AnnotatedClassImplementsNestedInterface(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows class implementing top-level interface`() {
        val code = """
            import org.koin.core.annotation.Single

            interface MyInterface

            @Single
            class MyImpl : MyInterface
        """.trimIndent()

        val findings = AnnotatedClassImplementsNestedInterface(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows class without Koin annotations implementing nested interface`() {
        val code = """
            class Parent {
                interface ChildInterface
            }

            class MyImpl : Parent.ChildInterface
        """.trimIndent()

        val findings = AnnotatedClassImplementsNestedInterface(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
