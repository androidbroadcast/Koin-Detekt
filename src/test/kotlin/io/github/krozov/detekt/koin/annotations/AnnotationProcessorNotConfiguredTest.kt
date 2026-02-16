package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AnnotationProcessorNotConfiguredTest {

    @Test
    fun `reports @Single without generated code check (info level)`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService
        """.trimIndent()

        val findings = AnnotationProcessorNotConfigured(Config.empty).lint(code)

        // This rule provides info-level message since we can't reliably detect
        // if annotation processor is configured in Detekt context
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Koin annotations used")
        assertThat(findings[0].message).contains("io.insert-koin.compiler.plugin")
        assertThat(findings[0].message).contains("koin-ksp-compiler")
    }

    @Test
    fun `allows regular classes`() {
        val code = """
            class MyService
        """.trimIndent()

        val findings = AnnotationProcessorNotConfigured(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `can be configured to skip check`() {
        val config = TestConfig(
            "skipCheck" to true
        )

        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService
        """.trimIndent()

        val findings = AnnotationProcessorNotConfigured(config).lint(code)
        assertThat(findings).isEmpty()
    }
}
