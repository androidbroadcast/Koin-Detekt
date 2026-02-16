package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EnumQualifierCollisionTest {

    @Test
    fun `reports enum qualifiers with same value name from different enums`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            enum class Type1 { VALUE }
            enum class Type2 { VALUE }

            val m = module {
                single(named(Type1.VALUE)) { ServiceA() }
                single(named(Type2.VALUE)) { ServiceB() }
            }
        """.trimIndent()

        val findings = EnumQualifierCollision(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report enum qualifiers with different value names`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            enum class Type { VALUE_A, VALUE_B }

            val m = module {
                single(named(Type.VALUE_A)) { ServiceA() }
                single(named(Type.VALUE_B)) { ServiceB() }
            }
        """.trimIndent()

        val findings = EnumQualifierCollision(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report same enum value used multiple times`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            enum class Type { VALUE }

            val m = module {
                single(named(Type.VALUE)) { ServiceA() }
                factory(named(Type.VALUE)) { ServiceB() }
            }
        """.trimIndent()

        val findings = EnumQualifierCollision(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports collision with multiple enum types`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            enum class Status { ACTIVE }
            enum class Mode { ACTIVE }
            enum class State { ACTIVE }

            val m = module {
                single(named(Status.ACTIVE)) { ServiceA() }
                single(named(Mode.ACTIVE)) { ServiceB() }
                single(named(State.ACTIVE)) { ServiceC() }
            }
        """.trimIndent()

        val findings = EnumQualifierCollision(Config.empty).lint(code)
        assertThat(findings.size).isGreaterThanOrEqualTo(1)
    }

    @Test
    fun `does not report string qualifiers`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val m = module {
                single(named("VALUE")) { ServiceA() }
                single(named("VALUE")) { ServiceB() }
            }
        """.trimIndent()

        val findings = EnumQualifierCollision(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
