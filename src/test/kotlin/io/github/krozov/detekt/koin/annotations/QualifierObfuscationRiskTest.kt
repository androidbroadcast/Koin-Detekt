package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class QualifierObfuscationRiskTest {

    @Test
    fun `reports Qualifier with class reference`() {
        val code = """
            import org.koin.core.annotation.Qualifier

            class SomeClass

            @Qualifier(SomeClass::class)
            annotation class MyQualifier
        """.trimIndent()

        val findings = QualifierObfuscationRisk(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("FQN")
    }

    @Test
    fun `does not report Named with string value`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Named("my-service")
            @Single
            class MyService
        """.trimIndent()

        val findings = QualifierObfuscationRisk(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report Qualifier without arguments`() {
        val code = """
            import org.koin.core.annotation.Qualifier

            @Qualifier
            annotation class MyQualifier
        """.trimIndent()

        val findings = QualifierObfuscationRisk(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report Qualifier with string argument`() {
        val code = """
            import org.koin.core.annotation.Qualifier

            @Qualifier("explicit-name")
            annotation class MyQualifier
        """.trimIndent()

        val findings = QualifierObfuscationRisk(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }
}
