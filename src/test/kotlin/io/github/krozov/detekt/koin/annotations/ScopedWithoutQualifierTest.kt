package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ScopedWithoutQualifierTest {

    @Test
    fun `reports Scoped without Scope annotation`() {
        val code = """
            import org.koin.core.annotation.Scoped

            @Scoped
            class MyService
        """.trimIndent()

        val findings = ScopedWithoutQualifier(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("@Scope")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports Scoped without Scope annotation on class`() {
        val code = """
            import org.koin.core.annotation.Scoped

            @Scoped(name = "userScope")
            class MyService // Has qualifier but no @Scope!
        """.trimIndent()

        val findings = ScopedWithoutQualifier(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("@Scope")
    }

    @Test
    fun `allows Scoped with Scope annotation`() {
        val code = """
            import org.koin.core.annotation.Scope
            import org.koin.core.annotation.Scoped

            @Scope(name = "userScope")
            @Scoped
            class MyService
        """.trimIndent()

        val findings = ScopedWithoutQualifier(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows Scoped with ActivityScope archetype`() {
        val code = """
            import org.koin.core.annotation.Scoped
            import org.koin.android.annotation.ActivityScope

            @ActivityScope
            @Scoped
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
