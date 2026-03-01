package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoinAnnotationOnClassWithNestedDeclarationsTest {

    @Test
    fun `reports Single on class with nested class`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class OuterService {
                class NestedHelper
            }
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("nested class")
    }

    @Test
    fun `reports Factory on class with nested class`() {
        val code = """
            import org.koin.core.annotation.Factory

            @Factory
            class OuterFactory {
                class NestedModel
            }
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report Single on class without nested classes`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report unannotated class with nested classes`() {
        val code = """
            class OuterClass {
                class NestedClass
            }
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `should not report when annotation is from non-Koin package`() {
        val code = """
            import com.other.Single

            @Single
            class OuterService {
                class NestedHelper
            }
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }
}
