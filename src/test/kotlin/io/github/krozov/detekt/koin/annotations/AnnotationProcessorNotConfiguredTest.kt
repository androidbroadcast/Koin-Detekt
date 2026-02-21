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

        // This rule is informational only — it cannot verify build configuration.
        // Message should mention both KSP and Koin Compiler Plugin setup methods.
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("annotation processor configuration cannot be verified")
        assertThat(findings[0].message).contains("com.google.devtools.ksp")
        assertThat(findings[0].message).contains("io.insert-koin.compiler.plugin")
        assertThat(findings[0].message).contains("skipCheck=true")
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

    @Test
    fun `reports KoinViewModel without processor`() {
        val code = """
            import org.koin.android.annotation.KoinViewModel

            @KoinViewModel
            class MyViewModel
        """.trimIndent()

        val findings = AnnotationProcessorNotConfigured(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports ComponentScan without processor`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.ComponentScan

            @Module
            @ComponentScan
            class MyModule
        """.trimIndent()

        val findings = AnnotationProcessorNotConfigured(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
