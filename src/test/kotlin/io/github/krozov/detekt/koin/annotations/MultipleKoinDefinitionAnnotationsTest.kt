package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MultipleKoinDefinitionAnnotationsTest {

    @Test
    fun `reports class with Single and Factory`() {
        val code = """
            import org.koin.core.annotation.Single
            import org.koin.core.annotation.Factory

            @Single
            @Factory
            class MyService
        """.trimIndent()

        val findings = MultipleKoinDefinitionAnnotations(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("@Single")
        assertThat(findings[0].message).contains("@Factory")
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports three definition annotations on same class`() {
        val code = """
            import org.koin.core.annotation.Single
            import org.koin.core.annotation.Factory
            import org.koin.core.annotation.Scoped

            @Single
            @Factory
            @Scoped
            class MyService
        """.trimIndent()

        val findings = MultipleKoinDefinitionAnnotations(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("@Single")
        assertThat(findings[0].message).contains("@Factory")
        assertThat(findings[0].message).contains("@Scoped")
    }

    @Test
    fun `allows class with only Single annotation`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService
        """.trimIndent()

        val findings = MultipleKoinDefinitionAnnotations(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows class with only Factory annotation`() {
        val code = """
            import org.koin.core.annotation.Factory

            @Factory
            class MyService
        """.trimIndent()

        val findings = MultipleKoinDefinitionAnnotations(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report class with Single and a non-Koin annotation`() {
        val code = """
            import org.koin.core.annotation.Single
            import com.example.SomeOtherAnnotation

            @Single
            @SomeOtherAnnotation
            class MyService
        """.trimIndent()

        val findings = MultipleKoinDefinitionAnnotations(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when annotation is from non-Koin package`() {
        val code = """
            import com.other.Single
            import com.other.Factory

            @Single
            @Factory
            class MyService
        """.trimIndent()

        val findings = MultipleKoinDefinitionAnnotations(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report KoinViewModel alone`() {
        val code = """
            import org.koin.core.annotation.KoinViewModel

            @KoinViewModel
            class MyViewModel
        """.trimIndent()

        val findings = MultipleKoinDefinitionAnnotations(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
