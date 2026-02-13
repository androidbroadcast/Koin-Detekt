package io.github.krozov.detekt.koin

import io.github.krozov.detekt.koin.servicelocator.NoKoinComponentInterface
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuppressionTest {

    @Test
    fun `Suppress on class suppresses rule`() {
        val code = """
            import org.koin.core.component.KoinComponent

            @Suppress("NoKoinComponentInterface")
            class MyClass : KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `Suppress on file suppresses all violations`() {
        val code = """
            @file:Suppress("NoKoinComponentInterface")

            import org.koin.core.component.KoinComponent

            class MyClass : KoinComponent
            class Another : KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `Without Suppress violations are reported`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class MyClass : KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
