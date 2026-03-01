package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ScopedBindsHasNoEffectTest {

    @Test
    fun `reports Scoped with binds parameter`() {
        val code = """
            import org.koin.core.annotation.Scoped

            interface MyInterface

            @Scoped(binds = [MyInterface::class])
            class MyService : MyInterface
        """.trimIndent()

        val findings = ScopedBindsHasNoEffect(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("binds")
    }

    @Test
    fun `does not report Scoped without binds parameter`() {
        val code = """
            import org.koin.core.annotation.Scoped

            @Scoped
            class MyService
        """.trimIndent()

        val findings = ScopedBindsHasNoEffect(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report Single with binds parameter`() {
        val code = """
            import org.koin.core.annotation.Single

            interface MyInterface

            @Single(binds = [MyInterface::class])
            class MyService : MyInterface
        """.trimIndent()

        val findings = ScopedBindsHasNoEffect(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report Factory with binds parameter`() {
        val code = """
            import org.koin.core.annotation.Factory

            interface MyInterface

            @Factory(binds = [MyInterface::class])
            class MyService : MyInterface
        """.trimIndent()

        val findings = ScopedBindsHasNoEffect(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports Scoped with binds containing multiple classes`() {
        val code = """
            import org.koin.core.annotation.Scoped

            interface InterfaceA
            interface InterfaceB

            @Scoped(binds = [InterfaceA::class, InterfaceB::class])
            class MyService : InterfaceA, InterfaceB
        """.trimIndent()

        val findings = ScopedBindsHasNoEffect(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
    }
}
