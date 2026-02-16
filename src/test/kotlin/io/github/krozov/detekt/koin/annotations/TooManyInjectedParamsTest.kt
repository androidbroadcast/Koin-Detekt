package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TooManyInjectedParamsTest {

    @Test
    fun `reports class with 6 InjectedParam parameters`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(
                @InjectedParam val a: String,
                @InjectedParam val b: Int,
                @InjectedParam val c: Long,
                @InjectedParam val d: Float,
                @InjectedParam val e: Double,
                @InjectedParam val f: Boolean
            )
        """.trimIndent()

        val findings = TooManyInjectedParams(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("6")
        assertThat(findings[0].message).contains("5")
    }

    @Test
    fun `allows class with 5 InjectedParam parameters`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(
                @InjectedParam val a: String,
                @InjectedParam val b: Int,
                @InjectedParam val c: Long,
                @InjectedParam val d: Float,
                @InjectedParam val e: Double
            )
        """.trimIndent()

        val findings = TooManyInjectedParams(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows mix of InjectedParam and regular params`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(
                @InjectedParam val a: String,
                @InjectedParam val b: Int,
                val regularParam: Long,
                val anotherRegular: Float
            )
        """.trimIndent()

        val findings = TooManyInjectedParams(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows class without InjectedParam`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService(val a: String, val b: Int)
        """.trimIndent()

        val findings = TooManyInjectedParams(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
