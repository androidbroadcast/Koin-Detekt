package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InjectedParamAnnotationOrderTest {

    @Test
    fun `reports InjectedParam not as first annotation`() {
        val code = """
            import org.koin.core.annotation.Single
            import org.koin.core.annotation.InjectedParam

            @Single
            class MyService(
                @Suppress("Unused") @InjectedParam val param: String,
                val dep: Dependency
            )
        """.trimIndent()

        val findings = InjectedParamAnnotationOrder(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("InjectedParam")
        assertThat(findings[0].message).contains("first")
    }

    @Test
    fun `does not report InjectedParam as first annotation`() {
        val code = """
            import org.koin.core.annotation.Single
            import org.koin.core.annotation.InjectedParam

            @Single
            class MyService(
                @InjectedParam @Suppress("Unused") val param: String,
                val dep: Dependency
            )
        """.trimIndent()

        val findings = InjectedParamAnnotationOrder(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report InjectedParam as only annotation`() {
        val code = """
            import org.koin.core.annotation.Single
            import org.koin.core.annotation.InjectedParam

            @Single
            class MyService(
                @InjectedParam val param: String
            )
        """.trimIndent()

        val findings = InjectedParamAnnotationOrder(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report parameters without InjectedParam`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService(@Suppress("Unused") val param: String)
        """.trimIndent()

        val findings = InjectedParamAnnotationOrder(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `should not report when class annotation is from non-Koin package`() {
        val code = """
            import com.other.Single
            import org.koin.core.annotation.InjectedParam

            @Single
            class MyService(
                @Suppress("Unused") @InjectedParam val param: String
            )
        """.trimIndent()

        val findings = InjectedParamAnnotationOrder(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `should not report when InjectedParam is from non-Koin library`() {
        val code = """
            import org.koin.core.annotation.Single
            import com.other.library.InjectedParam

            @Single
            class MyService(
                @Suppress("Unused") @InjectedParam val param: String
            )
        """.trimIndent()

        val findings = InjectedParamAnnotationOrder(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
