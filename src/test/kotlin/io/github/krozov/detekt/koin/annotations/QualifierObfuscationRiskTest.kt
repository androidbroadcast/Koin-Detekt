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

    @Test
    fun `known limitation - reports non-Koin Qualifier with class reference`() {
        // Rule uses short-name matching: any annotation named "Qualifier" with a class literal
        // argument is flagged, including non-Koin @Qualifier annotations from javax.inject,
        // jakarta.inject, Dagger, etc. This is a known false-positive risk.
        val code = """
            annotation class Qualifier(val value: kotlin.reflect.KClass<*>)
            class SomeClass
            @Qualifier(SomeClass::class)
            annotation class MyQualifier
        """.trimIndent()

        val findings = QualifierObfuscationRisk(Config.empty).lint(code)

        // Documents the known limitation: non-Koin @Qualifier with class ref is also flagged.
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `should not report when annotation is from non-Koin package`() {
        val findings = QualifierObfuscationRisk(Config.empty).lint("""
            import com.other.Qualifier
            import com.example.SomeClass
            @Qualifier(SomeClass::class)
            annotation class MyQualifier
        """.trimIndent())
        assertThat(findings).isEmpty()
    }
}
