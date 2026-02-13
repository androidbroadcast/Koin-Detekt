package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ScopedWithoutQualifierTest {

    @Test
    fun `reports Scoped without name`() {
        val code = """
            import org.koin.core.annotation.Scoped

            @Scoped
            class MyService
        """.trimIndent()

        val findings = ScopedWithoutQualifier(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `allows Scoped with name`() {
        val code = """
            import org.koin.core.annotation.Scoped

            @Scoped(name = "userScope")
            class MyService
        """.trimIndent()

        val findings = ScopedWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows Single and Factory`() {
        val code = """
            import org.koin.core.annotation.Single
            import org.koin.core.annotation.Factory

            @Single
            class SingleService

            @Factory
            class FactoryService
        """.trimIndent()

        val findings = ScopedWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
